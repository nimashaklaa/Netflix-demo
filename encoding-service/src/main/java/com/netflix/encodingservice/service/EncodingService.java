package com.netflix.encodingservice.service;

import com.netflix.encodingservice.event.VideoUploadedEvent;
import com.netflix.encodingservice.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {
    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${encoding.base-path}")
    private String basePath;

    private static final String VIDEO_ENCODED_TOPIC = "video-encoded";

    //Video qualities to encode
    //format: resolution, bitrate, framerate
    private static final List<int[]> VIDEO_QUALITIES = Arrays.asList(
            new int[] {1920,5000, 1080},
            new int[] {1280,2800, 720},
            new int[] {854,1200, 480},
            new int[] {640,700, 360},
            new int[] {426,500, 240}

    );

    /**
     * Main encoding pipeline
     * 1. Download video from S3
     * 2. Encode video to different qualities
     * 3. Generate HLS playlist
     * 4. Create a master playlist
     5. Upload encoded video to S3
     6. Publish VideoEncodedEvent to Kafka
     */
    public void encodeVideo(VideoUploadedEvent event) {
        log.info("Encoding video: {}", event.getMovieId());

        //Create a unique key for the encoded video
        String jobPath = basePath + "/" + event.getMovieId();
        try{
            // Create temp directory
            Files.createDirectories(Paths.get(jobPath));
            Files.createDirectories(Paths.get(jobPath + "/encoded"));

            // Step 1: Download raw video from S3
            String localVideoPath = jobPath + "/raw_video.mp4";
            downloadFromS3(event.getVideo(), localVideoPath);
            log.info("Downloaded raw video from S3: {}", localVideoPath);

            // Step 2 and 3: Encode video to different qualities and generate HLS playlists
            for (int[] qualities : VIDEO_QUALITIES) {
                int width = qualities[0];
                int bitrate = qualities[1];
                int height = qualities[2];

                String qualityDir = jobPath + "/encoded" + height + "p";
                Files.createDirectories(Paths.get(qualityDir));

                encodeToHLS(localVideoPath, qualityDir, width, height, bitrate);
                log.info("Encoded video quality dir: {}", qualityDir);
            }
            // step 4: Generate master playlist
            String masterPlaylistPath = jobPath + "/encoded/master.m3u8";
            generateMasterPlaylist(masterPlaylistPath);
            log.info("Generated master playlist: {}", masterPlaylistPath);

            //Step 5: upload all resources files to S3
            String encodedPrefix = "encoded/" + event.getMovieId() +"/";
            uploadEncodedFilesToS3(jobPath+"/encoded", encodedPrefix);
            log.info("Uploaded encoded files to S3");

            //Step 6: Publish VideoEncodedEvent
            String masterPlaylistKey = encodedPrefix + "master.m3u8";
            String hlsUrl = "https://" + bucketName + ".s3.amazonaws.com/" + masterPlaylistKey;
            VideoEncodedEvent videoEncodedEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    hlsUrl,
                    masterPlaylistKey,
                    true,
                    null
            );
            kafkaTemplate.send(VIDEO_ENCODED_TOPIC,event.getMovieId(), videoEncodedEvent);
            log.info("Published VideoEncodedEvent for movie: {}", event.getMovieId());
        }
        catch (Exception e) {
            log.error("Error occurred while encoding video: {}", event.getMovieId(), e);

            //publish failure event
            VideoEncodedEvent failureEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    null,
                    null,
                    false,
                    e.getMessage()
            );
            kafkaTemplate.send(VIDEO_ENCODED_TOPIC,event.getMovieId(), failureEvent);
        }finally {
            cleanupTempFiles(jobPath);
        }


    }

    private void cleanupTempFiles(String jobPath) {
        try {
            Path dirPath = Paths.get(jobPath);
            if (Files.exists(dirPath)) {
                try (var paths = Files.walk(dirPath)) {
                    paths.sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    log.error("Error deleting temp file: {}", path, e);
                                }
                            });
                }
                log.info("Cleaned up temp files");
            }
        } catch (IOException e) {
            log.error("Error occurred while cleaning up temp files", e);
        }
    }

    /**
     * Download file from s3 to local path
     */
    private void downloadFromS3(String s3Key, String localPath) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        s3Client.getObject(getObjectRequest, Paths.get(localPath));
    }

    private void encodeToHLS(String inputPath, String outputDir, int width, int height, int bitrate) throws IOException, InterruptedException {
        String playlistPath = outputDir +"/playlist.m3u8";
        String segmentPattern = outputDir + "/segment_%03d.ts";

        //FFmpeg command for HTS encoding
        List<String> command = Arrays.asList(
                ffmpegPath,
                "-i",inputPath,
                "-vf","scale=" + width + ":" + height,
                "-b:v", bitrate + "k",
                "-c:a","aac",
                "-b:a", "128k",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_segment_filename", segmentPattern,
                "-f", "hls",
                playlistPath
        );
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg process exited with code " + exitCode);
            }
        } finally {
            process.destroy();
        }

    }

    private void generateMasterPlaylist(String masterPlaylistPath) throws IOException {
        StringBuilder master = new StringBuilder();
        master.append("master");
        master.append("#EXTM3U\n");
        master.append("#EXT-X-VERSION: 3\n\n");

        //Add each quality to master playlist
        int[][] qualities = {{1920,5000,1080},{1280,2800,720},{854,1200,480},{640,800,360},{426,500,240}};

        for (int[] q:qualities) {
            int width = q[0];
            int bitrate = q[1];
            int height = q[2];
            master.append("#EXT-X-STREAM-INF:BANDWIDTH=").append(bitrate*1000).append(",RESOLUTION=").append(width).append("x").append(height).append(",CODECS=\"avc1.42e01e,mp4a.40.2\"n");

            master.append(height).append("p/playlist.m3u8\n\n");

            Files.writeString(Paths.get(masterPlaylistPath), master.toString());

        }
    }

    private void uploadEncodedFilesToS3(String encodedPath, String outputDir) {
        File directory = new File(outputDir);
        uploadDirectoryToS3(directory, encodedPath,outputDir);
    }

    private void uploadDirectoryToS3(File directory, String encodedPath, String outputDir) {
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                uploadDirectoryToS3(file, encodedPath, outputDir);
            }else{
              String relativePath = file.getAbsolutePath().substring(encodedPath.length() + 1).replace("\\", "/");
              String s3Key = outputDir + relativePath;
              String contentType = file.getName().endsWith(".m3u8") ? "application/x-mpegurl" : "video/MP2T";
              PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                      .bucket(bucketName)
                      .key(s3Key)
                      .contentType(contentType)
                      .contentLength(file.length())
                      .build();
              s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
              log.info("Uploaded file to S3: {}", s3Key);
            }
        }
    }

}

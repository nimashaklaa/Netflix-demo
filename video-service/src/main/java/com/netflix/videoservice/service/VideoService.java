package com.netflix.videoservice.service;

import com.netflix.videoservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoService {

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final String VIDEO_UPLOADED_TOPIC = "video-uploaded";


    /**
     * Upload a video file to S3 and publish a VideoUploadedEvent to Kafka
     * <p>
     * FLOW->
     * 1. Receive a multipart file
     * 2. Generate a unique s3 key
     * 3. Upload the file to S3
     * 4. Publish a VideoUploadedEvent to Kafka
     * 5. Encoding Service picks up and starts EEmpeg encoding
     */

    public String uploadVideo(String movieId, MultipartFile videoFile) throws IOException {
        log.info("Uploading video for movieId: {} file {}", movieId, videoFile.getOriginalFilename());

        //Generate a unique s3 key
        String videoKey = "raw"+ movieId + "/" + UUID.randomUUID()+ "_" + videoFile.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(videoKey)
                .contentType(videoFile.getContentType())
                .contentLength(videoFile.getSize())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(videoFile.getInputStream(), videoFile.getSize()));
        log.info("Video uploaded to S3 successfully with key: {}", videoKey);

        //publish event to Kafka

        VideoUploadedEvent event = new VideoUploadedEvent(movieId, videoKey, bucketName, videoFile.getOriginalFilename(), videoFile.getSize()) ;
        kafkaTemplate.send(VIDEO_UPLOADED_TOPIC,movieId, event);

        log.info("VideoUploadedEvent published to Kafka for movieId: {} with videoKey: {}", movieId, videoKey);
        return videoKey;
    }
}

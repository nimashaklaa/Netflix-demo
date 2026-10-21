package com.netflix.streamingservice.service;


import com.netflix.streamingservice.dto.StreamingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingService {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry}")
    private long presignedUrlExpiry;

    //Redis key for streaming urls
    private final String STREAMING_KEY_CACHE_PREFIX = "streaming:url:";

    /**
     * Get streaming URL for a movie
     *
     * FLOW:
     * 1. Check redis cache for existing presigned URL
     * 2. If cached, return immediately
     * 3. If not cached = generated new presigned URL from S3
     * 4. Cache the URL in redis
     * 5. Return streaming URL
     */
    public StreamingResponse getStreamingUrl(String movieId, String streamingUrl) {
        log.info("Generating streaming url for movieId: {}", movieId);
        String streamingKey = STREAMING_KEY_CACHE_PREFIX + movieId;

        // check redis cache for existing presigned URL
        String cachedUrl = redisTemplate.opsForValue().get(streamingKey);
        if (cachedUrl != null) {
            log.info("Returning cached streaming url for movieId: {}", movieId);
            return new StreamingResponse(movieId, cachedUrl, "1080,720,480,360", presignedUrlExpiry);
        }

        //Generate new presigned url from S3
        log.info("Generating new streaming url for movieId: {}", movieId);
        String presignedUrl = generatePresignedUrl(streamingUrl);

        //cache in redis for 55 mins
        redisTemplate.opsForValue().set(streamingKey, presignedUrl, 55, TimeUnit.MINUTES);
        log.info("Cached streaming url for movieId: {}", movieId);

        return new StreamingResponse(movieId, presignedUrl, "1080,720,480,360", presignedUrlExpiry);
    }

    /**
     * Generate a presigned URL for the given S3 object key
     * URL expired after configured time.
     */
    private String generatePresignedUrl(String streamingUrl) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(streamingUrl).build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(presignedUrlExpiry)).getObjectRequest(getObjectRequest).build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Invalidate cache streaming URL for a movie
     * This is called when a new video is uploaded and encoded for the same movieId
     * so that the next request will generate a new presigned URL
     */
    public void invalidateStreamingUrl(String movieId) {
        String streamingKey = STREAMING_KEY_CACHE_PREFIX + movieId;
        redisTemplate.delete(streamingKey);
        log.info("Invalidated streaming url for movieId: {}", movieId);
    }

    public String getSignedPlaylist(String movieId, String playlistUrl) {
        //Get base path for this playlist
        String playlistPath = playlistUrl.substring(0, playlistUrl.lastIndexOf("/") + 1);
        //Read m3u8 content from S3 and replace the playlist URL with the final URL
        String m3u8Content = readFromS3(playlistPath);

        // Rewrite each line that is a segment or playlist reference
        return rewriteM3u8SignedUrls(m3u8Content, playlistPath);
    }

    private String rewriteM3u8SignedUrls(String m3u8Content, String playlistPath) {
        StringBuilder rewritten = new StringBuilder();
        for (String line : m3u8Content.split("\n")) {
            String trimmedLine = line.trim();

            // skip empty lines and comments
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                rewritten.append(line).append("\n");
                continue;
            }
            // This is a segment or playlist reference
            String fullKey = playlistPath + trimmedLine;
            String signedUrl = generatePresignedUrl(fullKey);
            rewritten.append(signedUrl).append("\n");
        }
        return rewritten.toString();
    }

    private String readFromS3(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(s3Key).build();
        ResponseInputStream<GetObjectRequest> response = s3Client.getObject(getObjectRequest);
        return new BufferedReader(new InputStreamReader(response)).lines().collect(Collectors.joining("\n"));
    }
}

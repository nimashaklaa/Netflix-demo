package com.netflix.contentservice.service;

import com.netflix.contentservice.model.VideoStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoUploadedEncodedEventConsumer {
    private final ContentService contentService;


    @KafkaListener(topics = "video-uploaded", groupId = "video-uploaded-consumer")
    public void consumerVideoUploadedEvent(@Payload Map<String, Object> payload) {
        String movieId = payload.get("movieId") != null ? payload.get("movieId").toString() : null;
        String videoKey = payload.get("video") != null ? payload.get("video").toString() :
                (payload.get("s3Key") != null ? payload.get("s3Key").toString() : null);

        if (movieId == null || videoKey == null) {
            log.error("Received malformed video-uploaded event: {}", payload);
            return;
        }

        log.info("Received video uploaded event for movieId: {}, videoKey: {}", movieId, videoKey);
        contentService.updateVideoKey(movieId, videoKey);
    }

    @KafkaListener(topics = "video-encoded")
    public void consumeVideoEncodedEvent(@Payload Map<String, Object> payload) {
        String movieId = payload.get("movieId") != null ? payload.get("movieId").toString() : null;
        String hlsUrl = payload.get("hlsUrl") != null ? payload.get("hlsUrl").toString() : null;
        boolean success = payload.get("success") != null && Boolean.parseBoolean(payload.get("success").toString());

        if (movieId == null) {
            log.error("Received malformed video-encoded event: {}", payload);
            return;
        }

        if (success) {
            contentService.updateHlsUrl(movieId, hlsUrl);
            log.info("Received successful HLS URL event for movieId: {}, hlsUrl: {}", movieId, hlsUrl);
        } else {
            String errorMessage = payload.get("errorMessage") != null ? payload.get("errorMessage").toString() : "Unknown encoding error";
            log.error("Encoding failed for movieId: {}, errorMessage: {}", movieId, errorMessage);
            contentService.updateVideoStatus(movieId, VideoStatus.FAILED);
        }
    }

}

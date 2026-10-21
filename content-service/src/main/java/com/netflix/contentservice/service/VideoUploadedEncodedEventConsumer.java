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


    @KafkaListener(topics = "video-uploaded"
    , groupId = "video-uploaded-consumer")
    public void consumerVideoUploadedEvent(
            @Payload Map<String, Object> payload
    ){
        String movieId = payload.get("movieId").toString();
        String videoKey = payload.get("videoKey").toString();
        log.info("Received video uploaded event for movieId: {}, videoKey: {}", movieId, videoKey);
        contentService.updateVideoKey(movieId, videoKey);
    }

    @KafkaListener(
            topics = "video-encoded"
    )
    public void consumeVideoEncodedEvent(
            @Payload Map<String, Object> payload
    ){
        String movieId = payload.get("movieId").toString();
        String hlsUrl = payload.get("hlsUrl").toString();
        boolean success = (boolean) payload.get("success");
        if(!success){
            contentService.updateHlsUrl(movieId, hlsUrl);
        }else{
            String errorMessages = payload.get("errorMessages").toString();
            log.error("Error uploading HLS URL for movieId: {}, errorMessages: {}", movieId, errorMessages);
            contentService.updateVideoStatus(movieId, VideoStatus.READY);

        }
        log.info("Received HLS URL event for movieId: {}, hlsUrl: {}", movieId, hlsUrl);
        contentService.updateVideoStatus(movieId, VideoStatus.READY);
    }

}

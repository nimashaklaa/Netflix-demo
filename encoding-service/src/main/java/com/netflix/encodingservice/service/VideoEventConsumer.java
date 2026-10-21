package com.netflix.encodingservice.service;

import com.netflix.encodingservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEventConsumer {

    private final EncodingService videoEncodingService;

    /**
     * Listens to video. Uploaded Kafka topic.
     * Triggered when video service uploads a raw video to S3
     *
     * FLOW
     *
     * Video Service -> s3 upload -> Kafka(video.uploaded)
     * -> This Consumer
     * -> EncodingService
     * ->FFmpeg encoding -> S3 upload -> Kafka(video.encoded)
     */
    @KafkaListener(topics = "video-uploaded", groupId = "encoding-service-group")
    public void consumeVideoUploadedEvent(VideoUploadedEvent event) {
        log.info("Received video uploaded event {}", event);
        try {
            videoEncodingService.encodeVideo(event);
        } catch (Exception e) {
            log.error("Error occurred while encoding video", e);
        }
    }

}

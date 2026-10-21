package com.netflix.streamingservice.service;

import com.netflix.streamingservice.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEncodedEventConsumer {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String MASTER_PLAYLIST_KEY_PREFIX = "streaming:playlist";


    /**
     * Listens to video.encoded event and updates the redis cache
     * Store master playlist in redis when video is encoded
     *
     * This allows StreamingService to serve the master playlist
     */
    @KafkaListener(topics = "video.encoded", groupId = "streaming-service-group")
    public void consumeVideoEncodedEvent(VideoEncodedEvent videoEncodedEvent) {
        log.info("Received video encoded event for movieId: {}", videoEncodedEvent.getMovieId());
        if (videoEncodedEvent.isSuccess()) {
            String streamingKey = MASTER_PLAYLIST_KEY_PREFIX + videoEncodedEvent.getMovieId();
            redisTemplate.opsForValue().set(streamingKey, videoEncodedEvent.getMasterPlaylistUrl());
            log.info("Master playlist url stored in redis for movieId: {}", videoEncodedEvent.getMovieId());
        } else {
            log.error("Failed to encode video for movieId: {}-{}", videoEncodedEvent.getMovieId(), videoEncodedEvent.getErrorMessage());
        }
    }
}

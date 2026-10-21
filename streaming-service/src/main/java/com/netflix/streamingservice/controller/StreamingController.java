package com.netflix.streamingservice.controller;

import com.netflix.streamingservice.dto.StreamingResponse;
import com.netflix.streamingservice.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stream")
@Slf4j
@RequiredArgsConstructor
public class StreamingController {

    private final StreamingService streamingService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String STREAMING_KEY_PREFIX = "streaming:playlist";

    /**
     * Get streaming url for a movie
     * Returns presigned HLS url for the movie if it is encoded and available in S3
     */
    @RequestMapping("/{movieId}")
    public ResponseEntity<StreamingResponse> getStreamingUrl(@PathVariable String movieId) {
        log.info("Getting streaming url for movieId: {}", movieId);

        //Check if the movie is encoded and available in S3
        String streamingKey = STREAMING_KEY_PREFIX  + movieId;
        String streamingUrl = redisTemplate.opsForValue().get(streamingKey);

        if (streamingUrl == null) {
            log.warn("Streaming url not found for movieId: {}", movieId);
            return ResponseEntity.notFound().build();
        }
        StreamingResponse response = streamingService.getStreamingUrl(movieId, streamingUrl);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a signed playlist for a movie
     * called by netflix-vod-player for each quality
     * @param movieId
     * @param streamingUrl
     * @return
     */
    @GetMapping("/{movieId}/playlist")
    public ResponseEntity<String> getSignedPlaylist(@PathVariable String movieId, @RequestParam String streamingUrl) {
        String signedPlaylist = streamingService.getSignedPlaylist(movieId, streamingUrl);

        return ResponseEntity.ok().header("Content-Type", "application/x-mpegURL").body(signedPlaylist);
    }
}

package com.netflix.videoservice.controller;

import com.netflix.videoservice.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/videos")
@Slf4j
@RequiredArgsConstructor

public class VideoController {
    private final VideoService videoService;
    /**
     * Upload video files for a movie
     * Accepts a multipart request with a video file
     * POST /api/v1/videos/upload/{movieId}
     */

    @PostMapping("/upload/{movieId}")
    public ResponseEntity<String> uploadVideo(
            @PathVariable String movieId,
            @RequestParam("file") MultipartFile videoFile
    ) throws IOException {
        log.info("Uploading video for movieId: {} file size: {}MB" , movieId, videoFile.getSize() / 1024 / 1024);

        if(videoFile.isEmpty()){
            return ResponseEntity.badRequest().body("Please provide a valid video file");
        }
        String videoKey = videoService.uploadVideo(movieId,videoFile);

        return ResponseEntity.ok(
                "Video uploaded successfully with videoKey: " + videoKey+
                        "Encoding Started automatically via kafka "
        );
    }
}

package com.netflix.videoservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event to be published when a video is uploaded
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadedEvent {
    private String movieId;
    private String s3Key;
    private String bucketName;
    private String originalFilename;
    private long fileSizeBytes;
}

package com.netflix.encodingservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadedEvent {
    private String movieId;
    private String s3Key;
    private String bucketName;
    private String originalFilename;
    private long fileSizeBytes;

    public String getVideo() {
        return s3Key;
    }

    public void setVideo(String video) {
        this.s3Key = video;
    }
}

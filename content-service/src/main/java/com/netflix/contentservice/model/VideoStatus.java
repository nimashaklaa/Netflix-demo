package com.netflix.contentservice.model;

/**
 * tracks the video processing lifecycle
 *
 * FLOW:
 * PENDING -> UPLOADED -> ENCODED -> READY
 *                                ->FAILED
 */
public enum VideoStatus {
    PENDING, UPLOADED, ENCODED, READY, FAILED
}

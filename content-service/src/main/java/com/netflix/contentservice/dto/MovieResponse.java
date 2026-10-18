package com.netflix.contentservice.dto;

import com.netflix.contentservice.model.Genre;
import com.netflix.contentservice.model.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {
    private String id;
    private String title;
    private String description;
    private Genre genre;
    private String director;
    private String cast;
    private int releaseYear;
    private double rating;
    private String thumbnailUrl;
    private int durationMinutes;
    private String videoKey;
    private String hlsUrl;
    private VideoStatus videoStatus;
    private LocalDateTime createdAt;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String title;
        private String description;
        private Genre genre;
        private String director;
        private String cast;
        private int releaseYear;
        private double rating;
        private String thumbnailUrl;
        private int durationMinutes;
        private String videoKey;
        private String hlsUrl;
        private VideoStatus videoStatus;
        private LocalDateTime createdAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder genre(Genre genre) {
            this.genre = genre;
            return this;
        }

        public Builder director(String director) {
            this.director = director;
            return this;
        }

        public Builder cast(String cast) {
            this.cast = cast;
            return this;
        }

        public Builder releaseYear(int releaseYear) {
            this.releaseYear = releaseYear;
            return this;
        }

        public Builder rating(double rating) {
            this.rating = rating;
            return this;
        }

        public Builder thumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;
        }

        public Builder durationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public Builder videoKey(String videoKey) {
            this.videoKey = videoKey;
            return this;
        }

        public Builder hlsUrl(String hlsUrl) {
            this.hlsUrl = hlsUrl;
            return this;
        }

        public Builder videoStatus(VideoStatus videoStatus) {
            this.videoStatus = videoStatus;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public MovieResponse build() {
            MovieResponse m = new MovieResponse();
            m.setId(this.id);
            m.setTitle(this.title);
            m.setDescription(this.description);
            m.setGenre(this.genre);
            m.setDirector(this.director);
            m.setCast(this.cast);
            m.setReleaseYear(this.releaseYear);
            m.setRating(this.rating);
            m.setThumbnailUrl(this.thumbnailUrl);
            m.setDurationMinutes(this.durationMinutes);
            m.setVideoKey(this.videoKey);
            m.setHlsUrl(this.hlsUrl);
            m.setVideoStatus(this.videoStatus);
            m.setCreatedAt(this.createdAt);
            return m;
        }
    }
}

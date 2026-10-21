package com.netflix.contentservice.service;

import com.netflix.contentservice.dto.MovieRequest;
import com.netflix.contentservice.dto.MovieResponse;
import com.netflix.contentservice.model.Genre;
import com.netflix.contentservice.model.Movie;
import com.netflix.contentservice.model.VideoStatus;
import com.netflix.contentservice.repository.MovieRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentService {

    private final MovieRepository movieRepository;

    /**
     * Add a new movie to the catalog
     * Video is not uploaded yet at this stage
     */
    public MovieResponse addMovie(@Valid MovieRequest movieRequest) {
        log.info("Adding movie: {}", movieRequest.getTitle());
        Movie movie = Movie.builder()
                .title(movieRequest.getTitle())
                .description(movieRequest.getDescription())
                .genre(movieRequest.getGenre())
                .director(movieRequest.getDirector())
                .cast(movieRequest.getCast())
                .releaseYear(movieRequest.getReleaseYear())
                .rating(movieRequest.getRating())
                .thumbnailUrl(movieRequest.getThumbnailUrl())
                .durationMinutes(movieRequest.getDurationMinutes())
                .videoStatus(VideoStatus.PENDING)
                .build();
        movieRepository.save(movie);
        log.info("Movie added with ID: {}", movie.getId());
        return mapToResponse(movie);
    }

    private MovieResponse mapToResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .director(movie.getDirector())
                .cast(movie.getCast())
                .releaseYear(movie.getReleaseYear())
                .rating(movie.getRating())
                .thumbnailUrl(movie.getThumbnailUrl())
                .durationMinutes(movie.getDurationMinutes())
                .videoStatus(movie.getVideoStatus())
                .build();
    }

    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<MovieResponse> getMovieByGenre(Genre genre) {
        return movieRepository.findByGenre(genre).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public MovieResponse getMovieById(String movieId) {
        return movieRepository.findById(movieId).map(this::mapToResponse).orElseThrow(() -> new RuntimeException("Movie not found :"+ movieId));
    }

    public List<MovieResponse> searchMovies(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public void updateVideoKey(String movieId, String videoKey) {
        log.info("Updating video key for movie: {}", movieId);
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found :"+ movieId));
        movie.setVideoKey(videoKey);
        movieRepository.save(movie);
    }

    public void updateHlsUrl(String movieId, String hlsUrl) {
        log.info("Updating HLS URL for movie: {}", movieId);
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found :"+ movieId));
        movie.setHlsUrl(hlsUrl);
        movie.setVideoStatus(VideoStatus.READY);
        movieRepository.save(movie);
        log.info("HLS URL updated for movie: {}", movieId);
    }

    public void updateVideoStatus(String movieId, VideoStatus videoStatus) {
        log.info("Updating video status for movie: {}", movieId);
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found :"+ movieId));
        movie.setVideoStatus(videoStatus);
        movieRepository.save(movie);
        log.info("Video status updated for movie: {}", movieId);
    }
}

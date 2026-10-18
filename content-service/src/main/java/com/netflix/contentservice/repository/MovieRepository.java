package com.netflix.contentservice.repository;

import com.netflix.contentservice.model.Genre;
import com.netflix.contentservice.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface MovieRepository extends JpaRepository<Movie, String> {
    Collection<Movie> findByGenre(Genre genre);

    Collection<Movie> findByTitleContainingIgnoreCase(String title);
}

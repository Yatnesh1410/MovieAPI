package com.movieflix.movieApi.service;

import com.movieflix.movieApi.dto.MovieDto;
import com.movieflix.movieApi.dto.MoviePageResponse;
import com.movieflix.movieApi.entities.Movie;
import com.movieflix.movieApi.exceptions.FileExistsException;
import com.movieflix.movieApi.exceptions.MovieNotFoundException;
import com.movieflix.movieApi.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final FileService fileService;

    public MovieServiceImpl(MovieRepository movieRepository, FileService fileService) {
        this.movieRepository = movieRepository;
        this.fileService = fileService;
    }

    @Value("${project.poster}")
    private String path;

    @Value("${base.url}")
    private String baseUrl;

    @Override
    public MovieDto addMovie(MovieDto movieDto, MultipartFile file) throws IOException {
        //1. Upload the file
        if(Files.exists(Paths.get(path + File.separator + file.getOriginalFilename()))){
            throw new FileExistsException("File already exists! Please enter another file name.");
        }
        String uploadedFileName = fileService.uploadFile(path, file);

        //2. Set the value of field 'poster' as filename
        movieDto.setPoster(uploadedFileName);

        //3. We map dto to movie object
        Movie movie = new Movie(
                null,
                movieDto.getTitle(),
                movieDto.getDirector(),
                movieDto.getStudio(),
                movieDto.getMovieCast(),
                movieDto.getReleaseYear(),
                movieDto.getPoster()
        );

        //4. Save the movie object ---> returns saved Movie object
        Movie savedMovie = movieRepository.save(movie);

        //5. Generate the poster url
        String posterUrl = baseUrl + "/file/" + uploadedFileName;

        //6. Map Movie object to DTO object and return
        MovieDto response = new MovieDto(
                savedMovie.getMovieId(),
                savedMovie.getTitle(),
                savedMovie.getDirector(),
                savedMovie.getStudio(),
                savedMovie.getMovieCast(),
                savedMovie.getReleaseYear(),
                savedMovie.getPoster(),
                posterUrl
        );

        return response;
    }

    @Override
    public MovieDto getMovie(Integer movieId) {
        //1. Check the data in DB and if exists fetch the data for given ID
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie not found with ID:" + movieId));

        //2. Generate poster url
        String posterUrl = baseUrl + "/file/" + movie.getPoster();

        //3. Map to MovieDto object and return
        MovieDto response = new MovieDto(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getStudio(),
                movie.getMovieCast(),
                movie.getReleaseYear(),
                movie.getPoster(),
                posterUrl
        );

        return response;
    }

    @Override
    public List<MovieDto> getAllMovies() {
        //1. fetch all data from DB
        List<Movie> movies = movieRepository.findAll();

        //2. Iterate through the list and generate poster url for each of them, add map to MovieDto
        List<MovieDto> response = new ArrayList<>();

        movies.forEach(movie -> {
            String posterUrl = baseUrl + "/file/" + movie.getPoster();

            MovieDto movieDto = new MovieDto(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl
            );

            response.add(movieDto);
        });

        return response;
    }

    @Override
    public MovieDto updateMovie(Integer movieId, MovieDto movieDto, MultipartFile file) throws IOException {
        //1.Check if movie object exists or not
        Movie existingMovie = movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie not found with ID:" + movieId));

        //2.If file is null , do nothing
        //If file is not null, delete existing file associated with record and upload new file
        String fileName = existingMovie.getPoster();

        if(file != null){
            Files.deleteIfExists(Paths.get(path + File.separator + fileName));
            fileName = fileService.uploadFile(path, file);
        }

        //3. Set MovieDto's poster value acc. to step 2
        movieDto.setPoster(fileName);

        //4. Map it to movie object
        Movie movie = new Movie(
                movieId,
                movieDto.getTitle(),
                movieDto.getDirector(),
                movieDto.getStudio(),
                movieDto.getMovieCast(),
                movieDto.getReleaseYear(),
                movieDto.getPoster()
        );

        //5.Save movie object -> returns saved movie object
        Movie updatedMovie = movieRepository.save(movie);

        //6.Generate poster url
        String posterUrl = baseUrl + "/file/" + fileName;

        //7. Map to movie DTO and return it
        MovieDto response = new MovieDto(
                updatedMovie.getMovieId(),
                updatedMovie.getTitle(),
                updatedMovie.getDirector(),
                updatedMovie.getStudio(),
                updatedMovie.getMovieCast(),
                updatedMovie.getReleaseYear(),
                updatedMovie.getPoster(),
                posterUrl
        );

        return response;
    }

    @Override
    public String deleteMovie(Integer movieId) throws IOException {
        //1. Check if movie object exists in DB.
        Movie existingMovie = movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie not found with ID:" + movieId));
        Integer idToBeDeleted = existingMovie.getMovieId();

        //2. Delete the file associated with record.
        Files.deleteIfExists(Paths.get(path + File.separator + existingMovie.getPoster()));

        //3. Delete the movie object
        movieRepository.delete(existingMovie);

        return "Movie deleted with ID:" + idToBeDeleted;
    }

    @Override
    public MoviePageResponse getAllMoviesWithPagination(Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Movie> moviePages = movieRepository.findAll(pageable);
        List<Movie> movies = moviePages.getContent();

        List<MovieDto> movieDtos = new ArrayList<>();
        movies.forEach(movie -> {
            String posterUrl = baseUrl + "/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl
            );

            movieDtos.add(movieDto);
        });


        return new MoviePageResponse(movieDtos, pageNumber, pageSize, moviePages.getTotalElements(), moviePages.getTotalPages(), moviePages.isLast());
    }

    @Override
    public MoviePageResponse getAllMoviesWithPaginationAndSorting(Integer pageNumber, Integer pageSize, String sortBy, String dir) {

        Sort sort = dir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<Movie> moviePages = movieRepository.findAll(pageable);
        List<Movie> movies = moviePages.getContent();

        List<MovieDto> movieDtos = new ArrayList<>();
        movies.forEach(movie -> {
            String posterUrl = baseUrl + "/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl
            );

            movieDtos.add(movieDto);
        });


        return new MoviePageResponse(movieDtos, pageNumber, pageSize, moviePages.getTotalElements(), moviePages.getTotalPages(), moviePages.isLast());
    }
}

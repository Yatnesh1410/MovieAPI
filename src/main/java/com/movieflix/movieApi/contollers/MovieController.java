package com.movieflix.movieApi.contollers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieflix.movieApi.dto.MovieDto;
import com.movieflix.movieApi.dto.MoviePageResponse;
import com.movieflix.movieApi.exceptions.EmptyFileException;
import com.movieflix.movieApi.service.MovieService;
import com.movieflix.movieApi.utils.AppConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/movie")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/addMovie")
    public ResponseEntity<MovieDto> addMovieHandler(@RequestPart MultipartFile file,
                                                    @RequestPart String movieDto) throws IOException, EmptyFileException {
        if(file.isEmpty()){
            throw new EmptyFileException("File is empty! Please send a valid file.");
        }

        MovieDto dto = convertToMovieDto(movieDto);
        return new ResponseEntity<>(movieService.addMovie(dto, file), HttpStatus.CREATED);
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<MovieDto> getMovieHandler(@PathVariable Integer movieId){
        return ResponseEntity.ok(movieService.getMovie(movieId));
    }

    @GetMapping("/allMovies")
    public ResponseEntity<List<MovieDto>> getMoviesHandler(){
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/allMoviesPaginated")
    public ResponseEntity<MoviePageResponse> getMoviesWithPaginationHandler(
            @RequestParam(defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize
            ){
        return ResponseEntity.ok(movieService.getAllMoviesWithPagination(pageNumber,pageSize));
    }

    @GetMapping("/allMoviesPaginatedAndSorted")
    public ResponseEntity<MoviePageResponse> getMoviesWithPaginationAndSortingHandler(
            @RequestParam(defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(defaultValue = AppConstants.SORT_BY, required = false) String sortBy,
            @RequestParam(defaultValue = AppConstants.SORT_DIR,required = false) String dir
    ){
        return ResponseEntity.ok(movieService.getAllMoviesWithPaginationAndSorting(pageNumber,pageSize, sortBy, dir));
    }

    @PutMapping("/update/{movieId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MovieDto> updateMovieHandler(@PathVariable Integer movieId,
                                                       @RequestPart MultipartFile file,
                                                       @RequestPart String movieDto) throws IOException {
        if(file.isEmpty()){
            file = null;
        }
        MovieDto dto = convertToMovieDto(movieDto);
        return ResponseEntity.ok(movieService.updateMovie(movieId, dto, file));
    }

    @DeleteMapping("/delete/{movieId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteMovieHandler(@PathVariable Integer movieId) throws IOException {
        return ResponseEntity.ok(movieService.deleteMovie(movieId));
    }

    private MovieDto convertToMovieDto(String movieDtoObj) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        MovieDto movieDto = objectMapper.readValue(movieDtoObj, MovieDto.class);
        return movieDto;
    }
}

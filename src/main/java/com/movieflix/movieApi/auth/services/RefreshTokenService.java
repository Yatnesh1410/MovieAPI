package com.movieflix.movieApi.auth.services;

import com.movieflix.movieApi.auth.entities.RefreshToken;
import com.movieflix.movieApi.auth.entities.User;
import com.movieflix.movieApi.auth.repositories.RefreshTokenRepository;
import com.movieflix.movieApi.auth.repositories.UserRepository;
import com.movieflix.movieApi.exceptions.RefreshTokenExpiredException;
import com.movieflix.movieApi.exceptions.RefreshTokenNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }


    public RefreshToken createRefreshToken(String username) {
       User user =  userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not found with email: " + username));

       RefreshToken refreshToken = user.getRefreshToken();

       if(refreshToken == null){
           long refreshTokenValidity = 30*1000;
           refreshToken = RefreshToken.builder()
                          .refreshToken(UUID.randomUUID().toString())
                          .expirationTime(Instant.now().plusMillis(refreshTokenValidity))
                          .user(user)
                          .build();

           refreshTokenRepository.save(refreshToken);
       }

       return refreshToken;
    }

    public RefreshToken verifyRefreshToken(String refreshToken) throws RefreshTokenNotFoundException, RefreshTokenExpiredException {
        RefreshToken token = refreshTokenRepository.findByRefreshToken(refreshToken)
                                                   .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh Token not found!"));

        if(token.getExpirationTime().compareTo(Instant.now()) < 0){
            refreshTokenRepository.delete(token);
            throw new RefreshTokenExpiredException("Refresh token expired!");
        }

        return token;
    }

}
package com.movieflix.movieApi.exceptions;

public class RefreshTokenExpiredException extends Throwable{
    public RefreshTokenExpiredException(String message){
        super(message);
    }
}

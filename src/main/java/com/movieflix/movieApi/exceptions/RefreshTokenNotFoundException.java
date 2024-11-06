package com.movieflix.movieApi.exceptions;

public class RefreshTokenNotFoundException extends Throwable{

    public RefreshTokenNotFoundException(String message){
        super(message);
    }
}

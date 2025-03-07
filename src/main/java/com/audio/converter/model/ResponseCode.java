package com.audio.converter.model;

import lombok.Getter;

@Getter
public enum ResponseCode {
    SUCCESS("SUCCESS", "SUCCESS"),
    SYSTEM_ERROR("SYSTEM_ERROR", "Contact our team"),
    BIND_ERROR("BIND_ERROR", "Please fill in mandatory parameter"),
    RUNTIME_ERROR("RUNTIME_ERROR", "Runtime Error"),
    CONVERSION_FAILED("CONVERSION_FAILED", "Conversion Invalid"),
    RETRIEVE_FAILED("RETRIEVE_FAILED", "Retrieve Invalid"),
    UPLOAD_FAILED("UPLOAD_FAILED", "Upload Invalid"),
    FORMAT_INVALID("FORMAT_INVALID", "Format File is invalid"),
    USER_NOT_EXIST("USER_NOT_EXIST", "User is not exist"),
    FILE_NOT_EXIST("FILE_NOT_EXIST","File is not exist"),
    PHRASE_NOT_EXIST("PHRASE_NOT_EXIST","Phrase is not exist"),
    AUDIO_NOT_EXIST("AUDIO_NOT_EXIST","Audio is not exist"),
    AUDIO_ALREADY_EXIST("AUDIO_ALREADY_EXIST","Audio already exist");


    private String code;
    private String message;

    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    }

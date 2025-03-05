package com.audio.converter.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Getter
public class BaseResponse {
    private String code;
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Resource audio;

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public BaseResponse(String code, String message, Resource audio) {
        this.code = code;
        this.message = message;
        this.audio = audio;
    }
}

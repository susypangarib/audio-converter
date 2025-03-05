package com.audio.converter.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class AudioRequest {

    private Format targetFormat;
    private File file;
    private String phraseId;
    private String userId;

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public AudioRequest(Format targetFormat, File file, String phraseId, String userId) {
        this.targetFormat = targetFormat;
        this.file = file;
        this.phraseId = phraseId;
        this.userId = userId;
    }
}

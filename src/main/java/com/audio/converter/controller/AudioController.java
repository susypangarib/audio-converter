package com.audio.converter.controller;

import com.audio.converter.model.AudioRequest;
import com.audio.converter.model.BaseResponse;
import com.audio.converter.model.Format;
import com.audio.converter.model.ResponseCode;
import com.audio.converter.service.AudioService;
import com.audio.converter.util.RequestValidationException;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/audio")
@Validated
public class AudioController {

    @Autowired
    private AudioService audioService;

    @PostMapping("/user/{userId}/phrase/{phraseId}")
    public ResponseEntity<BaseResponse> uploadAudio(
            @PathVariable @NotBlank String userId,
            @PathVariable @NotBlank String phraseId,
            @RequestPart("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new RequestValidationException(ResponseCode.BIND_ERROR.getCode(), "Uploaded file is empty");
        }

        Path tempFilePath = Files.createTempFile("upload_", "_" + file.getOriginalFilename());
        File tempFile = tempFilePath.toFile();
        Files.write(tempFilePath, file.getBytes());

        if (!audioService.retrieveAudioFormat(tempFile)){
            throw new RequestValidationException(ResponseCode.FORMAT_INVALID.getCode(), ResponseCode.FORMAT_INVALID.getMessage());
        }

        audioService.save(AudioRequest.builder()
                .targetFormat(Format.WAV)//still hardcoded
                .userId(userId)
                .phraseId(phraseId)
                .file(tempFile)
                .build());

        // Mark for deletion after processing
        Files.deleteIfExists(tempFilePath);

        return ResponseEntity.ok().body(BaseResponse.builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message(ResponseCode.SUCCESS.getMessage())
                .build());
    }

    @GetMapping("/user/{userId}/phrase/{phraseId}/{audioFormat}")
    public ResponseEntity<?> getAudio(
            @PathVariable @NotBlank String userId,
            @PathVariable @NotBlank String phraseId,
            @PathVariable @NotBlank String audioFormat) {
        Format.fromValue(audioFormat).orElseThrow(() -> new RequestValidationException(ResponseCode.FORMAT_INVALID.getCode(), ResponseCode.FORMAT_INVALID.getMessage()));
        Resource audio = audioService.get(userId, phraseId, audioFormat);
        String fileName = userId.concat("_").concat(phraseId).concat(".").concat(audioFormat);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(audio);
    }
}

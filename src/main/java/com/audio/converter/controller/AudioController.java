package com.audio.converter.controller;

import com.audio.converter.model.AudioRequest;
import com.audio.converter.model.BaseResponse;
import com.audio.converter.model.Format;
import com.audio.converter.model.ResponseCode;
import com.audio.converter.service.AudioService;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

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

        File tempFile = File.createTempFile("upload_".concat(UUID.randomUUID().toString()), "_" + file.getOriginalFilename());

        // Write the uploaded file content to tempFile
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
            fos.flush();
        }

        audioService.save(AudioRequest.builder()
                .targetFormat(Format.WAV)//still hardcoded
                .userId(userId)
                .phraseId(phraseId)
                .file(tempFile)
                .build());

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

        Resource audio = audioService.get(userId, phraseId, audioFormat);
        String fileName = userId.concat("_").concat(phraseId).concat(".").concat(audioFormat);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(audio);
    }
}

package com.audio.converter.controller;

import com.audio.converter.model.*;
import com.audio.converter.service.AudioService;
import com.audio.converter.util.RequestValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AudioControllerTest {

    @InjectMocks
    private AudioController audioController;

    @Mock
    private AudioService audioService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    public AudioRequest request = AudioRequest.builder()
            .userId("4d9d6b40-e8f4-47ab-a10c-a9d3791ff2b8")
            .phraseId("4d9d6b40-e8f4-47ab-a10c-a9d3791ff2b9")
            .file(new File("test.m4a"))
            .build();

    @Test
    void testUploadAudio_Success() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "test.m4a", "audio/mpeg", "test audio content".getBytes());

        when(audioService.retrieveAudioFormat(any(File.class))).thenReturn(true);

        ResponseEntity<BaseResponse> response = audioController.uploadAudio(request.getUserId(), request.getPhraseId(), file);

        assertNotNull(response);
        assertEquals(ResponseCode.SUCCESS.getCode(), response.getBody().getCode());
        assertEquals(ResponseCode.SUCCESS.getMessage(), response.getBody().getMessage());

        verify(audioService, times(1)).retrieveAudioFormat(any(File.class));
        verify(audioService, times(1)).save(any(AudioRequest.class));
    }

    @Test
    void testUploadAudio_InvalidFormat() throws IOException {
         MultipartFile file = new MockMultipartFile("file", "test.mp3", "audio/mpeg", "test audio content".getBytes());

        when(audioService.retrieveAudioFormat(any(File.class))).thenReturn(false);

        RequestValidationException exception = assertThrows(RequestValidationException.class, () -> {
            audioController.uploadAudio(request.getUserId(), request.getPhraseId(), file);
        });

        assertEquals(ResponseCode.FORMAT_INVALID.getCode(), exception.getCode());
        assertEquals(ResponseCode.FORMAT_INVALID.getMessage(), exception.getMessage());

        verify(audioService, times(1)).retrieveAudioFormat(any(File.class));
        verify(audioService, never()).save(request);
    }

    @Test
    void testGetAudio_Success() throws IOException {
         String audioFormat = "wav";
        Resource mockResource = mock(Resource.class);

        when(audioService.get(request.getUserId(), request.getPhraseId(), audioFormat)).thenReturn(mockResource);

        ResponseEntity<?> response = audioController.getAudio(request.getUserId(), request.getPhraseId(), audioFormat);

        assertNotNull(response);
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(mockResource, response.getBody());

        verify(audioService, times(1)).get(request.getUserId(), request.getPhraseId(), audioFormat);
    }

    @Test
    void testGetAudio_InvalidFormat() {
        String audioFormat = "invalidFormat";

        RequestValidationException exception = assertThrows(RequestValidationException.class, () -> {
            audioController.getAudio(request.getUserId(), request.getPhraseId(), audioFormat);
        });

        assertEquals(ResponseCode.FORMAT_INVALID.getCode(), exception.getCode());
        assertEquals(ResponseCode.FORMAT_INVALID.getMessage(), exception.getMessage());

        verify(audioService, never()).get(anyString(), anyString(), anyString());
    }
}
package com.audio.converter.service;

import com.audio.converter.model.AudioRequest;
import com.audio.converter.model.Format;
import com.audio.converter.model.entity.Audio;
import com.audio.converter.model.entity.Phrase;
import com.audio.converter.model.entity.User;
import com.audio.converter.repository.AudioRepository;
import com.audio.converter.repository.PhraseRepository;
import com.audio.converter.repository.UserRepository;
import com.audio.converter.util.BusinessLogicException;
import com.audio.converter.util.RequestValidationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AudioServiceImplTest {

    @InjectMocks
    private AudioServiceImpl audioService;

    @Mock
    private AudioRepository audioRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhraseRepository phraseRepository;

    @Mock
    private GCPService gcpService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private User user = User.builder()
            .id("4d9d6b40-e8f4-47ab-a10c-a9d3791ff2b8")
            .name("pumpkin")
            .build();

    public Phrase phrase = Phrase.builder()
            .id("4d9d6b40-e8f4-47ab-a10c-a9d3791ff2b9")
            .text("pumpkin")
            .build();
    public AudioRequest request = AudioRequest.builder()
            .userId("4d9d6b40-e8f4-47ab-a10c-a9d3791ff2b8")
            .phraseId("4d9d6b40-e8f4-47ab-a10c-a9d3791ff2b9")
            .file(new File("test.m4a"))
            .build();

    public Audio audio = Audio.builder()
            .userId("pumpkin")
            .phraseId("create")
            .path("converted-audio/4d9d6b40-e8f4-47ab-a10c-a9d3791ff2b9.wav")
            .build();

    private final String filePath = "gs://bucket/audio.wav";
    @Test(expected = RequestValidationException.class)
    public void testSave_UserNotExist_ShouldThrowException() {
        when(userRepository.findById(request.getUserId())).thenReturn(Optional.empty());
        audioService.save(request);
    }
    @Test(expected = RequestValidationException.class)
    public void testSave_PhraseNotExist_ShouldThrowException() {
        when(userRepository.findById(request.getUserId())).thenReturn(Optional.of(user));
        when(phraseRepository.findById(request.getPhraseId())).thenReturn(Optional.empty());

        audioService.save(request);
    }
    @Test(expected = RequestValidationException.class)
    public void testSave_AlreadyExist_ShouldThrowException() {
        when(userRepository.findById(request.getUserId())).thenReturn(Optional.of(user));
        when(phraseRepository.findById(request.getPhraseId())).thenReturn(Optional.of(phrase));
        when(audioRepository.findByUserIdAndPhraseAndDeletedAtIsNull(request.getUserId(), request.getPhraseId())).thenReturn(audio);

        audioService.save(request);
    }

    @Test
    public void testSave_ValidRequest_ShouldSaveAudio() throws Exception {
        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(phraseRepository.findById(anyString())).thenReturn(Optional.of(phrase));

        // Ensure uploadFile() is correctly mocked
        doReturn("gcs_url").when(gcpService).uploadFile(any(File.class));

        Path originalPath = Paths.get(getClass().getClassLoader().getResource("test-audio.m4a").toURI());
        Path tempFile = Files.createTempFile("test-audio-copy", ".m4a");
        Files.copy(originalPath, tempFile, StandardCopyOption.REPLACE_EXISTING);

        File file = tempFile.toFile();

        AudioRequest audio = request;
        audio.setFile(file);

       assertTrue(file.exists());
        request.setFile(file);

        try {
            boolean result = audioService.save(request);
            assertTrue(result);
            verify(audioRepository, times(1)).save(any(Audio.class));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception occurred: " + e.getMessage());
        }
    }


    @Test(expected = BusinessLogicException.class)
    public void testSave_ConversionFail_ShouldThrowException() throws Exception {
        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(phraseRepository.findById(anyString())).thenReturn(Optional.of(phrase));
        when(audioRepository.findByUserIdAndPhraseAndDeletedAtIsNull(anyString(), anyString())).thenReturn(null);

        AudioRequest request = AudioRequest.builder()
                .userId("pumpkin")
                .phraseId("create")
                .file(new File("invalid.m4a"))
                .build();

        audioService.save(request);
    }

    @Test
    public void testGet_ValidRequest_ShouldReturnResource() throws Exception {
        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(phraseRepository.findById(anyString())).thenReturn(Optional.of(phrase));
        when(audioRepository.findByUserIdAndPhraseAndDeletedAtIsNull(anyString(), anyString()))
                .thenReturn(audio);
        when(gcpService.getFileBytes(anyString())).thenReturn(new ByteArrayResource(new byte[10]));

        Resource resource = audioService.get(request.getUserId(), request.getPhraseId(), Format.WAV.getValue());
        assertNotNull(resource);
    }

    @Test(expected = RequestValidationException.class)
    public void testGet_AudioNotExist_ShouldThrowException() {
        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(phraseRepository.findById(anyString())).thenReturn(Optional.of(phrase));
        when(audioRepository.findByUserIdAndPhraseAndDeletedAtIsNull(anyString(), anyString()))
                .thenReturn(null);

        audioService.get(user.getId(), phrase.getId(), Format.WAV.getValue());
    }

    @Test
    public void testGet_Success_WAVFormat() throws IOException {
        Resource resource = new ClassPathResource("test-audio.wav");
        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(phraseRepository.findById(anyString())).thenReturn(Optional.of(phrase));
        when(audioRepository.findByUserIdAndPhraseAndDeletedAtIsNull(request.getUserId(), request.getPhraseId())).thenReturn(audio);
        when(gcpService.getFileBytes(anyString())).thenReturn(resource);

        Resource result = audioService.get(request.getUserId(), request.getPhraseId(), Format.WAV.getValue());

        assertNotNull(result);
        assertEquals(resource, result);
    }
    @Test
    public void testGet_Success_M4aFormat() throws IOException {
        Resource resource = new ClassPathResource("test-audio.wav");
        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(phraseRepository.findById(anyString())).thenReturn(Optional.of(phrase));
        when(audioRepository.findByUserIdAndPhraseAndDeletedAtIsNull(request.getUserId(), request.getPhraseId())).thenReturn(audio);
        when(gcpService.getFileBytes(anyString())).thenReturn(resource);

        Resource result = audioService.get(request.getUserId(), request.getPhraseId(), Format.M4A.getValue());
        // Validate the result
        assertNotNull(result);
    }
    @Test
    public void testGet_Success_Mp3Format() throws IOException {
        Resource resource = new ClassPathResource("test-audio.wav");
        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(phraseRepository.findById(anyString())).thenReturn(Optional.of(phrase));
        when(audioRepository.findByUserIdAndPhraseAndDeletedAtIsNull(request.getUserId(), request.getPhraseId())).thenReturn(audio);
        when(gcpService.getFileBytes(anyString())).thenReturn(resource);

        Resource result = audioService.get(request.getUserId(), request.getPhraseId(), Format.MP3.getValue());
        // Validate the result
        assertNotNull(result);
    }

    @Test
    public void testRetrieveAudioFormat_ValidM4AFile_ShouldReturnTrue() throws URISyntaxException, IOException {
        Path originalPath = Paths.get(getClass().getClassLoader().getResource("test-audio.m4a").toURI());
        Path tempFile = Files.createTempFile("test-audio-copy", ".m4a");
        Files.copy(originalPath, tempFile, StandardCopyOption.REPLACE_EXISTING);

        File file = tempFile.toFile();

        boolean result = audioService.retrieveAudioFormat(file);
        file.delete();

        assertTrue(result);
    }

    @Test
    public void testRetrieveAudioFormat_InvalidFile_ShouldReturnFalse() throws IOException {
        File tempFile = File.createTempFile("test-audio", ".txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("This is a text file, not an audio file.");
        }

        boolean result = audioService.retrieveAudioFormat(tempFile);
        tempFile.delete();

        assertFalse(result);
    }
}

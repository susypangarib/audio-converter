package com.audio.converter.service;

import com.audio.converter.model.AudioRequest;
import com.audio.converter.model.Format;
import com.audio.converter.model.ResponseCode;
import com.audio.converter.model.entity.Audio;
import com.audio.converter.repository.AudioRepository;
import com.audio.converter.repository.PhraseRepository;
import com.audio.converter.repository.UserRepository;
import com.audio.converter.util.BusinessLogicException;
import com.audio.converter.util.RequestValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AudioServiceImpl implements AudioService {

    @Autowired
    private AudioRepository audioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PhraseRepository phraseRepository;

    @Autowired
    private GCPService gcpService;

    private static final String FFMPEG_PATH = "/usr/local/bin/ffmpeg";

    @Override
    public Boolean save(AudioRequest request) {
        validateRequest(request.getPhraseId(), request.getUserId());

        Audio audio = audioRepository.findByUserIdAndPhraseAndDeletedAtIsNull(request.getPhraseId(), request.getUserId());
        if (audio != null) {
            throw new RequestValidationException(ResponseCode.AUDIO_ALREADY_EXIST.getCode(), ResponseCode.AUDIO_ALREADY_EXIST.getMessage());
        }

        //convert from m4a to wav
        String gcsUrl = "";
        try {

            // Convert M4A to WAV
            File outputFile = new File(request.getFile().getParent(), UUID.randomUUID() + ".wav");
            convertM4AToWAV(request.getFile().getAbsolutePath(), outputFile.getAbsolutePath());

            // Upload to GCS
            gcsUrl = gcpService.uploadFile(outputFile);

            // Clean up temp files
            request.getFile().delete();
            outputFile.delete();
        } catch (Exception e) {
            log.info("Fail to Upload the file", e.getMessage());
            throw new BusinessLogicException(ResponseCode.UPLOAD_FAILED.getCode(), ResponseCode.UPLOAD_FAILED.getMessage());
        }
        audioRepository.save(Audio.builder()
                .path(gcsUrl)
                .updatedBy(request.getUserId())
                .createdBy(request.getUserId())
                .convertedFormat(Format.WAV.getValue())
                .phraseId(request.getPhraseId())
                .userId(request.getUserId())
                .originalFormat(Format.M4A.getValue())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        return true;
    }

    private void validateRequest(String phraseId, String userId) {
        userRepository.findById(userId).orElseThrow(() ->
                new RequestValidationException(ResponseCode.USER_NOT_EXIST.getCode(), ResponseCode.USER_NOT_EXIST.getMessage()));
        phraseRepository.findById(phraseId).orElseThrow(() ->
                new RequestValidationException(ResponseCode.PHRASE_NOT_EXIST.getCode(), ResponseCode.PHRASE_NOT_EXIST.getMessage()));
    }

    @Override
    public Resource get(String phraseId, String userId, String format) {

        validateRequest(phraseId, userId);
        Audio audio = audioRepository.findByUserIdAndPhraseAndDeletedAtIsNull(userId, phraseId);
        Optional.of(audio)
                .orElseThrow(() -> new RequestValidationException(ResponseCode.AUDIO_NOT_EXIST.getCode(), ResponseCode.AUDIO_NOT_EXIST.getMessage()));
        // if exist -> retrieve from server by path

        // then convert to targeted format
        // then return to user
        Resource file;
        try {
            //File downloadedFile = gcpService.downloadFile("converted-audio/sample.wav", "/tmp/sample.wav");
            file = gcpService.getFileBytes(audio.getPath());
            if (format.equals(Format.M4A.getValue())) {
                file = convertWAVToM4A(file);
            } else if (format.equals(Format.MP3.getValue())) {
                file = convertWavToMp3(file);
            }
        } catch (IOException e) {
            log.info("Error ", e.getMessage());
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log.info("Error ", e.getMessage());
            throw new RuntimeException(e);
        }

        return file;
    }

    @Override
    public String retrieveAudioFormat(File file) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=format_name",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file.getAbsolutePath()
        );

        Process process = null;
        try {
            process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.readLine(); // Returns format (e.g., "mp4", "m4a", "wav")
        } catch (IOException e) {
            log.info("error retrieve audio format", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void convertM4AToWAV(String inputPath, String outputPath) throws IOException {
        // Run FFmpeg command
        ProcessBuilder builder = new ProcessBuilder(
                FFMPEG_PATH, "-i", inputPath, "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2", outputPath);

        builder.redirectErrorStream(true);
        Process process = builder.start();

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg conversion failed.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Conversion process interrupted", e);
        }
    }

    public Resource convertWAVToM4A(Resource wavResource) throws IOException, InterruptedException {
        // Create temp input and output files
        File tempInputFile = File.createTempFile("input_", ".wav");
        File tempOutputFile = File.createTempFile("output_", ".m4a");

        // Copy Resource to temp file
        try (InputStream inputStream = wavResource.getInputStream()) {
            Files.copy(inputStream, tempInputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Run FFmpeg command
        ProcessBuilder builder = new ProcessBuilder(
                FFMPEG_PATH, "-i", tempInputFile.getAbsolutePath(),
                "-c:a", "aac", "-b:a", "192k", tempOutputFile.getAbsolutePath()
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg conversion failed.");
        }

        // Read output file into ByteArrayResource
        byte[] fileBytes = Files.readAllBytes(tempOutputFile.toPath());
        Resource outputResource = new ByteArrayResource(fileBytes);

        // Clean up temp files
        tempInputFile.delete();
        tempOutputFile.delete();

        return outputResource;
    }

    public ByteArrayResource convertWavToMp3(Resource inputResource) throws IOException, InterruptedException {
        // Create a temp file for the input WAV
        File inputFile = File.createTempFile("input_", ".wav");
        Files.copy(inputResource.getInputStream(), inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Create a temp file for the output MP3
        File outputFile = new File(inputFile.getParent(), UUID.randomUUID() + ".mp3");

        // Run FFmpeg conversion
        ProcessBuilder builder = new ProcessBuilder(
                FFMPEG_PATH, "-i", inputFile.getAbsolutePath(),
                "-codec:a", "libmp3lame", "-b:a", "192k", outputFile.getAbsolutePath()
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();
        int exitCode = process.waitFor();

        // Cleanup input file
        inputFile.delete();

        if (exitCode != 0) {
            throw new IOException("FFmpeg conversion failed.");
        }

        // Return the MP3 file as a Resource
        ByteArrayResource mp3Resource = new ByteArrayResource(Files.readAllBytes(outputFile.toPath()));

        // Cleanup output file
        outputFile.delete();

        return mp3Resource;
    }

}

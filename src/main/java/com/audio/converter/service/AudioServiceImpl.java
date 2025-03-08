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
import java.util.Objects;
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
    //private static final String FFMPEG_PATH = "/usr/bin/ffmpeg";

    @Override
    public Boolean save(AudioRequest request) {
        validateRequest(request.getUserId(), request.getPhraseId());

        Audio audio = audioRepository.findByUserIdAndPhraseAndDeletedAtIsNull(request.getUserId(), request.getPhraseId());
        if (Objects.nonNull(audio)) {
            throw new RequestValidationException(ResponseCode.AUDIO_ALREADY_EXIST.getCode(), ResponseCode.AUDIO_ALREADY_EXIST.getMessage());
        }

        //convert from m4a to wav
        File outputFile = null;
        String gcsUrl = "";
        try {
            // Convert M4A to WAV
            outputFile = new File(request.getFile().getParent(), UUID.randomUUID() + ".wav");
            convertM4AToWAV(request.getFile().getAbsolutePath(), outputFile.getAbsolutePath());
            // Upload to GCS
            gcsUrl = gcpService.uploadFile(outputFile);
        } catch (Exception e) {
            log.error("Fail to Upload the file", e);
            throw new BusinessLogicException(ResponseCode.UPLOAD_FAILED.getCode(), ResponseCode.UPLOAD_FAILED.getMessage());
        } finally {
            // Clean up temp files
            request.getFile().delete();
            outputFile.delete();
        }
        try{ audioRepository.save(Audio.builder()
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

        }catch (Exception e){
            log.error("Fail to save Audio to repository {}", e);
            throw new BusinessLogicException(ResponseCode.UPLOAD_FAILED.getCode(), ResponseCode.UPLOAD_FAILED.getMessage());
        }
        return true;
    }

    private void validateRequest(String userId, String phraseId) {
        userRepository.findById(userId).orElseThrow(() ->
                new RequestValidationException(ResponseCode.USER_NOT_EXIST.getCode(), ResponseCode.USER_NOT_EXIST.getMessage()));
        phraseRepository.findById(phraseId).orElseThrow(() ->
                new RequestValidationException(ResponseCode.PHRASE_NOT_EXIST.getCode(), ResponseCode.PHRASE_NOT_EXIST.getMessage()));
    }

    @Override
    public Resource get(String userId, String phraseId, String format) {

        validateRequest(userId, phraseId);
        Audio audio = audioRepository.findByUserIdAndPhraseAndDeletedAtIsNull(userId, phraseId);

        if (Objects.isNull(audio)) {
            throw new RequestValidationException(ResponseCode.AUDIO_NOT_EXIST.getCode(), ResponseCode.AUDIO_NOT_EXIST.getMessage());
        }

        // If path exist in db, retrieve from server and convert to requested format.
        Resource file;
        try {
            file = gcpService.getFileBytes(audio.getPath());
            if (format.equals(Format.M4A.getValue())) {
                file = convertWAVToM4A(file);
            } else if (format.equals(Format.MP3.getValue())) {
                file = convertWavToMp3(file);
            }
        } catch (IOException e) {
            log.error("Failed to retrieve or process file: path={}, error={}", audio.getPath(), e);
            throw new BusinessLogicException(ResponseCode.RETRIEVE_FAILED.getCode(), ResponseCode.RETRIEVE_FAILED.getMessage());
        } catch (InterruptedException e) {
            log.error("File conversion interrupted: path={}, error={}", audio.getPath(), e);
            throw new BusinessLogicException(ResponseCode.CONVERSION_FAILED.getCode(), ResponseCode.CONVERSION_FAILED.getMessage());
        }

        return file;
    }

    @Override
    public Boolean retrieveAudioFormat(File file) {
        try {
            // Command to analyze the file using FFmpeg
            ProcessBuilder processBuilder = new ProcessBuilder(
                    FFMPEG_PATH,
                    "-i", file.getAbsolutePath(),
                    "-f", "null",
                    "-"
            );

            // Start the process
            Process process = processBuilder.start();

            // Read the output (error stream, as FFmpeg outputs info to stderr)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Wait for the process to complete
            process.waitFor();
            // Check if the output contains the M4A format
            String outputStr = output.toString().toLowerCase();
            boolean isM4aContainer = outputStr.contains("input #0, mov,mp4,m4a,3gp,3g2,mj2");
            boolean isAacCodec = outputStr.contains("audio: aac");

            // Return true if both conditions are met
            return isM4aContainer && isAacCodec;
        } catch (IOException | InterruptedException e) {
            log.error("FFmpeg reda failed {}", e);

            // If an error occurs, assume the file is not valid
            return false;
        }

    }

    private void convertM4AToWAV(String inputPath, String outputPath) throws IOException, InterruptedException {
        // Run FFmpeg command
        ProcessBuilder builder = new ProcessBuilder(
                FFMPEG_PATH, "-i", inputPath, "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2", outputPath);

        builder.redirectErrorStream(true);
        Process process = builder.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("FFmpeg conversion failed with exit code {}", exitCode);
            throw new BusinessLogicException(ResponseCode.CONVERSION_FAILED.getCode(), "FFmpeg conversion failed.");
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
                FFMPEG_PATH, "-y", "-i", tempInputFile.getAbsolutePath(),
                "-c:a", "aac", "-b:a", "192k", tempOutputFile.getAbsolutePath()
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("FFmpeg conversion failed with exit code {}", exitCode);
            throw new BusinessLogicException(ResponseCode.CONVERSION_FAILED.getCode(), "FFmpeg conversion failed.");
        }

        // Read output file into ByteArrayResource
        byte[] fileBytes = Files.readAllBytes(tempOutputFile.toPath());
        Resource outputResource = new ByteArrayResource(fileBytes);

        // Delete temp files after reading
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
            log.error("FFmpeg conversion failed with exit code {}", exitCode);
            throw new BusinessLogicException(ResponseCode.CONVERSION_FAILED.getCode(), "FFmpeg conversion failed.");
        }

        // Return the MP3 file as a Resource
        ByteArrayResource mp3Resource = new ByteArrayResource(Files.readAllBytes(outputFile.toPath()));

        // Cleanup output file
        outputFile.delete();

        return mp3Resource;
    }

}

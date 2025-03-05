package com.audio.converter.service;

import com.audio.converter.model.AudioRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

public interface AudioService {
    Boolean save(AudioRequest request);
    Resource get(String phraseId, String userId, String format);

    String retrieveAudioFormat(File file) throws IOException ;

}

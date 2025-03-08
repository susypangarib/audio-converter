package com.audio.converter.service;

import com.audio.converter.model.AudioRequest;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

public interface AudioService {
    Boolean save(AudioRequest request);
    Resource get(String userID, String phraseId, String format);

    Boolean retrieveAudioFormat(File file) throws IOException ;

}

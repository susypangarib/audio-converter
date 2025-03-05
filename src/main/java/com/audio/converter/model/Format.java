package com.audio.converter.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum Format {
    WAV("wav"),
    M4A("m4a"),
    MP3("mp3");

    private final String value;

    Format(String value) {
        this.value = value;
    }

    public static Optional<Format> fromValue(String value) {
        return Arrays.stream(Format.values())
                .filter(state -> state.getValue().equalsIgnoreCase(value))
                .findFirst();
    }


}

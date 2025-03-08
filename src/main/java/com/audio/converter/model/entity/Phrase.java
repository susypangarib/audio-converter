package com.audio.converter.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "phrases")
public class Phrase {
    @Id
    @GeneratedValue(generator = "UUID")
    private String id;
    @Column(name = "text", nullable = false)
    private String text;

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public Phrase(String id, String text) {
        this.id = id;
        this.text = text;
    }
}

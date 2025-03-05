package com.audio.converter.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "phrases")
public class Phrase {
    @Id
    @GeneratedValue(generator = "UUID")
    private String id;
    @Column(name = "text", nullable = false)
    private String name;
}

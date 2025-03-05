package com.audio.converter.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(generator = "UUID")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;
}

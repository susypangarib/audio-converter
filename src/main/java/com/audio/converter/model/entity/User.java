package com.audio.converter.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(generator = "UUID")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }
}

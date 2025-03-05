package com.audio.converter.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(
        name = "audio",
        indexes = {
                @Index(name = "idx_user_phrase_deleted", columnList = "user_id,phrase_id,deleted_at"),
        }
)
public class Audio {
    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @Column(name = "original_format", nullable = false, length = 10)
    private String originalFormat;

    @Column(name = "converted_format", nullable = false, length = 10)
    private String convertedFormat;

    @Column(name = "path", nullable = false, length = 1000)
    private String path;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "phrase_id", nullable = false)
    private String phraseId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public Audio(String id, String originalFormat, String convertedFormat, String path, String description, String userId, String phraseId, LocalDateTime createdAt, String createdBy, String updatedBy, LocalDateTime updatedAt, String deletedBy, LocalDateTime deletedAt) {
        this.id = id;
        this.originalFormat = originalFormat;
        this.convertedFormat = convertedFormat;
        this.path = path;
        this.description = description;
        this.userId = userId;
        this.phraseId = phraseId;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.deletedBy = deletedBy;
        this.deletedAt = deletedAt;
    }
}

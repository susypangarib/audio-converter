package com.audio.converter.repository;

import com.audio.converter.model.entity.Audio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AudioRepository extends JpaRepository<Audio, String> {
    @Query("SELECT audio FROM Audio audio WHERE audio.userId = :userId and audio.phraseId = :phraseId and audio.deletedAt IS null")
    Audio findByUserIdAndPhraseAndDeletedAtIsNull(String userId, String phraseId);

}
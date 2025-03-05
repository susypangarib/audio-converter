package com.audio.converter.repository;

import com.audio.converter.model.entity.Audio;
import com.audio.converter.model.entity.Phrase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.concurrent.Phaser;

public interface PhraseRepository extends JpaRepository<Phrase, String> {

}
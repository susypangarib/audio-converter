package com.audio.converter.repository;

import com.audio.converter.model.entity.Phrase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhraseRepository extends JpaRepository<Phrase, String> {

}
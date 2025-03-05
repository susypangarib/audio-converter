package com.audio.converter.repository;

import com.audio.converter.model.entity.Audio;
import com.audio.converter.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

}
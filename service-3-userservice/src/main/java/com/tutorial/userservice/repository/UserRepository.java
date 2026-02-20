package com.tutorial.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.tutorial.userservice.model.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

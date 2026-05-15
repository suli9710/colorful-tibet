package com.tibet.tourism.repository;

import com.tibet.tourism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    Boolean existsByUsername(String username);
    Boolean existsByUsernameIgnoreCase(String username);
    Optional<User> findByNickname(String nickname);
    Boolean existsByNickname(String nickname);
    List<User> findByCreatedAtAfterOrderByCreatedAtAsc(LocalDateTime createdAt);
}

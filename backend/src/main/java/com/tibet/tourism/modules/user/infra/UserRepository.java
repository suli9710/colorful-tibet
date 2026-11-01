package com.tibet.tourism.modules.user.infra;
import com.tibet.tourism.modules.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    Boolean existsByUsername(String username);
    Boolean existsByUsernameIgnoreCase(String username);
    Optional<User> findByNickname(String nickname);
    Boolean existsByNickname(String nickname);
    @Query("SELECT u.username FROM com.tibet.tourism.modules.user.domain.User u WHERE u.role = :role")
    List<String> findUsernamesByRole(@Param("role") User.Role role);
    List<User> findByCreatedAtAfterOrderByCreatedAtAsc(LocalDateTime createdAt);

    @Query("SELECT u.city, COUNT(u) FROM com.tibet.tourism.modules.user.domain.User u WHERE u.city IS NOT NULL AND u.city <> '' GROUP BY u.city")
    List<Object[]> countByCityGroup();
}

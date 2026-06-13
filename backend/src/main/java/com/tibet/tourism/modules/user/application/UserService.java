package com.tibet.tourism.modules.user.application;
import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BusinessException("Username already exists");
        }
        String plainPassword = user.getPassword();
        validatePassword(plainPassword);
        user.setPassword(passwordEncoder.encode(plainPassword)); // BCrypt哈希
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            return user.get();
        }
        throw new BusinessException("Invalid username or password");
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("Old password is incorrect");
        }

        validatePassword(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(newPassword)); // BCrypt哈希
        user.setMustChangePassword(false);
        user.incrementSessionVersion();
        userRepository.save(user);
    }

    private void validatePassword(String password) {
        InputSanitizer.validatePassword(password);
    }
}

package com.tibet.tourism.service;

import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        String plainPassword = user.getPassword();
        // 双重存储：BCrypt用于验证，AES用于管理员查看
        user.setPassword(passwordEncoder.encode(plainPassword)); // BCrypt哈希
        user.setEncryptedPassword(passwordEncryptionService.encrypt(plainPassword)); // AES加密
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            return user.get();
        }
        throw new RuntimeException("Invalid username or password");
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }
        
        // 双重存储：BCrypt用于验证，AES用于管理员查看
        user.setPassword(passwordEncoder.encode(newPassword)); // BCrypt哈希
        user.setEncryptedPassword(passwordEncryptionService.encrypt(newPassword)); // AES加密
        userRepository.save(user);
    }
    
    /**
     * 解密用户密码（仅管理员可用）
     */
    public String decryptPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getEncryptedPassword() == null || user.getEncryptedPassword().isEmpty()) {
            return null; // 旧用户可能没有加密密码
        }
        
        try {
            return passwordEncryptionService.decrypt(user.getEncryptedPassword());
        } catch (Exception e) {
            throw new RuntimeException("密码解密失败: " + e.getMessage());
        }
    }
}

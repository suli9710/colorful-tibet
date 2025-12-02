package com.tibet.tourism.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 密码可逆加密服务
 * 使用AES加密算法，仅管理员可以解密查看原始密码
 */
@Service
public class PasswordEncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 128; // AES-128
    
    // 从配置文件读取密钥，如果没有则使用默认密钥（生产环境应该使用环境变量）
    @Value("${app.encryption.key:tibetTourismSecretKey12345678901234567890}")
    private String encryptionKey;
    
    private SecretKeySpec getSecretKey() {
        // 确保密钥长度为16字节（AES-128）
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 16) {
            // 如果密钥太短，进行填充
            byte[] paddedKey = new byte[16];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 16));
            return new SecretKeySpec(paddedKey, ALGORITHM);
        } else if (keyBytes.length > 16) {
            // 如果密钥太长，截取前16字节
            byte[] truncatedKey = new byte[16];
            System.arraycopy(keyBytes, 0, truncatedKey, 0, 16);
            return new SecretKeySpec(truncatedKey, ALGORITHM);
        }
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * 加密密码（可逆）
     * @param plainPassword 原始密码
     * @return Base64编码的加密字符串
     */
    public String encrypt(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return null;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
            byte[] encryptedBytes = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
    
    /**
     * 解密密码（仅管理员可用）
     * @param encryptedPassword Base64编码的加密字符串
     * @return 原始密码
     */
    public String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return null;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("密码解密失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成加密密钥（用于初始化，仅需运行一次）
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("密钥生成失败", e);
        }
    }
}







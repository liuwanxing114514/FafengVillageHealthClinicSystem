package com.fafeng.clinic.ai.config;

import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key AES-256-GCM 加密存储。主密钥来自 env {@code CLINIC_SETTINGS_ENCRYPTION_KEY}（Base64 32 字节）。
 */
@Component
public class SecretEncryptor {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final byte[] keyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecretEncryptor(@Value("${clinic.settings.encryption-key:}") String encryptionKeyBase64) {
        if (encryptionKeyBase64 == null || encryptionKeyBase64.isBlank()) {
            this.keyBytes = null;
        } else {
            this.keyBytes = Base64.getDecoder().decode(encryptionKeyBase64.trim());
            if (this.keyBytes.length != 32) {
                throw new IllegalStateException("CLINIC_SETTINGS_ENCRYPTION_KEY must decode to 32 bytes");
            }
        }
    }

    public boolean isEncryptionAvailable() {
        return keyBytes != null;
    }

    public void requireEncryptionForPersist() {
        if (!isEncryptionAvailable()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "未配置 CLINIC_SETTINGS_ENCRYPTION_KEY，无法在设置页保存 API 密钥");
        }
    }

    public String encrypt(String plaintext) {
        requireEncryptionForPersist();
        if (plaintext == null || plaintext.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "API 密钥加密失败");
        }
    }

    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            return "";
        }
        if (!isEncryptionAvailable()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "未配置加密密钥，无法解密 API 密钥");
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64.trim());
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "API 密钥解密失败");
        }
    }

    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }
}

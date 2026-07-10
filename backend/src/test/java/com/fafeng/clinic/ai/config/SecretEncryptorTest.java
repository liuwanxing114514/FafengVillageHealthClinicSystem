package com.fafeng.clinic.ai.config;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretEncryptorTest {

    private static final String KEY_BASE64 = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptDecryptRoundTrip() {
        SecretEncryptor encryptor = new SecretEncryptor(KEY_BASE64);
        assertTrue(encryptor.isEncryptionAvailable());
        String plain = "sk-test-secret-key-12345678";
        String encrypted = encryptor.encrypt(plain);
        assertFalse(encrypted.equals(plain));
        assertEquals(plain, encryptor.decrypt(encrypted));
    }

    @Test
    void maskApiKeyShowsLastFour() {
        assertEquals("****5678", SecretEncryptor.maskApiKey("sk-12345678"));
        assertEquals("****", SecretEncryptor.maskApiKey("ab"));
    }
}

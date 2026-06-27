package dev.skymetron.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecretManager tests")
class SecretManagerTest {

    SecretManager manager;

    @BeforeEach
    void setUp() {
        String key = "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=";
        manager = new SecretManager(key);
    }

    @Test
    @DisplayName("encrypt() and decrypt() are reversible")
    void encryptDecrypt() {
        String original = "sk-or-v1-test-api-key-12345";
        String encrypted = manager.encrypt(original);
        assertThat(encrypted).isNotEqualTo(original);
        String decrypted = manager.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("encrypt() produces different output for same input (IV random)")
    void encryptUniqueEachTime() {
        String plain = "test-value";
        String e1 = manager.encrypt(plain);
        String e2 = manager.encrypt(plain);
        assertThat(e1).isNotEqualTo(e2);
    }
}

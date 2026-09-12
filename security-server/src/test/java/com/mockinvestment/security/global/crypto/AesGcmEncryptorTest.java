package com.mockinvestment.security.global.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmEncryptorTest {

    private static final String KEY_32 = Base64.getEncoder().encodeToString(
            "test-aes-256-gcm-key-32-bytes!!!".getBytes());

    private final AesGcmEncryptor encryptor = new AesGcmEncryptor(KEY_32);

    @Test
    void 암호화_후_복호화하면_원문이_나온다() {
        String plain = "777123456781";

        String encrypted = encryptor.encrypt(plain);

        assertThat(encrypted).isNotEqualTo(plain);
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(plain);
    }

    @Test
    void 같은_평문도_암호화할_때마다_다른_암호문이_나온다_랜덤_IV() {
        String plain = "777123456781";

        String first = encryptor.encrypt(plain);
        String second = encryptor.encrypt(plain);

        assertThat(first).isNotEqualTo(second);
        assertThat(encryptor.decrypt(first)).isEqualTo(encryptor.decrypt(second));
    }

    @Test
    void 암호문이_변조되면_복호화가_실패한다_GCM_인증태그() {
        byte[] bytes = Base64.getDecoder().decode(encryptor.encrypt("777123456781"));
        bytes[bytes.length - 1] ^= 0x01;   // 마지막 바이트 1비트 뒤집기
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> encryptor.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("decryption failed");
    }

    @Test
    void 키가_32바이트가_아니면_생성_시점에_실패한다() {
        String key16 = Base64.getEncoder().encodeToString("only-16-bytes!!!".getBytes());

        assertThatThrownBy(() -> new AesGcmEncryptor(key16))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}

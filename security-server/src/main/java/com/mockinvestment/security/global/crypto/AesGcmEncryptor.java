package com.mockinvestment.security.global.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 암호화 (계좌번호 등 복호화가 필요한 민감 정보용).
 *
 * GCM 을 쓰는 이유: CBC 는 기밀성만 보장한다. 암호문이 한 바이트 바뀌어도 복호화는 "성공"하고 쓰레기 값이 나온다.
 * GCM 은 AEAD 라서 인증 태그가 붙고, 변조되면 복호화 단계에서 예외가 난다.
 *
 * IV(nonce) 는 암호화할 때마다 SecureRandom 으로 새로 만든다. 같은 키 + 같은 IV 재사용은 GCM 의 보안을 깨뜨린다.
 * 그래서 설정에 고정 IV 가 없고, IV 는 암호문 앞에 붙여 함께 저장한다.
 *
 * 저장 형식: Base64( IV(12바이트) || 암호문 || 인증태그(16바이트) )
 */
@Component
public class AesGcmEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_BYTES = 32;      // AES-256
    private static final int IV_BYTES = 12;       // GCM 권장 nonce 길이
    private static final int TAG_BITS = 128;      // 인증 태그 길이

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmEncryptor(@Value("${aes.key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != KEY_BYTES) {
            // 키 길이가 틀리면 부팅 시점에 바로 실패시킨다. 첫 암호화 요청에서 터지는 것보다 낫다
            throw new IllegalStateException(
                    "aes.key must be exactly " + KEY_BYTES + " bytes (Base64 of 32 random bytes), but was " + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)); // 태그 포함

            byte[] out = new byte[IV_BYTES + cipherText.length];
            System.arraycopy(iv, 0, out, 0, IV_BYTES);
            System.arraycopy(cipherText, 0, out, IV_BYTES, cipherText.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM encryption failed", e);
        }
    }

    public String decrypt(String base64CipherText) {
        try {
            byte[] in = Base64.getDecoder().decode(base64CipherText);
            byte[] iv = Arrays.copyOfRange(in, 0, IV_BYTES);
            byte[] cipherText = Arrays.copyOfRange(in, IV_BYTES, in.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // AEADBadTagException(변조) 포함. 호출자에게 "복호화 실패"만 알리고 원인은 로그로
            throw new IllegalStateException("AES-GCM decryption failed (tampered or wrong key?)", e);
        }
    }
}

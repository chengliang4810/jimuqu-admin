package com.jimuqu.common.core.encrypt.utils;

import cn.hutool.v7.core.text.StrUtil;
import com.jimuqu.common.core.encrypt.domain.RsaKeyPair;
import com.jimuqu.common.core.exception.ServiceException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Bell 6.X 接口 RSA + AES 加解密工具。 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiCryptoUtil {

    private static final String RSA = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final String AES = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String KEY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String decryptRequest(String body, String encryptedKey, String privateKey) {
        if (StrUtil.hasBlank(body, encryptedKey, privateKey)) {
            throw new ServiceException("接口加密参数不完整");
        }
        try {
            String encodedAesKey = new String(rsa(Cipher.DECRYPT_MODE,
                    Base64.getDecoder().decode(encryptedKey), parsePrivateKey(privateKey)), StandardCharsets.UTF_8);
            String aesKey = new String(Base64.getDecoder().decode(encodedAesKey), StandardCharsets.UTF_8);
            return decryptByAes(body, aesKey);
        } catch (Exception e) {
            throw new ServiceException("接口请求解密失败: " + e.getMessage());
        }
    }

    public static String encryptByAes(String content, String aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey(aesKey));
        return Base64.getEncoder().encodeToString(cipher.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    public static String decryptByAes(String content, String aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, aesKey(aesKey));
        return new String(cipher.doFinal(Base64.getDecoder().decode(content)), StandardCharsets.UTF_8);
    }

    public static String encryptByRsa(String content, String publicKey) throws Exception {
        return Base64.getEncoder().encodeToString(rsa(Cipher.ENCRYPT_MODE,
                content.getBytes(StandardCharsets.UTF_8), parsePublicKey(publicKey)));
    }

    public static String randomAesKey() {
        StringBuilder key = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            key.append(KEY_CHARS.charAt(RANDOM.nextInt(KEY_CHARS.length())));
        }
        return key.toString();
    }

    public static RsaKeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
            generator.initialize(1024);
            java.security.KeyPair keyPair = generator.generateKeyPair();
            return new RsaKeyPair(
                    Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        } catch (Exception e) {
            throw new ServiceException("生成RSA密钥对失败: " + e.getMessage());
        }
    }

    public static void validateRsaKeyPair(String publicKey, String privateKey) {
        try {
            parsePublicKey(publicKey);
            parsePrivateKey(privateKey);
        } catch (Exception e) {
            throw new ServiceException("RSA秘钥配置错误: " + e.getMessage());
        }
    }

    private static SecretKeySpec aesKey(String key) {
        int length = key == null ? 0 : key.getBytes(StandardCharsets.UTF_8).length;
        if (length != 16 && length != 24 && length != 32) {
            throw new ServiceException("AES秘钥长度要求为16位、24位、32位");
        }
        return new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES);
    }

    private static byte[] rsa(int mode, byte[] data, java.security.Key key) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(mode, key);
        return cipher.doFinal(data);
    }

    private static PublicKey parsePublicKey(String publicKey) throws Exception {
        return KeyFactory.getInstance(RSA).generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(cleanKey(publicKey))));
    }

    private static PrivateKey parsePrivateKey(String privateKey) throws Exception {
        return KeyFactory.getInstance(RSA).generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(cleanKey(privateKey))));
    }

    private static String cleanKey(String key) {
        if (StrUtil.isBlank(key)) {
            throw new ServiceException("RSA秘钥未配置");
        }
        return key.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
    }
}

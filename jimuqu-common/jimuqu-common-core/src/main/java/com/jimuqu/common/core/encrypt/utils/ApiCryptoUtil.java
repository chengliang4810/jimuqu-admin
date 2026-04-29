package com.jimuqu.common.core.encrypt.utils;

import cn.hutool.v7.core.text.StrUtil;
import com.jimuqu.common.core.encrypt.domain.ApiEncryptPayload;
import com.jimuqu.common.core.encrypt.domain.RsaKeyPair;
import com.jimuqu.common.core.exception.ServiceException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
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

/**
 * 接口 RSA + AES 混合加解密工具。
 *
 * @author chengliang
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiCryptoUtil {

    private static final String RSA = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int AES_IV_SIZE = 12;
    private static final int AES_TAG_LENGTH = 128;

    public static RsaKeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
            generator.initialize(2048);
            java.security.KeyPair keyPair = generator.generateKeyPair();
            return new RsaKeyPair(
                    Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
            );
        } catch (Exception e) {
            throw new ServiceException("生成RSA密钥对失败: " + e.getMessage());
        }
    }

    public static ApiEncryptPayload encrypt(String json, String publicKey) {
        if (StrUtil.isBlank(json)) {
            return new ApiEncryptPayload();
        }
        try {
            SecretKey aesKey = generateAesKey();
            byte[] iv = randomIv();
            byte[] encryptedData = aesEncrypt(json.getBytes(StandardCharsets.UTF_8), aesKey.getEncoded(), iv);
            byte[] encryptedKey = rsaEncrypt(aesKey.getEncoded(), parsePublicKey(publicKey));
            return new ApiEncryptPayload()
                    .setEncryptKey(Base64.getEncoder().encodeToString(encryptedKey))
                    .setIv(Base64.getEncoder().encodeToString(iv))
                    .setData(Base64.getEncoder().encodeToString(encryptedData));
        } catch (Exception e) {
            throw new ServiceException("接口响应加密失败: " + e.getMessage());
        }
    }

    public static String decrypt(ApiEncryptPayload payload, String privateKey) {
        if (payload == null || StrUtil.hasBlank(payload.getEncryptKey(), payload.getIv(), payload.getData())) {
            throw new ServiceException("接口加密参数不完整");
        }
        try {
            byte[] aesKey = rsaDecrypt(Base64.getDecoder().decode(payload.getEncryptKey()), parsePrivateKey(privateKey));
            byte[] iv = Base64.getDecoder().decode(payload.getIv());
            byte[] data = Base64.getDecoder().decode(payload.getData());
            return new String(aesDecrypt(data, aesKey, iv), StandardCharsets.UTF_8);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("接口请求解密失败: " + e.getMessage());
        }
    }

    private static SecretKey generateAesKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance(AES);
        generator.init(AES_KEY_SIZE);
        return generator.generateKey();
    }

    private static byte[] randomIv() {
        byte[] iv = new byte[AES_IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private static byte[] aesEncrypt(byte[] data, byte[] aesKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, AES), new GCMParameterSpec(AES_TAG_LENGTH, iv));
        return cipher.doFinal(data);
    }

    private static byte[] aesDecrypt(byte[] data, byte[] aesKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, AES), new GCMParameterSpec(AES_TAG_LENGTH, iv));
        return cipher.doFinal(data);
    }

    private static byte[] rsaEncrypt(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    private static byte[] rsaDecrypt(byte[] data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    private static PublicKey parsePublicKey(String publicKey) throws Exception {
        if (StrUtil.isBlank(publicKey)) {
            throw new ServiceException("接口加密公钥未配置");
        }
        byte[] keyBytes = Base64.getDecoder().decode(cleanKey(publicKey));
        return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private static PrivateKey parsePrivateKey(String privateKey) throws Exception {
        if (StrUtil.isBlank(privateKey)) {
            throw new ServiceException("接口加密私钥未配置");
        }
        byte[] keyBytes = Base64.getDecoder().decode(cleanKey(privateKey));
        return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private static String cleanKey(String key) {
        return key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
    }
}

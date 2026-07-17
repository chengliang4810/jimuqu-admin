package com.jimuqu.common.core.encrypt;

import com.jimuqu.common.core.encrypt.domain.ApiEncryptPayload;
import com.jimuqu.common.core.encrypt.domain.RsaKeyPair;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiCryptoUtilTest {

    @Test
    void hybridEncryptAndDecryptRoundTripJson() {
        String json = "{\"mobile\":\"13812345678\",\"name\":\"张三\"}";
        RsaKeyPair keyPair = ApiCryptoUtil.generateRsaKeyPair();

        ApiEncryptPayload encrypted = ApiCryptoUtil.encrypt(json, keyPair.getPublicKey());
        String decrypted = ApiCryptoUtil.decrypt(encrypted, keyPair.getPrivateKey());

        assertEquals(json, decrypted);
    }

    @Test
    void decryptFrontendRsaPkcs1AndAesEcbRequest() throws Exception {
        String json = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        String aesKey = "01234567890123456789012345678901";
        RsaKeyPair keyPair = ApiCryptoUtil.generateRsaKeyPair();
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, java.security.KeyFactory.getInstance("RSA")
                .generatePublic(new java.security.spec.X509EncodedKeySpec(
                        Base64.getDecoder().decode(keyPair.getPublicKey()))));
        String encryptedKey = Base64.getEncoder().encodeToString(rsa.doFinal(
                Base64.getEncoder().encode(aesKey.getBytes(StandardCharsets.UTF_8))));
        Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES"));
        String body = Base64.getEncoder().encodeToString(aes.doFinal(json.getBytes(StandardCharsets.UTF_8)));

        assertEquals(json, ApiCryptoUtil.decryptFrontend(body, encryptedKey, keyPair.getPrivateKey()));
    }
}

package com.jimuqu.common.core.encrypt;

import com.jimuqu.common.core.encrypt.domain.RsaKeyPair;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiCryptoUtilTest {

    @Test
    void rejectsInvalidRsaConfigurationAtInitialization() {
        assertThrows(RuntimeException.class,
                () -> ApiCryptoUtil.validateRsaKeyPair("broken", "broken"));
    }

    @Test
    void decryptBellRsaPkcs1AndAesEcbRequest() throws Exception {
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

        assertEquals(json, ApiCryptoUtil.decryptRequest(body, encryptedKey, keyPair.getPrivateKey()));
    }

    @Test
    void encryptBellResponseWithRsaHeaderAndAesBody() throws Exception {
        String json = "{\"code\":200,\"msg\":\"成功\",\"data\":{}}";
        String aesKey = ApiCryptoUtil.randomAesKey();
        RsaKeyPair keyPair = ApiCryptoUtil.generateRsaKeyPair();

        String encryptedKey = ApiCryptoUtil.encryptByRsa(
                Base64.getEncoder().encodeToString(aesKey.getBytes(StandardCharsets.UTF_8)),
                keyPair.getPublicKey());
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.DECRYPT_MODE, java.security.KeyFactory.getInstance("RSA")
                .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(
                        Base64.getDecoder().decode(keyPair.getPrivateKey()))));
        String decryptedKey = new String(Base64.getDecoder().decode(rsa.doFinal(
                Base64.getDecoder().decode(encryptedKey))), StandardCharsets.UTF_8);
        String encryptedBody = ApiCryptoUtil.encryptByAes(json, aesKey);

        assertEquals(aesKey, decryptedKey);
        assertEquals(json, ApiCryptoUtil.decryptByAes(encryptedBody, decryptedKey));
    }
}

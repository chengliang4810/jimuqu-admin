package com.jimuqu.common.core.encrypt;

import com.jimuqu.common.core.encrypt.domain.ApiEncryptPayload;
import com.jimuqu.common.core.encrypt.domain.RsaKeyPair;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import org.junit.jupiter.api.Test;

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
}

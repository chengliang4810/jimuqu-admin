package com.jimuqu.common.core.encrypt.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Base64 编码的 RSA 密钥对。
 *
 * @author chengliang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RsaKeyPair implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String publicKey;

    private String privateKey;
}

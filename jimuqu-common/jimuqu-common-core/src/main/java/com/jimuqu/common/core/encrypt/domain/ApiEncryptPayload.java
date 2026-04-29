package com.jimuqu.common.core.encrypt.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * RSA + AES 混合加密载荷。
 *
 * @author chengliang
 */
@Data
@Accessors(chain = true)
public class ApiEncryptPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * RSA 加密后的 AES 密钥。
     */
    private String encryptKey;

    /**
     * AES/CBC 初始化向量。
     */
    private String iv;

    /**
     * AES 加密后的 JSON 数据。
     */
    private String data;
}

package com.jimuqu.auth.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.noear.snack4.annotation.ONodeAttr;

/**
 * 登录验证信息
 *
 * @author Michelle.Chung
 */
@Data
@Accessors(chain = true)
public class LoginVo {

    /**
     * 授权令牌
     */
    @JsonProperty("access_token")
    @ONodeAttr(name = "access_token")
    private String accessToken;

    /**
     * 刷新令牌
     */
    @JsonProperty("refresh_token")
    @ONodeAttr(name = "refresh_token")
    private String refreshToken;

    /**
     * 授权令牌 access_token 的有效期
     */
    @JsonProperty("expire_in")
    @ONodeAttr(name = "expire_in")
    private Long expireIn;

    /**
     * 刷新令牌 refresh_token 的有效期
     */
    @JsonProperty("refresh_expire_in")
    @ONodeAttr(name = "refresh_expire_in")
    private Long refreshExpireIn;

    /**
     * 应用id
     */
    @JsonProperty("client_id")
    @ONodeAttr(name = "client_id")
    private String clientId;

    /**
     * 令牌权限
     */
    private String scope;

    /**
     * 用户 openid
     */
    private String openid;

}

package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysSocial;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 社会化账号绑定视图。
 */
@Data
@ResultEntity(SysSocial.class)
@AutoMapper(target = SysSocial.class)
public class SysSocialVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String authId;
    private String source;
    private String accessToken;
    private Integer expireIn;
    private String refreshToken;
    private String openId;
    private String userName;
    private String nickName;
    private String email;
    private String avatar;
    private String accessCode;
    private String unionId;
    private String scope;
    private String tokenType;
    private String idToken;
    private String macAlgorithm;
    private String macKey;
    private String code;
    private String oauthToken;
    private String oauthTokenSecret;
    private Date createTime;
}

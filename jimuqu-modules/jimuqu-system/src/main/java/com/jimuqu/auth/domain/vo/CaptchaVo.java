package com.jimuqu.auth.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 验证码信息
 *
 * @author Michelle.Chung
 */
@Data
@Accessors(chain = true)
public class CaptchaVo {

    /**
     * 是否开启验证码
     */
    private Boolean captchaEnabled = true;

    private String uuid;

    /**
     * 验证码图片
     */
    private String img;

}

package com.jimuqu.common.web.config.properties;

import lombok.Data;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * 验证码 配置属性
 *
 * @author Lion Li,chengliang4810
 */
@Data
@Configuration
@Inject(value = "${captcha}", required = false)
public class CaptchaProperties {

    private Boolean enable = true;

    /**
     * 验证码类型：math 数学计算，char 字符验证
     */
    private String type = "math";

    /**
     * 数字验证码位数
     */
    private Integer numberLength = 1;

    /**
     * 字符验证码长度
     */
    private Integer charLength = 4;
}

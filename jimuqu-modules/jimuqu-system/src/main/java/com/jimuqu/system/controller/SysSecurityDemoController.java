package com.jimuqu.system.controller;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.common.core.sensitive.annotation.Sensitive;
import com.jimuqu.common.core.sensitive.enums.SensitiveType;
import com.jimuqu.common.web.encrypt.ApiEncryptSupport;
import lombok.Data;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;

import java.io.Serial;
import java.io.Serializable;

/**
 * 安全能力自检接口。
 *
 * @author jimuqu-admin
 * @since 2026-06-13
 */
@Controller
@Mapping("/system/security/demo")
public class SysSecurityDemoController {

    /**
     * 获取接口加密公钥。
     */
    @Get
    @Mapping("/public-key")
    public R<String> publicKey() {
        return R.ok(ApiEncryptSupport.currentKeyPair().getPublicKey());
    }

    /**
     * 脱敏响应示例。
     */
    @Get
    @Mapping("/sensitive")
    public R<SecurityDemoVo> sensitive() {
        return R.ok(buildDemoVo());
    }

    /**
     * 响应加密示例。
     */
    @Get
    @ApiEncrypt(request = false)
    @Mapping("/encrypt")
    public R<SecurityDemoVo> encrypt() {
        return R.ok(buildDemoVo());
    }

    private SecurityDemoVo buildDemoVo() {
        SecurityDemoVo vo = new SecurityDemoVo();
        vo.setName("张三");
        vo.setMobile("15888888888");
        vo.setEmail("chengliang4810@163.com");
        vo.setIdCard("430101199001011234");
        vo.setToken("abcdefyz");
        return vo;
    }

    /**
     * 安全能力自检视图对象。
     */
    @Data
    public static class SecurityDemoVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Sensitive(type = SensitiveType.NAME)
        private String name;

        @Sensitive(type = SensitiveType.MOBILE)
        private String mobile;

        @Sensitive(type = SensitiveType.EMAIL)
        private String email;

        @Sensitive(type = SensitiveType.ID_CARD)
        private String idCard;

        @Sensitive(prefixKeep = 2, suffixKeep = 2, mask = "***")
        private String token;
    }
}

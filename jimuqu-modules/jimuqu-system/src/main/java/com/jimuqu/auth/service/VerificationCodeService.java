package com.jimuqu.auth.service;

import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.mail.utils.MailUtils;
import lombok.RequiredArgsConstructor;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.cache.CacheService;

import java.security.SecureRandom;

/**
 * 登录验证码签发服务。
 *
 * <p>外部短信或邮件服务未配置时明确失败。测试环境可通过
 * {@code auth.verification.local-code} 配置本地验证码，不会隐式绕过校验。</p>
 */
@Component
@RequiredArgsConstructor
public class VerificationCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 4;

    private final CacheService cacheService;

    public void sendSms(String phoneNumber) {
        String code = resolveCode();
        if (StringUtil.isBlank(localCode())) {
            throw new ServiceException("短信验证码发送器未配置");
        }
        cache(phoneNumber, code);
    }

    public void sendEmail(String email) {
        String code = resolveCode();
        if (StringUtil.isNotBlank(localCode())) {
            cache(email, code);
            return;
        }
        if (!Solon.cfg().getBool("mail.enabled", false)) {
            throw new ServiceException("邮箱服务未配置");
        }
        MailUtils.sendText(
                email,
                "登录验证码",
                "您本次验证码为：" + code + "，有效期为" + Constants.CAPTCHA_EXPIRATION + "分钟。"
        );
        cache(email, code);
    }

    private String resolveCode() {
        String localCode = localCode();
        if (StringUtil.isNotBlank(localCode)) {
            return localCode;
        }
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    private String localCode() {
        return Solon.cfg().get("auth.verification.local-code", "");
    }

    private void cache(String target, String code) {
        cacheService.store(
                GlobalConstants.CAPTCHA_CODE_KEY + target,
                code,
                Constants.CAPTCHA_EXPIRATION * 60
        );
    }
}

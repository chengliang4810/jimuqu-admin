package com.jimuqu.auth.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.RandomUtil;
import com.jimuqu.auth.domain.vo.CaptchaVo;
import com.jimuqu.auth.service.VerificationCodeService;
import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.utils.MessageUtils;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.core.utils.regex.RegexValidator;
import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import com.jimuqu.common.ratelimit.core.RateLimiter;
import com.jimuqu.common.ratelimit.enums.RateLimitAlgorithm;
import com.jimuqu.common.ratelimit.exception.RateLimitException;
import com.jimuqu.common.web.config.properties.CaptchaProperties;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.common.web.core.WaveAndCircleCaptcha;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.Solon;
import org.noear.solon.core.handle.Context;
import org.noear.solon.data.cache.CacheService;

import java.awt.Font;
import java.util.UUID;

/**
 * 登录验证码接口。
 */
@SaIgnore
@Controller
@RequiredArgsConstructor
public class CaptchaController extends BaseController {

    private final CaptchaProperties captchaProperties;
    private final CacheService cacheService;
    private final VerificationCodeService verificationCodeService;
    private final RateLimiter rateLimiter;
    private final RateLimitConfig globalRateLimitConfig;

    @Get
    @Mapping("/resource/sms/code")
    public R<Void> smsCode(String phoneNumber) {
        if (StringUtil.isBlank(phoneNumber)) {
            return R.fail(MessageUtils.message("user.phonenumber.not.blank"));
        }
        if (!RegexValidator.isMobile(phoneNumber)) {
            return R.fail("请输入正确的手机号！");
        }
        checkRate("captcha:sms:" + phoneNumber, 1);
        verificationCodeService.sendSms(phoneNumber);
        return R.ok();
    }

    @Get
    @Mapping("/resource/email/code")
    public R<Void> emailCode(String email) {
        if (StringUtil.isBlank(email)) {
            return R.fail(MessageUtils.message("user.email.not.blank"));
        }
        if (!emailEnabled()) {
            return R.fail("当前系统没有开启邮箱功能！");
        }
        if (!RegexValidator.isEmail(email)) {
            return R.fail("请输入正确的邮箱地址！");
        }
        checkRate("captcha:email:" + email, 1);
        verificationCodeService.sendEmail(email);
        return R.ok();
    }

    @Get
    @Mapping("/auth/code")
    public R<CaptchaVo> getCode() {
        if (!Boolean.TRUE.equals(captchaProperties.getEnable())) {
            return R.ok(new CaptchaVo().setCaptchaEnabled(false));
        }
        checkRate("captcha:image:" + Context.current().realIp(), 10);

        String uuid = UUID.randomUUID().toString().replace("-", "");
        CaptchaImage captchaImage = createCaptchaImage();
        cacheService.store(
                GlobalConstants.CAPTCHA_CODE_KEY + uuid,
                captchaImage.challenge().answer(),
                Constants.CAPTCHA_EXPIRATION * 60
        );
        CaptchaVo captcha = new CaptchaVo()
                .setCaptchaEnabled(true)
                .setUuid(uuid)
                .setImg(captchaImage.imageBase64());
        return R.ok(captcha);
    }

    private boolean emailEnabled() {
        return StringUtil.isNotBlank(Solon.cfg().get("auth.verification.local-code", ""))
                || Solon.cfg().getBool("mail.enabled", false);
    }

    void checkRate(String key, int count) {
        RateLimitConfig config = new RateLimitConfig();
        config.setEnabled(true);
        config.setAlgorithm(RateLimitAlgorithm.FIXED_WINDOW);
        config.setWindow(60);
        config.setMaxBurst(count);
        config.setKeyPrefix(globalRateLimitConfig.getKeyPrefix());
        if (!rateLimiter.tryAcquire(key, 1, config)) {
            throw new RateLimitException(globalRateLimitConfig.getErrorMessage());
        }
    }

    CaptchaChallenge createChallenge() {
        return createChallenge(codeGenerator().generate());
    }

    CaptchaImage createCaptchaImage() {
        WaveAndCircleCaptcha captcha = new WaveAndCircleCaptcha(160, 60);
        captcha.setFont(new Font("Arial", Font.BOLD, 45));
        captcha.setGenerator(codeGenerator());
        captcha.createCode();
        return new CaptchaImage(createChallenge(captcha.getCode()), captcha.getImageBase64());
    }

    private CodeGenerator codeGenerator() {
        if ("math".equals(captchaProperties.getType())) {
            return new MathGenerator(captchaProperties.getNumberLength(), false);
        }
        return new RandomGenerator(RandomUtil.BASE_CHAR_NUMBER_LOWER, captchaProperties.getCharLength());
    }

    private CaptchaChallenge createChallenge(String code) {
        if (!"math".equals(captchaProperties.getType())) {
            return new CaptchaChallenge(code, code);
        }
        int numberLength = captchaProperties.getNumberLength();
        int left = Integer.parseInt(code.substring(0, numberLength).trim());
        int right = Integer.parseInt(code.substring(numberLength + 1, numberLength * 2 + 1).trim());
        int answer = switch (code.charAt(numberLength)) {
            case '+' -> left + right;
            case '-' -> left - right;
            default -> left * right;
        };
        return new CaptchaChallenge(code, String.valueOf(answer));
    }

    record CaptchaChallenge(String display, String answer) {
    }

    record CaptchaImage(CaptchaChallenge challenge, String imageBase64) {
    }
}

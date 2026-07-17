package com.jimuqu.auth.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.jimuqu.auth.domain.vo.CaptchaVo;
import com.jimuqu.auth.service.VerificationCodeService;
import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.web.config.properties.CaptchaProperties;
import com.jimuqu.common.web.core.BaseController;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.data.cache.CacheService;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 登录验证码接口。
 */
@SaIgnore
@Controller
@RequiredArgsConstructor
public class CaptchaController extends BaseController {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CaptchaProperties captchaProperties;
    private final CacheService cacheService;
    private final VerificationCodeService verificationCodeService;

    @Get
    @Mapping("/resource/sms/code")
    public R<Void> smsCode(String phoneNumber) {
        if (StringUtil.isBlank(phoneNumber) || !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            return R.fail("请输入正确的手机号");
        }
        verificationCodeService.sendSms(phoneNumber);
        return R.ok();
    }

    @Get
    @Mapping("/resource/email/code")
    public R<Void> emailCode(String email) {
        if (StringUtil.isBlank(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            return R.fail("请输入正确的邮箱地址");
        }
        verificationCodeService.sendEmail(email);
        return R.ok();
    }

    @Get
    @Mapping("/auth/code")
    public R<CaptchaVo> getCode() throws IOException {
        if (!Boolean.TRUE.equals(captchaProperties.getEnable())) {
            return R.ok(new CaptchaVo().setCaptchaEnabled(false));
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        CaptchaChallenge challenge = createChallenge();
        cacheService.store(
                GlobalConstants.CAPTCHA_CODE_KEY + uuid,
                challenge.answer(),
                Constants.CAPTCHA_EXPIRATION * 60
        );
        CaptchaVo captcha = new CaptchaVo()
                .setCaptchaEnabled(true)
                .setUuid(uuid)
                .setImg(renderPng(challenge.display()));
        return R.ok(captcha);
    }

    CaptchaChallenge createChallenge() {
        if ("math".equals(captchaProperties.getType())) {
            int limit = (int) Math.pow(10, captchaProperties.getNumberLength());
            int left = RANDOM.nextInt(limit);
            int right = RANDOM.nextInt(limit);
            char operator = "+-*".charAt(RANDOM.nextInt(3));
            int answer = switch (operator) {
                case '+' -> left + right;
                case '-' -> left - right;
                default -> left * right;
            };
            return new CaptchaChallenge(left + String.valueOf(operator) + right + "=", String.valueOf(answer));
        }
        String code = randomCode(captchaProperties.getCharLength());
        return new CaptchaChallenge(code, code);
    }

    private String randomCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(CAPTCHA_CHARS.charAt(RANDOM.nextInt(CAPTCHA_CHARS.length())));
        }
        return code.toString();
    }

    private String renderPng(String code) throws IOException {
        BufferedImage image = new BufferedImage(160, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
            int step = image.getWidth() / (code.length() + 1);
            for (int i = 0; i < code.length(); i++) {
                graphics.setColor(new Color(30 + RANDOM.nextInt(120), 30 + RANDOM.nextInt(120), 30 + RANDOM.nextInt(120)));
                graphics.drawString(String.valueOf(code.charAt(i)), step * i + 12, 42 + RANDOM.nextInt(6));
            }
            for (int i = 0; i < 8; i++) {
                graphics.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
                graphics.drawLine(
                        RANDOM.nextInt(image.getWidth()),
                        RANDOM.nextInt(image.getHeight()),
                        RANDOM.nextInt(image.getWidth()),
                        RANDOM.nextInt(image.getHeight())
                );
            }
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        }
    }

    record CaptchaChallenge(String display, String answer) {
    }
}

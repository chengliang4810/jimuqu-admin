package com.jimuqu.auth.controller;

import com.jimuqu.auth.service.VerificationCodeService;
import com.jimuqu.common.web.config.properties.CaptchaProperties;
import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import com.jimuqu.common.ratelimit.core.RateLimiter;
import com.jimuqu.common.ratelimit.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptchaControllerTest {

    @Test
    void createsMathAndCharChallengesFromConfiguration() {
        CaptchaProperties properties = new CaptchaProperties();
        CaptchaController controller = new CaptchaController(properties, null, null, null, null);

        CaptchaController.CaptchaChallenge math = controller.createChallenge();
        assertTrue(math.display().matches("\\d[+\\-*]\\d="));
        assertEquals(evaluate(math.display()), Integer.parseInt(math.answer()));

        properties.setType("char");
        properties.setCharLength(6);
        CaptchaController.CaptchaChallenge chars = controller.createChallenge();
        assertEquals(6, chars.display().length());
        assertEquals(chars.display(), chars.answer());
    }

    @Test
    void mathChallengeMatchesUpstreamFixedWidthAndNeverProducesNegativeAnswers() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.setNumberLength(3);
        CaptchaController controller = new CaptchaController(properties, null, null, null, null);

        for (int i = 0; i < 1_000; i++) {
            CaptchaController.CaptchaChallenge challenge = controller.createChallenge();
            assertEquals(8, challenge.display().length());
            assertTrue(Integer.parseInt(challenge.answer()) >= 0);
            assertEquals(evaluateFixedWidth(challenge.display()), Integer.parseInt(challenge.answer()));
            if (challenge.display().charAt(3) == '-') {
                int left = Integer.parseInt(challenge.display().substring(0, 3).trim());
                int right = Integer.parseInt(challenge.display().substring(4, 7).trim());
                assertTrue(left == 0 ? right == 0 : right < left);
            }
        }
    }

    @Test
    void charChallengeUsesTheUpstreamLowercaseAndDigitAlphabet() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.setType("char");
        properties.setCharLength(32);
        CaptchaController controller = new CaptchaController(properties, null, null, null, null);
        StringBuilder generated = new StringBuilder();

        for (int i = 0; i < 100; i++) {
            generated.append(controller.createChallenge().display());
        }

        assertTrue(generated.toString().matches("[a-z0-9]+"));
        assertTrue(generated.toString().matches(".*[a-z].*"));
    }

    @Test
    void rendersTheUpstreamWaveAndCirclePng() throws Exception {
        CaptchaController controller = new CaptchaController(
                new CaptchaProperties(), null, null, null, null);

        CaptchaController.CaptchaImage rendered = controller.createCaptchaImage();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(
                Base64.getDecoder().decode(rendered.imageBase64())));

        assertNotNull(image);
        assertEquals(160, image.getWidth());
        assertEquals(60, image.getHeight());
        HashSet<Integer> colors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                colors.add(image.getRGB(x, y));
            }
        }
        assertTrue(colors.size() > 20, "验证码必须包含彩色文字和干扰元素");
        assertEquals(evaluateFixedWidth(rendered.challenge().display()),
                Integer.parseInt(rendered.challenge().answer()));
    }

    @Test
    void rateLimitsSmsByPhoneNumber() {
        AtomicReference<String> key = new AtomicReference<>();
        RateLimiter limiter = (RateLimiter) Proxy.newProxyInstance(
                RateLimiter.class.getClassLoader(), new Class<?>[]{RateLimiter.class},
                (proxy, method, args) -> {
                    if ("tryAcquire".equals(method.getName())) {
                        key.set((String) args[0]);
                        return false;
                    }
                    return null;
                });
        RateLimitConfig config = new RateLimitConfig();
        CaptchaController controller = new CaptchaController(new CaptchaProperties(), null, null, limiter, config);

        assertThrows(RateLimitException.class, () -> controller.smsCode("13800138000"));
        assertEquals("captcha:sms:13800138000", key.get());
    }

    @Test
    void usesUpstreamLimitsForAllVerificationTargets() {
        List<String> limits = new ArrayList<>();
        RateLimiter limiter = (RateLimiter) Proxy.newProxyInstance(
                RateLimiter.class.getClassLoader(), new Class<?>[]{RateLimiter.class},
                (proxy, method, args) -> {
                    if ("tryAcquire".equals(method.getName())) {
                        RateLimitConfig applied = (RateLimitConfig) args[2];
                        limits.add(args[0] + ":" + applied.getWindow() + ":" + applied.getMaxBurst());
                        return true;
                    }
                    return null;
                });
        CaptchaController controller = new CaptchaController(
                new CaptchaProperties(), null, null, limiter, new RateLimitConfig());

        controller.checkRate("captcha:sms:13800138000", 1);
        controller.checkRate("captcha:email:user@example.com", 1);
        controller.checkRate("captcha:image:127.0.0.1", 10);

        assertEquals(List.of(
                "captcha:sms:13800138000:60:1",
                "captcha:email:user@example.com:60:1",
                "captcha:image:127.0.0.1:60:10"
        ), limits);
    }

    @Test
    void disabledCaptchaReturnsBellDtoWithoutRateLimiting() throws Exception {
        CaptchaProperties properties = new CaptchaProperties();
        properties.setEnable(false);
        CaptchaController controller = new CaptchaController(properties, null, null, null, new RateLimitConfig());

        var response = controller.getCode();

        assertFalse(response.getData().getCaptchaEnabled());
        assertNull(response.getData().getUuid());
        assertNull(response.getData().getImg());
    }

    @Test
    void distinguishesBlankTargetsFromInvalidFormats() {
        RateLimitConfig disabled = new RateLimitConfig();
        disabled.setEnabled(false);
        CaptchaController controller = new CaptchaController(
                new CaptchaProperties(), null, null, null, disabled);

        assertEquals("用户手机号不能为空", controller.smsCode("").getMsg());
        assertEquals("邮箱不能为空", controller.emailCode("").getMsg());
    }

    @Test
    void invalidSmsTargetsDoNotConsumeRateLimitQuota() {
        AtomicInteger acquired = new AtomicInteger();
        RateLimiter limiter = (RateLimiter) Proxy.newProxyInstance(
                RateLimiter.class.getClassLoader(), new Class<?>[]{RateLimiter.class},
                (proxy, method, args) -> {
                    if ("tryAcquire".equals(method.getName())) {
                        acquired.incrementAndGet();
                        return true;
                    }
                    return null;
                });
        CaptchaController controller = new CaptchaController(
                new CaptchaProperties(), null, null, limiter, new RateLimitConfig());

        assertEquals("用户手机号不能为空", controller.smsCode("").getMsg());
        assertEquals("请输入正确的手机号！", controller.smsCode("not-a-mobile").getMsg());
        assertEquals(0, acquired.get());
    }

    @Test
    void acceptsTheSamePrefixedMobileFormatAsUpstreamValidator() {
        AtomicReference<String> sentTo = new AtomicReference<>();
        VerificationCodeService verificationCodes = new VerificationCodeService(null) {
            @Override
            public void sendSms(String phoneNumber) {
                sentTo.set(phoneNumber);
            }
        };
        RateLimitConfig disabled = new RateLimitConfig();
        disabled.setEnabled(false);
        CaptchaController controller = new CaptchaController(
                new CaptchaProperties(), null, verificationCodes, null, disabled);

        assertEquals(200, controller.smsCode("+8613800138000").getCode());
        assertEquals("+8613800138000", sentTo.get());
    }

    private int evaluate(String expression) {
        int left = expression.charAt(0) - '0';
        int right = expression.charAt(2) - '0';
        return switch (expression.charAt(1)) {
            case '+' -> left + right;
            case '-' -> left - right;
            default -> left * right;
        };
    }

    private int evaluateFixedWidth(String expression) {
        int numberLength = (expression.length() - 2) / 2;
        int left = Integer.parseInt(expression.substring(0, numberLength).trim());
        int right = Integer.parseInt(expression.substring(numberLength + 1, numberLength * 2 + 1).trim());
        return switch (expression.charAt(numberLength)) {
            case '+' -> left + right;
            case '-' -> left - right;
            default -> left * right;
        };
    }
}

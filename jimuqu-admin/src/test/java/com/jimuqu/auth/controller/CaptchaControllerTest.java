package com.jimuqu.auth.controller;

import com.jimuqu.common.web.config.properties.CaptchaProperties;
import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import com.jimuqu.common.ratelimit.core.RateLimiter;
import com.jimuqu.common.ratelimit.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private int evaluate(String expression) {
        int left = expression.charAt(0) - '0';
        int right = expression.charAt(2) - '0';
        return switch (expression.charAt(1)) {
            case '+' -> left + right;
            case '-' -> left - right;
            default -> left * right;
        };
    }
}

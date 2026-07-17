package com.jimuqu.auth.controller;

import com.jimuqu.common.web.config.properties.CaptchaProperties;
import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import com.jimuqu.common.ratelimit.core.RateLimiter;
import com.jimuqu.common.ratelimit.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

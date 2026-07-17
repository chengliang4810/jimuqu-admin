package com.jimuqu.auth.controller;

import com.jimuqu.common.web.config.properties.CaptchaProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptchaControllerTest {

    @Test
    void createsMathAndCharChallengesFromConfiguration() {
        CaptchaProperties properties = new CaptchaProperties();
        CaptchaController controller = new CaptchaController(properties, null, null);

        CaptchaController.CaptchaChallenge math = controller.createChallenge();
        assertTrue(math.display().matches("\\d[+\\-*]\\d="));
        assertEquals(evaluate(math.display()), Integer.parseInt(math.answer()));

        properties.setType("char");
        properties.setCharLength(6);
        CaptchaController.CaptchaChallenge chars = controller.createChallenge();
        assertEquals(6, chars.display().length());
        assertEquals(chars.display(), chars.answer());
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

package com.jimuqu.common.web.validation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ValidationMessageBundleTest {

    private static final List<String> LOGIN_VALIDATION_KEYS = List.of(
            "user.username.not.blank",
            "user.username.length.valid",
            "user.password.not.blank",
            "user.password.length.valid",
            "user.email.not.blank",
            "user.email.not.valid",
            "user.phonenumber.not.blank",
            "sms.code.not.blank",
            "email.code.not.blank",
            "social.source.not.blank",
            "social.code.not.blank",
            "social.state.not.blank",
            "xcx.code.not.blank",
            "repeat.submit.message"
    );

    @Test
    void containsBellLoginValidationMessagesForChineseAndEnglish() {
        ResourceBundle chinese = ResourceBundle.getBundle("i18n.messages", Locale.SIMPLIFIED_CHINESE);
        ResourceBundle english = ResourceBundle.getBundle("i18n.messages", Locale.US);

        assertAll(LOGIN_VALIDATION_KEYS.stream()
                .flatMap(key -> List.of(chinese, english).stream()
                        .map(bundle -> () -> assertFalse(bundle.getString(key).isBlank(), key))));
    }
}

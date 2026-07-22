package com.jimuqu.common.mail.utils;

import cn.hutool.extra.mail.MailAccount;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailUtilsTest {

    @Test
    void splitsMixedAddressSeparatorsAndIgnoresWhitespace() {
        assertEquals(
                List.of("first@example.test", "second@example.test", "third@example.test"),
                MailUtils.splitAddress(" first@example.test; second@example.test, third@example.test "));
        assertEquals(List.of(), MailUtils.splitAddress(" "));
    }

    @Test
    void rejectsMissingMessageFieldsBeforeOpeningAnSmtpSession() {
        MailAccount account = new MailAccount();
        assertThrows(IllegalArgumentException.class,
                () -> MailUtils.send(account, List.of(), "标题", "正文", false));
        assertThrows(IllegalArgumentException.class,
                () -> MailUtils.send(account, List.of("to@example.test"), " ", "正文", false));
        assertThrows(IllegalArgumentException.class,
                () -> MailUtils.send(account, List.of("to@example.test"), "标题", null, false));
    }

    @Test
    void createsAnIndependentAccountForPerMessageCredentials() {
        MailAccount source = new MailAccount()
                .setHost("smtp.example.test")
                .setPort(465)
                .setFrom("default@example.test")
                .setUser("default-user")
                .setPass("default-pass")
                .setSslEnable(true);

        MailAccount copy = MailUtils.copyWithOverrides(
                source, "sender@example.test", "sender-user", "sender-pass");

        assertAll(
                () -> assertNotSame(source, copy),
                () -> assertEquals("smtp.example.test", copy.getHost()),
                () -> assertEquals(465, copy.getPort()),
                () -> assertEquals("sender@example.test", copy.getFrom()),
                () -> assertEquals("sender-user", copy.getUser()),
                () -> assertEquals("sender-pass", copy.getPass()),
                () -> assertEquals("default@example.test", source.getFrom()),
                () -> assertEquals("default-user", source.getUser()),
                () -> assertEquals("default-pass", source.getPass())
        );
    }
}

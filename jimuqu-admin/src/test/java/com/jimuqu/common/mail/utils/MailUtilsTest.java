package com.jimuqu.common.mail.utils;

import cn.hutool.v7.extra.mail.MailAccount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class MailUtilsTest {

    @Test
    void customCredentialsDoNotMutateGlobalAccount() {
        MailAccount global = new MailAccount()
                .setHost("smtp.example.test")
                .setFrom("global@example.test")
                .setUser("global-user")
                .setPass("global-pass".toCharArray());

        MailAccount custom = MailUtils.copyWithOverrides(
                global, "custom@example.test", "custom-user", "custom-pass");

        assertNotSame(global, custom);
        assertEquals("custom@example.test", custom.getFrom());
        assertEquals("custom-user", custom.getUser());
        assertArrayEquals("custom-pass".toCharArray(), custom.getPass());
        assertEquals("global@example.test", global.getFrom());
        assertEquals("global-user", global.getUser());
        assertArrayEquals("global-pass".toCharArray(), global.getPass());
    }

    @Test
    void blankOverridesKeepConfiguredCredentials() {
        MailAccount global = new MailAccount()
                .setFrom("global@example.test")
                .setUser("global-user")
                .setPass("global-pass".toCharArray());

        MailAccount copy = MailUtils.copyWithOverrides(global, " ", null, "");

        assertEquals(global.getFrom(), copy.getFrom());
        assertEquals(global.getUser(), copy.getUser());
        assertArrayEquals(global.getPass(), copy.getPass());
    }
}

package com.jimuqu.common.mail.utils;

import cn.hutool.extra.mail.MailAccount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class MailUtilsTest {

    @Test
    void customCredentialsDoNotMutateGlobalAccount() {
        MailAccount global = new MailAccount()
                .setHost("smtp.example.test")
                .setFrom("global@example.test")
                .setUser("global-user")
                .setPass("global-pass");

        MailAccount custom = MailUtils.copyWithOverrides(
                global, "custom@example.test", "custom-user", "custom-pass");

        assertNotSame(global, custom);
        assertEquals("custom@example.test", custom.getFrom());
        assertEquals("custom-user", custom.getUser());
        assertEquals("custom-pass", custom.getPass());
        assertEquals("global@example.test", global.getFrom());
        assertEquals("global-user", global.getUser());
        assertEquals("global-pass", global.getPass());
    }

    @Test
    void blankOverridesKeepConfiguredCredentials() {
        MailAccount global = new MailAccount()
                .setFrom("global@example.test")
                .setUser("global-user")
                .setPass("global-pass");

        MailAccount copy = MailUtils.copyWithOverrides(global, " ", null, "");

        assertEquals(global.getFrom(), copy.getFrom());
        assertEquals(global.getUser(), copy.getUser());
        assertEquals(global.getPass(), copy.getPass());
    }
}

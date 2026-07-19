package com.jimuqu.common.mail.config;

import com.jimuqu.common.mail.config.properties.MailProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MailConfigTest {

    @Test
    void createsAccountWhenOptionalValuesAreMissing() {
        MailProperties properties = new MailProperties();
        properties.setHost("smtp.example.test");
        properties.setAuth(false);

        var account = new MailConfig().mailAccount(properties);

        assertEquals("smtp.example.test", account.getHost());
        assertNull(account.getPass());
    }
}

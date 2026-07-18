package com.jimuqu.common.security.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientAccessRuleTest {

    @Test
    void matchesExactWildcardAndCidrRules() {
        assertTrue(SecurityConfig.matchesIp("127.0.0.1", "127.0.0.1"));
        assertTrue(SecurityConfig.matchesIp("192.168.*", "192.168.1.8"));
        assertTrue(SecurityConfig.matchesIp("10.0.0.0/8", "10.2.3.4"));
        assertFalse(SecurityConfig.matchesIp("10.0.0.0/24", "10.0.1.4"));
        assertFalse(SecurityConfig.matchesIp("invalid/24", "10.0.0.1"));
    }
}

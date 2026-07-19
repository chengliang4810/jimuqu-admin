package com.jimuqu.common.social.topiam;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthTopIamRequestTest {

    @Test
    void buildsEndpointsFromConfiguredServer() {
        var source = AuthTopIamRequest.source("https://id.example.test/");
        assertEquals("https://id.example.test/oauth2/auth", source.authorize());
        assertEquals("https://id.example.test/oauth2/token", source.accessToken());
        assertEquals("https://id.example.test/oauth2/userinfo", source.userInfo());
    }
}

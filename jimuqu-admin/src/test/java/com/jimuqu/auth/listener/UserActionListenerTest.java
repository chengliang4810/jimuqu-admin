package com.jimuqu.auth.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserActionListenerTest {

    @Test
    void exposesOnlyTheLastEightTokenCharactersInLogs() {
        assertEquals("***efghijkl", UserActionListener.maskToken("abcdefghijkl"));
        assertEquals("***short", UserActionListener.maskToken("short"));
        assertEquals("***", UserActionListener.maskToken(null));
    }
}

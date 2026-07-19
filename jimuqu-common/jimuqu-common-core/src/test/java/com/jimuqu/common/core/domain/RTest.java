package com.jimuqu.common.core.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RTest {

    @Test
    void supportsDataAliasAndNullSafeStatusChecks() {
        R<String> response = R.data("value");

        assertEquals("value", response.getData());
        assertTrue(R.isSuccess(response));
        assertFalse(R.isSuccess(null));
        assertTrue(R.isError(null));
    }
}

package com.jimuqu.test.coverage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeRouteCoverageTest {

    @Test
    void normalizesRouteKeys() {
        assertEquals(
                "GET /system/user/{id}",
                RuntimeRouteCoverage.RouteKey.of("get", "//system//user/{id}/").toString()
        );
    }
}

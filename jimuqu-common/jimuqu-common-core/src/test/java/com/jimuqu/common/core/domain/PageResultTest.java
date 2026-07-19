package com.jimuqu.common.core.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageResultTest {

    @Test
    void totalUsesLongLikeBellUpstreamContract() {
        PageResult<String> result = new PageResult<>(List.of(), 3_000_000_000L);

        assertEquals(3_000_000_000L, result.getTotal());
    }
}

package com.jimuqu.system.domain;

import com.jimuqu.system.domain.vo.SysSocialVo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SysSocialParityTest {

    @Test
    void expireInUsesUpstreamZeroDefault() throws Exception {
        assertEquals(int.class, SysSocial.class.getDeclaredField("expireIn").getType());
        assertEquals(int.class, SysSocialVo.class.getDeclaredField("expireIn").getType());
        assertEquals(0, new SysSocial().getExpireIn());
        assertEquals(0, new SysSocialVo().getExpireIn());
    }
}

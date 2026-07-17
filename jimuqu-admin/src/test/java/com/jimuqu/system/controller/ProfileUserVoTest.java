package com.jimuqu.system.controller;

import com.jimuqu.common.core.sensitive.annotation.Sensitive;
import com.jimuqu.system.domain.vo.ProfileUserVo;
import com.jimuqu.system.domain.vo.ProfileVo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProfileUserVoTest {

    @Test
    void ownContactDetailsAreNotDesensitized() throws NoSuchFieldException {
        assertEquals(ProfileUserVo.class, ProfileVo.class.getDeclaredField("user").getType());
        assertNull(ProfileUserVo.class.getDeclaredField("email").getAnnotation(Sensitive.class));
        assertNull(ProfileUserVo.class.getDeclaredField("phonenumber").getAnnotation(Sensitive.class));
    }
}

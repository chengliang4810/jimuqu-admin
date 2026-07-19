package com.jimuqu.system.service;

import com.jimuqu.system.domain.SysLoginInfo;
import com.jimuqu.system.mapper.SysLoginInfoMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SysLoginInfoServiceTest {

    @Test
    void mixedExistingAndMissingIdsDoNotDeleteAnyLoginLog() {
        AtomicBoolean deleteInvoked = new AtomicBoolean();
        SysLoginInfoMapper mapper = (SysLoginInfoMapper) Proxy.newProxyInstance(
                SysLoginInfoMapper.class.getClassLoader(), new Class<?>[]{SysLoginInfoMapper.class},
                (proxy, method, args) -> {
                    if ("getEntityType".equals(method.getName())) {
                        return SysLoginInfo.class;
                    }
                    if ("count".equals(method.getName())) {
                        return 1;
                    }
                    if ("deleteByIds".equals(method.getName())) {
                        deleteInvoked.set(true);
                        return 1;
                    }
                    return method.getReturnType().isPrimitive() ? 0 : null;
                });

        assertThrows(RuntimeException.class,
                () -> new SysLoginInfoService(mapper, null).delete(List.of(1L, 999L)));
        assertFalse(deleteInvoked.get(), "存在任一无效 ID 时不得删除有效登录日志");
    }
}

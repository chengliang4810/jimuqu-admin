package com.jimuqu.system.service;

import com.jimuqu.common.log.event.OperLogEvent;
import com.jimuqu.system.domain.SysOperLog;
import com.jimuqu.system.domain.query.SysOperLogQuery;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.jimuqu.system.mapper.SysOperLogMapper;

class SysOperLogServiceTest {

    @Test
    void mixedExistingAndMissingIdsDoNotDeleteAnyOperationLog() {
        AtomicBoolean deleteInvoked = new AtomicBoolean();
        SysOperLogMapper mapper = (SysOperLogMapper) Proxy.newProxyInstance(
                SysOperLogMapper.class.getClassLoader(), new Class<?>[]{SysOperLogMapper.class},
                (proxy, method, args) -> {
                    if ("getEntityType".equals(method.getName())) {
                        return SysOperLog.class;
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
                () -> new SysOperLogService(mapper).delete(List.of(1L, 999L)));
        assertFalse(deleteInvoked.get(), "存在任一无效 ID 时不得删除有效操作日志");
    }

    @Test
    void ignoresNonPositiveSingleBusinessTypeLikeRuoYiSix() {
        SysOperLogQuery query = new SysOperLogQuery();
        query.setBusinessType(0);

        query.beforeBuildCondition();

        assertNull(query.getBusinessType());
    }

    @Test
    void mapsRequestThreadSnapshotWithoutReadingLoginContext() {
        Date operTime = new Date(1_234_567L);
        OperLogEvent event = new OperLogEvent();
        event.setOperId(1L);
        event.setTitle("用户管理");
        event.setBusinessType(2);
        event.setMethod("com.jimuqu.system.controller.SysUserController.update()");
        event.setRequestMethod("PUT");
        event.setOperatorType(1);
        event.setOperName("admin");
        event.setUserId(12L);
        event.setDeptId(34L);
        event.setDeptName("研发部");
        event.setClientKey("web-client");
        event.setDeviceType("pc");
        event.setBrowser("Chrome");
        event.setOs("Windows");
        event.setOperUrl("/system/user");
        event.setOperIp("127.0.0.1");
        event.setOperParam("{\"userId\":12}");
        event.setJsonResult("{\"code\":200}");
        event.setStatus(0);
        event.setErrorMsg(null);
        event.setOperTime(operTime);
        event.setCostTime(25L);

        SysOperLog entity = SysOperLogService.toEntity(event);

        assertAll(
                () -> assertEquals(1L, entity.getOperId()),
                () -> assertEquals("用户管理", entity.getTitle()),
                () -> assertEquals(2, entity.getBusinessType()),
                () -> assertEquals("com.jimuqu.system.controller.SysUserController.update()", entity.getMethod()),
                () -> assertEquals("PUT", entity.getRequestMethod()),
                () -> assertEquals(1, entity.getOperatorType()),
                () -> assertEquals("admin", entity.getOperName()),
                () -> assertEquals(12L, entity.getUserId()),
                () -> assertEquals(34L, entity.getDeptId()),
                () -> assertEquals("研发部", entity.getDeptName()),
                () -> assertEquals("web-client", entity.getClientKey()),
                () -> assertEquals("pc", entity.getDeviceType()),
                () -> assertEquals("Chrome", entity.getBrowser()),
                () -> assertEquals("Windows", entity.getOs()),
                () -> assertEquals("/system/user", entity.getOperUrl()),
                () -> assertEquals("127.0.0.1", entity.getOperIp()),
                () -> assertEquals("{\"userId\":12}", entity.getOperParam()),
                () -> assertEquals("{\"code\":200}", entity.getJsonResult()),
                () -> assertEquals(0, entity.getStatus()),
                () -> assertSame(operTime, entity.getOperTime()),
                () -> assertEquals(25L, entity.getCostTime())
        );
    }
}

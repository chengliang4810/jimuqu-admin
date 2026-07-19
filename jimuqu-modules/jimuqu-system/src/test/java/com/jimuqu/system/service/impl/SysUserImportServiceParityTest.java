package com.jimuqu.system.service.impl;

import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.system.domain.vo.SysUserImportVo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class SysUserImportServiceParityTest {

    @Test
    void emptyImportMatchesUpstreamSuccessSummary() {
        assertEquals("恭喜您，数据已全部导入成功！共 0 条，数据如下：",
                service(mock(SysUserImportRowTransactionExecutor.class)).importUsers(List.of(), false));
    }

    @Test
    void aggregatesFailuresAndKeepsProcessingLaterRows() {
        SysUserImportRowTransactionExecutor executor = directExecutor();
        SysUserServiceImpl service = spy(service(executor));
        List<String> persistedUsers = new ArrayList<>();
        doAnswer(invocation -> {
            SysUserImportVo imported = invocation.getArgument(0);
            String userName = imported.getUserName();
            if ("existing".equals(userName)) {
                throw new ServiceException("账号已存在");
            }
            if ("missing-dept".equals(userName)) {
                throw new ServiceException("部门不存在");
            }
            persistedUsers.add(userName);
            return false;
        }).when(service).importUser(any(SysUserImportVo.class), eq(false), eq("encoded-password"));

        ServiceException error = assertThrows(ServiceException.class, () -> service.processImportedUsers(List.of(
                row("first-valid"), row("existing"), row("missing-dept"), row("last-valid")),
                false, "encoded-password"));

        assertEquals(List.of("first-valid", "last-valid"), persistedUsers);
        assertTrue(error.getMessage().contains("很抱歉，导入失败！共 2 条数据格式不正确"));
        assertTrue(error.getMessage().contains("1、账号 existing 导入失败：账号已存在"));
        assertTrue(error.getMessage().contains("2、账号 missing-dept 导入失败：部门不存在"));
    }

    @Test
    void updateSupportIsForwardedToEveryRow() {
        SysUserImportRowTransactionExecutor executor = directExecutor();
        SysUserServiceImpl service = spy(service(executor));
        List<Boolean> updateFlags = new ArrayList<>();
        doAnswer(invocation -> {
            updateFlags.add(invocation.getArgument(1));
            SysUserImportVo imported = invocation.getArgument(0);
            return "existing".equals(imported.getUserName());
        }).when(service).importUser(any(SysUserImportVo.class), anyBoolean(), eq("encoded-password"));

        String message = service.processImportedUsers(
                List.of(row("existing"), row("new-user")), true, "encoded-password");

        assertEquals(List.of(true, true), updateFlags);
        assertEquals("恭喜您，数据已全部导入成功！共 2 条，数据如下：<br/>"
                + "1、账号 existing 更新成功<br/>2、账号 new-user 导入成功", message);
    }

    private static SysUserImportRowTransactionExecutor directExecutor() {
        SysUserImportRowTransactionExecutor executor = mock(SysUserImportRowTransactionExecutor.class);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        return executor;
    }

    private static SysUserServiceImpl service(SysUserImportRowTransactionExecutor executor) {
        return new SysUserServiceImpl(null, null, null, null, null,
                null, null, null, null, executor);
    }

    private static SysUserImportVo row(String userName) {
        SysUserImportVo row = new SysUserImportVo();
        row.setUserName(userName);
        return row;
    }
}

package com.jimuqu.system.service.impl;

import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.bo.SysDeptBo;
import com.jimuqu.system.mapper.SysDeptMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SysDeptParentValidationTest {

    @Test
    void rejectsMovingDepartmentBelowItsDescendantWithoutWriting() {
        SysDeptMapper mapper = mock(SysDeptMapper.class);
        SysDeptServiceImpl service = service(mapper);
        when(mapper.getById(10L)).thenReturn(dept(10L, 1L, "0,1", "0"));
        when(mapper.getById(20L)).thenReturn(dept(20L, 10L, "0,1,10", "0"));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.updateByBo(update(10L, 20L)));

        assertEquals("上级部门不能是当前部门或其下级部门", error.getMessage());
        verify(mapper).getById(10L);
        verify(mapper).getById(20L);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void rejectsDisabledNewParentWithoutWriting() {
        SysDeptMapper mapper = mock(SysDeptMapper.class);
        SysDeptServiceImpl service = service(mapper);
        when(mapper.getById(10L)).thenReturn(dept(10L, 1L, "0,1", "0"));
        when(mapper.getById(20L)).thenReturn(dept(20L, 1L, "0,1", "1"));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.updateByBo(update(10L, 20L)));

        assertEquals("部门停用，不允许修改", error.getMessage());
        verify(mapper).getById(10L);
        verify(mapper).getById(20L);
        verifyNoMoreInteractions(mapper);
    }

    private static SysDeptServiceImpl service(SysDeptMapper mapper) {
        ISysDataScopeService dataScopeService = mock(ISysDataScopeService.class);
        when(dataScopeService.checkUserDataScope(nullable(Long.class), anyLong())).thenReturn(true);
        return new SysDeptServiceImpl(mapper, null, null, dataScopeService);
    }

    private static SysDept dept(Long id, Long parentId, String ancestors, String status) {
        return new SysDept().setId(id).setParentId(parentId).setAncestors(ancestors).setStatus(status);
    }

    private static SysDeptBo update(Long id, Long parentId) {
        return new SysDeptBo().setId(id).setParentId(parentId)
                .setDeptName("测试部门").setOrderNum(1).setStatus("0");
    }
}

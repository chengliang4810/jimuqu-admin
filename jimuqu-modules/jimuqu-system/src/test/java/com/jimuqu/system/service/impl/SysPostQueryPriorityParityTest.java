package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.system.domain.SysPost;
import com.jimuqu.system.domain.query.SysPostQuery;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysPostMapper;
import com.jimuqu.system.mapper.SysUserPostMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SysPostQueryPriorityParityTest {

    @Test
    void deptIdTakesPriorityOverBelongDeptTree() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysPostServiceImpl service = service(deptMapper);

        service.applyDepartmentFilter(queryChain(),
                new SysPostQuery().setDeptId(10L).setBelongDeptId(20L));

        verifyNoInteractions(deptMapper);
    }

    @Test
    void belongDeptExpandsTreeWhenDeptIdIsAbsent() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectListByParentId(20L)).thenReturn(new ArrayList<>(List.of(21L)));
        SysPostServiceImpl service = service(deptMapper);

        service.applyDepartmentFilter(queryChain(), new SysPostQuery().setBelongDeptId(20L));

        verify(deptMapper).selectListByParentId(20L);
    }

    private static SysPostServiceImpl service(SysDeptMapper deptMapper) {
        return new SysPostServiceImpl(
                deptMapper,
                mock(SysPostMapper.class),
                mock(SysUserPostMapper.class),
                mock(ISysDataScopeService.class));
    }

    private static QueryChain<SysPost> queryChain() {
        return QueryChain.of(mock(SysPostMapper.class));
    }
}

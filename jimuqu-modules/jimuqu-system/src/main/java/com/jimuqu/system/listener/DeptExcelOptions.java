package com.jimuqu.system.listener;

import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.excel.core.ExcelOptionsProvider;
import com.jimuqu.system.domain.query.SysDeptQuery;
import com.jimuqu.system.domain.vo.SysDeptVo;
import com.jimuqu.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户导入模板的部门完整路径下拉选项。
 */
@Component
@RequiredArgsConstructor
public class DeptExcelOptions implements ExcelOptionsProvider {

    private final SysDeptService deptService;

    @Override
    public Set<String> getOptions() {
        List<SysDeptVo> departments = deptService.queryList(new SysDeptQuery());
        Map<Long, SysDeptVo> byId = new HashMap<>();
        departments.forEach(dept -> byId.put(dept.getId(), dept));

        Set<String> options = new LinkedHashSet<>();
        departments.forEach(dept -> options.add(buildPath(dept, byId, new HashSet<>())));
        return options;
    }

    private String buildPath(SysDeptVo dept, Map<Long, SysDeptVo> byId, Set<Long> visited) {
        if (!visited.add(dept.getId())) {
            throw new ServiceException("部门层级存在循环：" + dept.getDeptName());
        }
        SysDeptVo parent = byId.get(dept.getParentId());
        if (parent == null) {
            return dept.getDeptName();
        }
        return buildPath(parent, byId, visited) + "/" + dept.getDeptName();
    }
}

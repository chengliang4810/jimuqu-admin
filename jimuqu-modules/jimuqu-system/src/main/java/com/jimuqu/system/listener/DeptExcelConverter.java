package com.jimuqu.system.listener;

import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.system.domain.query.SysDeptQuery;
import com.jimuqu.system.domain.vo.SysDeptVo;
import com.jimuqu.system.service.SysDeptService;
import org.noear.solon.Solon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户导入时在部门完整路径与部门 ID 之间转换。
 */
public class DeptExcelConverter implements Converter<Long> {

    @Override
    public Class<Long> supportJavaTypeKey() {
        return Long.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public Long convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                  GlobalConfiguration globalConfiguration) {
        String path = cellData.getStringValue();
        if (StringUtil.isBlank(path)) {
            return null;
        }
        Long deptId = departmentMaps().nameToId().get(path);
        if (deptId == null) {
            throw new ServiceException("部门不存在：" + path);
        }
        return deptId;
    }

    @Override
    public WriteCellData<String> convertToExcelData(Long value, ExcelContentProperty contentProperty,
                                                     GlobalConfiguration globalConfiguration) {
        return new WriteCellData<>(value == null ? "" : departmentMaps().idToName().getOrDefault(value, ""));
    }

    private DepartmentMaps departmentMaps() {
        List<SysDeptVo> departments = Solon.context().getBean(SysDeptService.class)
                .queryList(new SysDeptQuery());
        Map<Long, SysDeptVo> byId = new HashMap<>();
        departments.forEach(dept -> byId.put(dept.getId(), dept));

        Map<Long, String> idToName = new HashMap<>();
        departments.forEach(dept -> idToName.put(dept.getId(), buildPath(dept, byId, new HashSet<>())));
        Map<String, Long> nameToId = new HashMap<>();
        idToName.forEach((id, path) -> nameToId.put(path, id));
        return new DepartmentMaps(idToName, nameToId);
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

    private record DepartmentMaps(Map<Long, String> idToName, Map<String, Long> nameToId) {
    }
}

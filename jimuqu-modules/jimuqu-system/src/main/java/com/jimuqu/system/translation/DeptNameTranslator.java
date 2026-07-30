package com.jimuqu.system.translation;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslationInterface;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 部门 ID 转部门名称。 */
@Component(value = "deptNameTranslator", typed = true)
@RequiredArgsConstructor
public class DeptNameTranslator implements TranslationInterface {

    private final SysDeptMapper deptMapper;

    @Override
    public String translate(Object value, Trans trans) {
        if (value instanceof String text && text.contains(",")) {
            return translateBatch(List.of(value), trans).getFirst();
        }
        Long deptId = TranslationValueSupport.toLong(value);
        if (deptId == null) {
            return trans.defaultValue();
        }
        SysDept dept = deptMapper.getById(deptId);
        return dept == null || dept.getDeptName() == null ? trans.defaultValue() : dept.getDeptName();
    }

    @Override
    public List<String> translateBatch(List<?> values, Trans trans) {
        List<Long> deptIds = TranslationValueSupport.distinctLongs(values);
        Map<Long, String> names = deptIds.isEmpty() ? Map.of() : QueryChain.of(deptMapper)
                .select(SysDept::getId, SysDept::getDeptName)
                .in(SysDept::getId, deptIds)
                .list().stream()
                .filter(dept -> dept.getId() != null && dept.getDeptName() != null)
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName,
                        (first, ignored) -> first));
        return TranslationValueSupport.resolveLongValues(values, names, trans.defaultValue());
    }
}

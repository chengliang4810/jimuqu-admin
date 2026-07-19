package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.common.core.utils.DateUtil;
import com.jimuqu.system.domain.SysRole;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.xbatis.db.annotations.Condition.Type.*;

/**
 * 角色信息查询条件对象
 * @author chengliang4810
 * @since 2025-06-05
 */
@Data
@FieldNameConstants
@ConditionTarget(SysRole.class)
public class SysRoleQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    @Condition(IGNORE)
    private Map<String, Object> params = new HashMap<>();

    /**
     * 角色ID
     */
    @Condition(value = EQ)
    private Long id;
    /**
     * 角色名称
     */
    @Condition(value = LIKE)
    private String roleName;
    /**
     * 角色权限字符串
     */
    @Condition(value = LIKE)
    private String roleKey;
    /**
     * 显示顺序
     */
    @Condition(value = EQ)
    private Integer roleSort;
    /**
     * 数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）
     */
    @Condition(value = EQ)
    private String dataScope;
    /**
     * 菜单树选择项是否关联显示
     */
    @Condition(value = EQ)
    private Boolean menuCheckStrictly;
    /**
     * 部门树选择项是否关联显示
     */
    @Condition(value = EQ)
    private Boolean deptCheckStrictly;
    /**
     * 角色状态（0正常 1停用）
     */
    @Condition(value = EQ)
    private String status;
    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @Condition(value = EQ)
    private String delFlag;
    /**
     * 备注
     */
    @Condition(value = EQ)
    private String remark;

    @Condition(BETWEEN)
    private List<Date> createTime;

    /**
     * 条件构建前执行
     */
    @Override
    public void beforeBuildCondition() {
        Object beginTime = params.get("beginTime");
        Object endTime = params.get("endTime");
        if (beginTime != null && endTime != null) {
            createTime = List.of(
                    DateUtil.dateTime(DateUtil.YYYY_MM_DD_HH_MM_SS, beginTime.toString()),
                    DateUtil.dateTime(DateUtil.YYYY_MM_DD_HH_MM_SS, endTime.toString()));
        }
    }

}

package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import cn.xbatis.db.annotations.Ignore;
import com.jimuqu.common.core.utils.DateUtil;
import com.jimuqu.system.domain.SysDept;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.xbatis.db.annotations.Condition.Type.*;

/**
 * 部门查询条件对象
 * @author chengliang4810
 * @since 2025-06-04
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@ConditionTarget(SysDept.class)
public class SysDeptQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    @Condition(IGNORE)
    private Map<String, Object> params = new HashMap<>();

    /**
     * 主键
     */
    @Condition(value = EQ)
    private Long id;
    /**
     * 所属部门
     */
    @Ignore
    private Long belongDeptId;
    /**
     * 父部门id
     */
    @Condition(value = EQ)
    private Long parentId;
    /**
     * 祖级列表
     */
    @Condition(value = EQ)
    private String ancestors;
    /**
     * 部门名称
     */
    @Condition(value = LIKE)
    private String deptName;
    /**
     * 部门类别编码
     */
    @Condition(value = LIKE)
    private String deptCategory;
    /**
     * 显示顺序
     */
    @Condition(value = EQ)
    private Integer orderNum;
    /**
     * 负责人
     */
    @Condition(value = EQ)
    private Long leader;
    /**
     * 联系电话
     */
    @Condition(value = EQ)
    private String phone;
    /**
     * 邮箱
     */
    @Condition(value = EQ)
    private String email;
    /**
     * 部门状态（0正常 1停用）
     */
    @Condition(value = EQ)
    private String status;
    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @Condition(value = EQ, defaultValue = "0")
    private String delFlag;

    /**
     * 创建时间
     */
    @Condition(value = BETWEEN)
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

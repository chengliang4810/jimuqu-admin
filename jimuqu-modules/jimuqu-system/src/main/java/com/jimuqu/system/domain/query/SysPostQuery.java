package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import cn.xbatis.db.annotations.Ignore;
import com.jimuqu.common.core.utils.DateUtil;
import com.jimuqu.system.domain.SysPost;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.xbatis.db.annotations.Condition.Type.BETWEEN;
import static cn.xbatis.db.annotations.Condition.Type.EQ;
import static cn.xbatis.db.annotations.Condition.Type.IGNORE;
import static cn.xbatis.db.annotations.Condition.Type.LIKE;

/**
 * 岗位信息查询条件对象
 * @author chengliang4810
 * @since 2025-06-04
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@ConditionTarget(SysPost.class)
public class SysPostQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    @Condition(IGNORE)
    private Map<String, Object> params = new HashMap<>();

    /**
     * 岗位ID
     */
    @Condition(value = EQ)
    private Long postId;
    /**
     * 部门id
     */
    @Condition(value = EQ)
    private Long deptId;
    /**
     * 所属部门
     */
    @Ignore
    private Long belongDeptId;
    /**
     * 岗位编码
     */
    @Condition(value = LIKE)
    private String postCode;
    /**
     * 岗位类别编码
     */
    @Condition(value = LIKE)
    private String postCategory;
    /**
     * 岗位名称
     */
    @Condition(value = LIKE)
    private String postName;
    /**
     * 显示顺序
     */
    @Condition(value = EQ)
    private Integer postSort;
    /**
     * 状态（0正常 1停用）
     */
    @Condition(value = EQ)
    private String status;
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

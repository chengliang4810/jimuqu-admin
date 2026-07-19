package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.common.core.utils.DateUtil;
import com.jimuqu.system.domain.SysOperLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static cn.xbatis.db.annotations.Condition.Type.*;

@Data
@ConditionTarget(SysOperLog.class)
public class SysOperLogQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    @Condition(IGNORE)
    private Map<String, Object> params = new HashMap<>();

    @Condition(LIKE)
    private String title;
    @Condition(LIKE)
    private String operName;
    @Condition(LIKE)
    private String operIp;
    @Condition(EQ)
    private Long userId;
    @Condition(EQ)
    private Long deptId;
    @Condition(EQ)
    private String clientKey;
    @Condition(EQ)
    private String deviceType;
    @Condition(LIKE)
    private String browser;
    @Condition(LIKE)
    private String os;
    @Condition(EQ)
    private Integer businessType;
    @Condition(value = IN, property = "businessType")
    private Integer[] businessTypes;
    @Condition(EQ)
    private Integer status;
    @Condition(BETWEEN)
    private List<Date> operTime;

    @Override
    public void beforeBuildCondition() {
        if (businessType != null && businessType <= 0) {
            businessType = null;
        }
        Object beginTime = getParams().get("beginTime");
        Object endTime = getParams().get("endTime");
        if (beginTime != null && endTime != null) {
            operTime = List.of(
                    DateUtil.dateTime(DateUtil.YYYY_MM_DD_HH_MM_SS, beginTime.toString()),
                    DateUtil.dateTime(DateUtil.YYYY_MM_DD_HH_MM_SS, endTime.toString()));
        }
    }
}

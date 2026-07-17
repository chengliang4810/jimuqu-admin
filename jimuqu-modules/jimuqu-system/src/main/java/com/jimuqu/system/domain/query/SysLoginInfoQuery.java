package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.common.core.utils.DateUtil;
import com.jimuqu.system.domain.SysLoginInfo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static cn.xbatis.db.annotations.Condition.Type.*;

@Data
@ConditionTarget(SysLoginInfo.class)
public class SysLoginInfoQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    @Condition(IGNORE)
    private Map<String, Object> params = new HashMap<>();

    @Condition(LIKE)
    private String userName;
    @Condition(LIKE)
    private String ipaddr;
    @Condition(EQ)
    private String status;
    @Condition(BETWEEN)
    private List<Date> loginTime;

    @Override
    public void beforeBuildCondition() {
        Object beginTime = getParams().get("beginTime");
        Object endTime = getParams().get("endTime");
        if (beginTime != null && endTime != null) {
            loginTime = List.of(
                    DateUtil.dateTime(DateUtil.YYYY_MM_DD_HH_MM_SS, beginTime.toString()),
                    DateUtil.dateTime(DateUtil.YYYY_MM_DD_HH_MM_SS, endTime.toString()));
        }
    }
}

package com.jimuqu.system.domain.query;

import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.system.domain.SysNotice;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

import static cn.xbatis.db.annotations.Condition.Type.EQ;
import static cn.xbatis.db.annotations.Condition.Type.LIKE;

/**
 * 通知公告查询条件。
 */
@Data
@ConditionTarget(SysNotice.class)
public class SysNoticeQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Condition(LIKE)
    private String noticeTitle;

    @Condition(EQ)
    private String noticeType;

    @Condition(EQ)
    private String status;

    /** 发布人名称，由服务层转换为用户 ID 条件。 */
    @Condition(cn.xbatis.db.annotations.Condition.Type.IGNORE)
    private String createByName;
}

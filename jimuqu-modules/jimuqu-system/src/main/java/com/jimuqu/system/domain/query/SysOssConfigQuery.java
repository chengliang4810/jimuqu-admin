package com.jimuqu.system.domain.query;

import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.system.domain.SysOssConfig;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

import static cn.xbatis.db.annotations.Condition.Type.EQ;
import static cn.xbatis.db.annotations.Condition.Type.LIKE;

@Data
@ConditionTarget(SysOssConfig.class)
public class SysOssConfigQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Condition(EQ)
    private String configKey;
    @Condition(LIKE)
    private String bucketName;
    @Condition(EQ)
    private String status;
}

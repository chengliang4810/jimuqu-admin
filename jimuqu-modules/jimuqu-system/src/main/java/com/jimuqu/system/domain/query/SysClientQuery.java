package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.system.domain.SysClient;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;

import static cn.xbatis.db.annotations.Condition.Type.*;

/**
 * 授权管理对象 sys_client查询条件对象
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@FieldNameConstants
@ConditionTarget(SysClient.class)
public class SysClientQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Condition(value = EQ)
    private Long id;
    /**
     * 客户端id
     */
    @Condition(value = EQ)
    private String clientId;
    /**
     * 客户端key
     */
    @Condition(value = EQ)
    private String clientKey;
    /**
     * 客户端秘钥
     */
    @Condition(value = EQ)
    private String clientSecret;
    /**
     * 授权类型
     */
    @Condition(value = EQ)
    private String grantType;
    /**
     * 设备类型
     */
    @Condition(value = EQ)
    private String deviceType;
    /**
     * 允许访问路径
     */
    @Condition(value = LIKE)
    private String accessPath;
    /**
     * IP白名单
     */
    @Condition(value = LIKE)
    private String ipWhitelist;
    /**
     * token活跃超时时间
     */
    @Condition(value = EQ)
    private Long activeTimeout;
    /**
     * token固定超时时间
     */
    @Condition(value = EQ)
    private Long timeout;
    /**
     * 状态（0正常 1停用）
     */
    @Condition(value = EQ)
    private String status;
    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @Condition(value = EQ)
    private String delFlag;

    /**
     * 条件构建前执行
     */
    @Override
    public void beforeBuildCondition() {
        
    }

}

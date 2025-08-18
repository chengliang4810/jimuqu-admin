package com.jimuqu.system.domain.query;

import cn.xbatis.db.annotations.Condition;
import com.jimuqu.system.domain.SysApiKey;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;

import static cn.xbatis.db.annotations.Condition.Type.EQ;
import static cn.xbatis.db.annotations.Condition.Type.LIKE;

/**
 * API密钥查询对象
 * @author jimuqu-admin
 * @since 2025-08-18
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = SysApiKey.class)
public class SysApiKeyQuery {

    /**
     * 绑定的用户ID
     */
    @Condition(value = EQ)
    private Long userId;

    /**
     * API Key值
     */
    @Condition(value = EQ)
    private String apiKey;

    /**
     * 名称
     */
    @Condition(value = LIKE)
    private String name;

    /**
     * 权限范围（JSON格式存储）
     */
    private String scope;

    /**
     * 失效时间，13位时间戳，-1=永不失效
     */
    private Long expiresTime;

    /**
     * 是否有效
     */
    @Condition(value = EQ)
    private Boolean isValid;

}
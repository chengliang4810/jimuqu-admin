package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysApiKey;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API密钥视图对象
 * @author jimuqu-admin
 * @since 2025-08-18
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysApiKey.class)
@AutoMapper(target = SysApiKey.class)
public class SysApiKeyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * API密钥ID
     */
    private Long id;

    /**
     * 绑定的用户ID
     */
    private Long userId;

    /**
     * API Key值
     */
    private String apiKey;

    /**
     * 名称
     */
    private String name;

    /**
     * 备注
     */
    private String remark;

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
    private Boolean isValid;

    /**
     * 扩展信息（JSON格式存储）
     */
    private String extraData;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    private String delFlag;

    /**
     * 命名空间
     */
    private String namespace;
}
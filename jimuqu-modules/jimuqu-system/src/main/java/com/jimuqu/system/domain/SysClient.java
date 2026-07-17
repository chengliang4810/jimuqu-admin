package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.*;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;

import java.io.Serial;

/**
 * 授权管理对象 sys_client
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_client")
public class SysClient extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "主键")
    private Long id;
    /**
     * 客户端id
     */
    @AutoColumn(comment = "客户端id", length = 255)
    private String clientId;
    /**
     * 客户端key
     */
    @AutoColumn(comment = "客户端key", length = 255)
    private String clientKey;
    /**
     * 客户端秘钥
     */
    @AutoColumn(comment = "客户端秘钥", length = 255)
    private String clientSecret;
    /**
     * 授权类型
     */
    @AutoColumn(comment = "授权类型", length = 255)
    private String grantType;
    /**
     * 设备类型
     */
    @AutoColumn(comment = "设备类型", length = 255)
    private String deviceType;
    /**
     * 允许访问路径
     */
    @AutoColumn(comment = "允许访问路径", length = 2000)
    private String accessPath;
    /**
     * IP白名单
     */
    @AutoColumn(comment = "IP白名单", length = 2000)
    private String ipWhitelist;
    /**
     * token活跃超时时间
     */
    @AutoColumn(comment = "token活跃超时时间", defaultValue = "-1")
    private Long activeTimeout;
    /**
     * token固定超时时间
     */
    @AutoColumn(comment = "token固定超时时间", defaultValue = "-1")
    private Long timeout;
    /**
     * 状态（0正常 1停用）
     */
    @AutoColumn(comment = "状态（0正常 1停用）", length = 255, defaultValue = "0")
    private String status;
    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @AutoColumn(comment = "删除标志（0代表存在 2代表删除）", length = 255, defaultValue = "0")
    private String delFlag;

}

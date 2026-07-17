package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import cn.idev.excel.annotation.ExcelIgnore;
import com.jimuqu.system.domain.SysClient;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 授权管理对象 sys_client视图对象
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysClient.class)
@AutoMapper(target = SysClient.class)
public class SysClientVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;
    /**
     * 客户端id
     */
    private String clientId;
    /**
     * 客户端key
     */
    private String clientKey;
    /**
     * 客户端秘钥
     */
    private String clientSecret;
    /**
     * 授权类型列表
     */
    @ExcelIgnore
    private List<String> grantTypeList;
    /**
     * 授权类型
     */
    private String grantType;
    /**
     * 设备类型
     */
    private String deviceType;
    /**
     * 允许访问路径
     */
    private String accessPath;
    /**
     * 允许访问路径列表
     */
    @ExcelIgnore
    private List<String> accessPathList;
    /**
     * IP白名单
     */
    private String ipWhitelist;
    /**
     * IP白名单列表
     */
    @ExcelIgnore
    private List<String> ipWhitelistList;
    /**
     * token活跃超时时间
     */
    private Long activeTimeout;
    /**
     * token固定超时时间
     */
    private Long timeout;
    /**
     * 状态（0正常 1停用）
     */
    private String status;
    /**
     * 删除标志（0代表存在 2代表删除）
     */
    private String delFlag;

}

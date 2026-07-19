package com.jimuqu.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.common.excel.annotation.ExcelDictFormat;
import com.jimuqu.common.excel.convert.ExcelDictConvert;
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
@ExcelIgnoreUnannotated
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
    @ExcelProperty(value = "id")
    private Long id;
    /**
     * 客户端id
     */
    @ExcelProperty(value = "客户端id")
    private String clientId;
    /**
     * 客户端key
     */
    @ExcelProperty(value = "客户端key")
    private String clientKey;
    /**
     * 客户端秘钥
     */
    @ExcelProperty(value = "客户端秘钥")
    private String clientSecret;
    /**
     * 授权类型列表
     */
    private List<String> grantTypeList;
    /**
     * 授权类型
     */
    @ExcelProperty(value = "授权类型")
    private String grantType;
    /**
     * 设备类型
     */
    private String deviceType;
    /**
     * 允许访问路径
     */
    @ExcelProperty(value = "允许访问路径")
    private String accessPath;
    /**
     * 允许访问路径列表
     */
    private List<String> accessPathList;
    /**
     * IP白名单
     */
    @ExcelProperty(value = "IP白名单")
    private String ipWhitelist;
    /**
     * IP白名单列表
     */
    private List<String> ipWhitelistList;
    /**
     * token活跃超时时间
     */
    @ExcelProperty(value = "token活跃超时时间")
    private Long activeTimeout;
    /**
     * token固定超时时间
     */
    @ExcelProperty(value = "token固定超时时间")
    private Long timeout;
    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
    private String status;
}

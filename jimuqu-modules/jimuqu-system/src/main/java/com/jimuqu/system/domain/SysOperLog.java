package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 操作日志。
 */
@Data
@Accessors(chain = true)
@Table("sys_oper_log")
public class SysOperLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "操作日志ID")
    private Long operId;
    @AutoColumn(comment = "操作模块", length = 100)
    private String title;
    @AutoColumn(comment = "业务类型")
    private Integer businessType;
    @AutoColumn(comment = "请求方法", length = 500)
    private String method;
    @AutoColumn(comment = "请求方式", length = 20)
    private String requestMethod;
    @AutoColumn(comment = "操作类别")
    private Integer operatorType;
    @AutoColumn(comment = "操作人员", length = 100)
    private String operName;
    @AutoColumn(comment = "操作用户ID")
    private Long userId;
    @AutoColumn(comment = "操作部门ID")
    private Long deptId;
    @AutoColumn(comment = "部门名称", length = 100)
    private String deptName;
    @AutoColumn(comment = "客户端", length = 100)
    private String clientKey;
    @AutoColumn(comment = "设备类型", length = 100)
    private String deviceType;
    @AutoColumn(comment = "浏览器", length = 100)
    private String browser;
    @AutoColumn(comment = "操作系统", length = 100)
    private String os;
    @AutoColumn(comment = "请求URL", length = 500)
    private String operUrl;
    @AutoColumn(comment = "操作IP", length = 128)
    private String operIp;
    @AutoColumn(comment = "操作地点", length = 255)
    private String operLocation;
    @AutoColumn(comment = "请求参数", type = MysqlTypeConstant.TEXT)
    private String operParam;
    @AutoColumn(comment = "返回参数", type = MysqlTypeConstant.TEXT)
    private String jsonResult;
    @AutoColumn(comment = "状态")
    private Integer status;
    @AutoColumn(comment = "错误消息", type = MysqlTypeConstant.TEXT)
    private String errorMsg;
    @AutoColumn(comment = "操作时间")
    private Date operTime;
    @AutoColumn(comment = "耗时")
    private Long costTime;
}

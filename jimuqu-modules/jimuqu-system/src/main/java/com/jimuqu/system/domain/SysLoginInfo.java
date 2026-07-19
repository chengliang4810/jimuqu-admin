package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.Index;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 登录访问日志。
 */
@Data
@Accessors(chain = true)
@Table("sys_login_info")
public class SysLoginInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "登录日志ID")
    private Long infoId;
    @AutoColumn(comment = "用户账号", length = 100)
    private String userName;
    @AutoColumn(comment = "客户端", length = 100)
    private String clientKey;
    @AutoColumn(comment = "设备类型", length = 100)
    private String deviceType;
    @AutoColumn(comment = "登录状态", length = 1)
    @Index(name = "sys_login_info_s")
    private String status;
    @AutoColumn(comment = "登录IP", length = 128)
    private String ipaddr;
    @AutoColumn(comment = "登录地点", length = 255)
    private String loginLocation;
    @AutoColumn(comment = "浏览器", length = 100)
    private String browser;
    @AutoColumn(comment = "操作系统", length = 100)
    private String os;
    @AutoColumn(comment = "提示消息", length = 500)
    private String msg;
    @AutoColumn(comment = "登录时间")
    @Index(name = "sys_login_info_lt")
    private Date loginTime;
}

package com.jimuqu.system.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysLoginInfo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ResultEntity(SysLoginInfo.class)
public class SysLoginInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("序号")
    private Long infoId;
    @ExcelProperty("用户账号")
    private String userName;
    @ExcelProperty("客户端")
    private String clientKey;
    @ExcelProperty("设备类型")
    private String deviceType;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("登录IP")
    private String ipaddr;
    @ExcelProperty("登录地点")
    private String loginLocation;
    @ExcelProperty("浏览器")
    private String browser;
    @ExcelProperty("操作系统")
    private String os;
    @ExcelProperty("提示消息")
    private String msg;
    @ExcelProperty("登录时间")
    private Date loginTime;
}

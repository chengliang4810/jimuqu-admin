package com.jimuqu.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.common.excel.annotation.ExcelDictFormat;
import com.jimuqu.common.excel.convert.ExcelDictConvert;
import com.jimuqu.system.domain.SysLoginInfo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
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
    @ExcelProperty(value = "设备类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_device_type")
    private String deviceType;
    @ExcelProperty(value = "登录状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_common_status")
    private String status;
    @ExcelProperty("登录地址")
    private String ipaddr;
    @ExcelProperty("登录地点")
    private String loginLocation;
    @ExcelProperty("浏览器")
    private String browser;
    @ExcelProperty("操作系统")
    private String os;
    @ExcelProperty("提示消息")
    private String msg;
    @ExcelProperty("访问时间")
    private Date loginTime;
}

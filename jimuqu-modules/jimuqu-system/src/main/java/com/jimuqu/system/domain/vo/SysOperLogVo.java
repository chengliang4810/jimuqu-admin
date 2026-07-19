package com.jimuqu.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.xbatis.db.annotations.Ignores;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.common.excel.annotation.ExcelDictFormat;
import com.jimuqu.common.excel.convert.ExcelDictConvert;
import com.jimuqu.system.domain.SysOperLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
@ResultEntity(SysOperLog.class)
@Ignores("businessTypes")
public class SysOperLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("日志主键")
    private Long operId;
    @ExcelProperty("操作模块")
    private String title;
    @ExcelProperty(value = "业务类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_oper_type")
    private Integer businessType;
    private Integer[] businessTypes;
    @ExcelProperty("请求方法")
    private String method;
    @ExcelProperty("请求方式")
    private String requestMethod;
    @ExcelProperty(value = "操作类别", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=其它,1=后台用户,2=手机端用户")
    private Integer operatorType;
    @ExcelProperty("操作人员")
    private String operName;
    @ExcelProperty("操作用户ID")
    private Long userId;
    @ExcelProperty("操作部门ID")
    private Long deptId;
    @ExcelProperty("部门名称")
    private String deptName;
    @ExcelProperty("客户端")
    private String clientKey;
    @ExcelProperty(value = "设备类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_device_type")
    private String deviceType;
    @ExcelProperty("浏览器")
    private String browser;
    @ExcelProperty("操作系统")
    private String os;
    @ExcelProperty("请求地址")
    private String operUrl;
    @ExcelProperty("操作地址")
    private String operIp;
    @ExcelProperty("操作地点")
    private String operLocation;
    @ExcelProperty("请求参数")
    private String operParam;
    @ExcelProperty("返回参数")
    private String jsonResult;
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_common_status")
    private Integer status;
    @ExcelProperty("错误消息")
    private String errorMsg;
    @ExcelProperty("操作时间")
    private Date operTime;
    @ExcelProperty("消耗时间")
    private Long costTime;
}

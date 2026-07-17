package com.jimuqu.system.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysOperLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ResultEntity(SysOperLog.class)
public class SysOperLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("日志主键")
    private Long operId;
    @ExcelProperty("操作模块")
    private String title;
    @ExcelProperty("业务类型")
    private Integer businessType;
    @ExcelProperty("请求方法")
    private String method;
    @ExcelProperty("请求方式")
    private String requestMethod;
    @ExcelProperty("操作类别")
    private Integer operatorType;
    @ExcelProperty("操作人员")
    private String operName;
    private Long userId;
    private Long deptId;
    @ExcelProperty("部门名称")
    private String deptName;
    private String clientKey;
    private String deviceType;
    private String browser;
    private String os;
    @ExcelProperty("请求地址")
    private String operUrl;
    @ExcelProperty("操作IP")
    private String operIp;
    @ExcelProperty("操作地点")
    private String operLocation;
    @ExcelProperty("请求参数")
    private String operParam;
    @ExcelProperty("返回参数")
    private String jsonResult;
    @ExcelProperty("状态")
    private Integer status;
    @ExcelProperty("错误消息")
    private String errorMsg;
    @ExcelProperty("操作时间")
    private Date operTime;
    @ExcelProperty("耗时")
    private Long costTime;
}

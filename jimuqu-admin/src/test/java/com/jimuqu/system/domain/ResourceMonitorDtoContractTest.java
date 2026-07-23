package com.jimuqu.system.domain;

import cn.idev.excel.annotation.ExcelProperty;
import cn.xbatis.db.annotations.Condition;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.system.controller.SysFileController;
import com.jimuqu.system.domain.query.SysOperLogQuery;
import com.jimuqu.system.domain.vo.CacheListInfoVo;
import com.jimuqu.system.domain.vo.SysLoginInfoVo;
import com.jimuqu.system.domain.vo.SysOperLogVo;
import com.jimuqu.system.domain.vo.SysUserOnlineVo;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.handle.UploadedFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResourceMonitorDtoContractTest {

    @Test
    void usesUpstreamCompatibleNumericTypes() throws NoSuchFieldException {
        assertEquals(Long.class, CacheListInfoVo.class.getDeclaredField("dbSize").getType());
        assertEquals(Long.class, SysUserOnlineVo.class.getDeclaredField("loginTime").getType());
    }

    @Test
    void keepsAllUpstreamOperationLogFilters() throws NoSuchFieldException {
        assertCondition("userId", Condition.Type.EQ);
        assertCondition("deptId", Condition.Type.EQ);
        assertCondition("clientKey", Condition.Type.EQ);
        assertCondition("deviceType", Condition.Type.EQ);
        assertCondition("browser", Condition.Type.LIKE);
        assertCondition("os", Condition.Type.LIKE);
    }

    @Test
    void keepsUpstreamAuditExportColumns() throws NoSuchFieldException {
        assertExcelTitle(SysOperLogVo.class, "userId", "操作用户ID");
        assertExcelTitle(SysOperLogVo.class, "deptId", "操作部门ID");
        assertExcelTitle(SysOperLogVo.class, "clientKey", "客户端");
        assertExcelTitle(SysOperLogVo.class, "browser", "浏览器");
        assertExcelTitle(SysOperLogVo.class, "os", "操作系统");
        assertExcelTitle(SysLoginInfoVo.class, "ipaddr", "登录地址");
        assertExcelTitle(SysLoginInfoVo.class, "loginTime", "访问时间");
    }

    @Test
    void recordsOssUploadAsUpstreamInsertOperation() throws NoSuchMethodException {
        Log annotation = SysFileController.class.getMethod("upload", UploadedFile.class, String.class)
                .getAnnotation(Log.class);
        assertNotNull(annotation);
        assertEquals(BusinessType.ADD, annotation.businessType());
    }

    private void assertCondition(String fieldName, Condition.Type expected) throws NoSuchFieldException {
        Condition condition = SysOperLogQuery.class.getDeclaredField(fieldName).getAnnotation(Condition.class);
        assertNotNull(condition, fieldName + " 必须声明 Xbatis 查询条件");
        assertEquals(expected, condition.value());
    }

    private void assertExcelTitle(Class<?> type, String fieldName, String expected) throws NoSuchFieldException {
        ExcelProperty property = type.getDeclaredField(fieldName).getAnnotation(ExcelProperty.class);
        assertNotNull(property, fieldName + " 必须参与导出");
        assertEquals(expected, property.value()[0]);
    }
}

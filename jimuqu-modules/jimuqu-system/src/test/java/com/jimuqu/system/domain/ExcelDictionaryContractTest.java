package com.jimuqu.system.domain;

import cn.idev.excel.annotation.ExcelProperty;
import com.jimuqu.common.excel.annotation.ExcelDictFormat;
import com.jimuqu.common.excel.convert.ExcelDictConvert;
import com.jimuqu.system.domain.vo.SysClientVo;
import com.jimuqu.system.domain.vo.SysConfigVo;
import com.jimuqu.system.domain.vo.SysDeptVo;
import com.jimuqu.system.domain.vo.SysDictDataVo;
import com.jimuqu.system.domain.vo.SysLoginInfoVo;
import com.jimuqu.system.domain.vo.SysOperLogVo;
import com.jimuqu.system.domain.vo.SysPostVo;
import com.jimuqu.system.domain.vo.SysRoleVo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExcelDictionaryContractTest {

    @Test
    void codedExportFieldsUseTheUpstreamDictionaryConverter() throws Exception {
        assertDictionary(SysClientVo.class, "status", "", "0=正常,1=停用");
        assertDictionary(SysConfigVo.class, "configType", "sys_yes_no", "");
        assertDictionary(SysDeptVo.class, "status", "sys_normal_disable", "");
        assertDictionary(SysDictDataVo.class, "isDefault", "sys_yes_no", "");
        assertDictionary(SysLoginInfoVo.class, "deviceType", "sys_device_type", "");
        assertDictionary(SysLoginInfoVo.class, "status", "sys_common_status", "");
        assertDictionary(SysOperLogVo.class, "businessType", "sys_oper_type", "");
        assertDictionary(SysOperLogVo.class, "operatorType", "",
                "0=其它,1=后台用户,2=手机端用户");
        assertDictionary(SysOperLogVo.class, "deviceType", "sys_device_type", "");
        assertDictionary(SysOperLogVo.class, "status", "sys_common_status", "");
        assertDictionary(SysPostVo.class, "status", "sys_normal_disable", "");
        assertDictionary(SysRoleVo.class, "dataScope", "",
                "1=全部数据权限,2=自定义数据权限,3=本部门数据权限,4=本部门及以下数据权限,5=仅本人数据权限,6=部门及以下或本人数据权限");
        assertDictionary(SysRoleVo.class, "status", "sys_normal_disable", "");
    }

    private void assertDictionary(Class<?> type, String fieldName, String dictType,
                                  String readConverterExp) throws NoSuchFieldException {
        Field field = type.getDeclaredField(fieldName);
        ExcelProperty property = field.getAnnotation(ExcelProperty.class);
        ExcelDictFormat format = field.getAnnotation(ExcelDictFormat.class);

        assertNotNull(property, type.getSimpleName() + "." + fieldName);
        assertEquals(ExcelDictConvert.class, property.converter());
        assertNotNull(format, type.getSimpleName() + "." + fieldName);
        assertEquals(dictType, format.dictType());
        assertEquals(readConverterExp, format.readConverterExp());
    }
}

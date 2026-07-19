package com.jimuqu.system.domain.vo;

import cn.idev.excel.EasyExcel;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelExportVoContractTest {

    @Test
    void exportedHeadersMatchRuoYiSixContractAndHideInternalFields() throws Exception {
        assertHeaders(SysClientVo.class, List.of(
                "id", "客户端id", "客户端key", "客户端秘钥", "授权类型", "允许访问路径",
                "IP白名单", "token活跃超时时间", "token固定超时时间", "状态"));
        assertHeaders(SysConfigVo.class, List.of(
                "参数主键", "参数名称", "参数键名", "参数键值", "系统内置", "备注", "创建时间"));
        assertHeaders(SysDictDataVo.class, List.of(
                "字典编码", "字典排序", "字典标签", "字典键值", "字典类型", "是否默认", "备注", "创建时间"));
        assertHeaders(SysDictTypeVo.class, List.of(
                "字典主键", "字典名称", "字典类型", "备注", "创建时间"));
        assertHeaders(SysDeptVo.class, List.of(
                "部门id", "部门名称", "部门类别编码", "负责人", "联系电话", "邮箱", "部门状态", "创建时间"));
        assertHeaders(SysPostVo.class, List.of(
                "岗位序号", "部门id", "岗位编码", "岗位名称", "类别编码", "岗位排序", "状态", "备注", "创建时间"));
        assertHeaders(SysRoleVo.class, List.of(
                "角色序号", "角色名称", "角色权限", "角色排序", "数据范围", "菜单树选择项是否关联显示",
                "部门树选择项是否关联显示", "角色状态", "备注", "创建时间"));
        assertHeaders(SysLoginInfoVo.class, List.of(
                "序号", "用户账号", "客户端", "设备类型", "登录状态", "登录地址", "登录地点", "浏览器",
                "操作系统", "提示消息", "访问时间"));
        assertHeaders(SysOperLogVo.class, List.of(
                "日志主键", "操作模块", "业务类型", "请求方法", "请求方式", "操作类别", "操作人员", "操作用户ID",
                "操作部门ID", "部门名称", "客户端", "设备类型", "浏览器", "操作系统", "请求地址", "操作地址",
                "操作地点", "请求参数", "返回参数", "状态", "错误消息", "操作时间", "消耗时间"));
        assertHeaders(SysUserExportVo.class, List.of(
                "用户序号", "用户账号", "部门名称", "用户昵称", "用户邮箱", "手机号码", "用户性别", "账号状态",
                "最后登录IP", "最后登录时间", "部门负责人"));
        assertHeaders(SysUserImportVo.class, List.of(
                "用户序号", "部门名称", "用户账号", "用户昵称", "用户邮箱", "手机号码", "用户性别", "账号状态"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertHeaders(Class<?> type, List<String> expected) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, (Class) type).sheet("导出").doWrite(List.of());
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            Row header = workbook.getSheetAt(0).getRow(0);
            List<String> actual = new ArrayList<>(header.getLastCellNum());
            for (int index = 0; index < header.getLastCellNum(); index++) {
                actual.add(header.getCell(index).getStringCellValue());
            }
            assertEquals(expected, actual, type.getSimpleName() + " 导出表头与上游不一致");
        }
    }
}

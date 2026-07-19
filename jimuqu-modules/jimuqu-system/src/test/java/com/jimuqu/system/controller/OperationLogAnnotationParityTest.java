package com.jimuqu.system.controller;

import com.jimuqu.common.log.annotation.Log;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OperationLogAnnotationParityTest {

    @Test
    void managementControllersUseUpstreamOperationTitles() {
        assertOnlyLogTitle(SysClientController.class, "客户端管理");
        assertOnlyLogTitle(SysConfigController.class, "参数管理");
        assertOnlyLogTitle(SysDictTypeController.class, "字典类型");
        assertOnlyLogTitle(SysDictDataController.class, "字典数据");
    }

    private static void assertOnlyLogTitle(Class<?> controllerType, String expectedTitle) {
        List<String> titles = Arrays.stream(controllerType.getDeclaredMethods())
                .map(method -> method.getAnnotation(Log.class))
                .filter(java.util.Objects::nonNull)
                .map(Log::title)
                .toList();
        assertFalse(titles.isEmpty(), controllerType.getSimpleName() + " 必须记录操作日志");
        assertEquals(1, titles.stream().distinct().count(),
                controllerType.getSimpleName() + " 的操作日志模块名称必须统一");
        assertEquals(expectedTitle, titles.get(0));
    }
}

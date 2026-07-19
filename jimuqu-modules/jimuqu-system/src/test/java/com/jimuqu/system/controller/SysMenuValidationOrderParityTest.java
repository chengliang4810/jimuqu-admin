package com.jimuqu.system.controller;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.system.domain.bo.SysMenuBo;
import com.jimuqu.system.service.SysMenuService;
import com.jimuqu.system.service.SysRoleService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SysMenuValidationOrderParityTest {

    @Test
    void invalidExternalLinkWinsOverRouteConflict() {
        SysMenuService menuService = proxy(SysMenuService.class, (method, args) -> switch (method.getName()) {
            case "checkMenuNameUnique" -> true;
            case "checkRouteConfigUnique" -> false;
            default -> defaultValue(method.getReturnType());
        });
        SysMenuController controller = new SysMenuController(menuService, proxy(SysRoleService.class,
                (method, args) -> defaultValue(method.getReturnType())));
        SysMenuBo menu = new SysMenuBo().setMenuName("外链").setIsFrame("Y").setPath("invalid");

        R<Void> response = controller.add(menu);

        assertEquals("新增菜单'外链'失败，地址必须以http(s)://开头", response.getMsg());
    }

    @Test
    void cascadeDeleteUsesUpstreamChildMenuWarning() {
        SysMenuService menuService = proxy(SysMenuService.class, (method, args) -> {
            if ("hasChildByMenuId".equals(method.getName())) {
                return true;
            }
            return defaultValue(method.getReturnType());
        });
        SysMenuController controller = new SysMenuController(menuService, proxy(SysRoleService.class,
                (method, args) -> defaultValue(method.getReturnType())));

        R<Void> response = controller.cascadeDelete(List.of(7L));

        assertEquals("存在子菜单,不允许删除", response.getMsg());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> invocation.invoke(method, args));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        return null;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(java.lang.reflect.Method method, Object[] arguments) throws Throwable;
    }
}

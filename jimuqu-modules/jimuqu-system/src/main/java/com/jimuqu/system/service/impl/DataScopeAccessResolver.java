package com.jimuqu.system.service.impl;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.jimuqu.common.mybatis.model.DataScopeAccess;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.noear.solon.core.handle.Action;
import org.noear.solon.core.handle.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 从当前 Solon Action 提取参与数据范围计算的权限约束。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DataScopeAccessResolver {

    static DataScopeAccess current() {
        Context context = Context.current();
        if (context == null) {
            return DataScopeAccess.unconstrained();
        }
        Action action = context.action();
        if (action == null) {
            return DataScopeAccess.unconstrained();
        }
        return resolve(action.method().getMethod(), action.controller().clz());
    }

    static DataScopeAccess resolve(Method method, Class<?> controllerType) {
        Set<String> permissions = new LinkedHashSet<>();
        Set<String> roleKeys = new LinkedHashSet<>();

        SaCheckPermission permission = find(method, controllerType, SaCheckPermission.class);
        if (permission != null) {
            addAll(permissions, permission.value());
            addAll(roleKeys, permission.orRole());
        }
        SaCheckRole role = find(method, controllerType, SaCheckRole.class);
        if (role != null) {
            addAll(roleKeys, role.value());
        }
        return DataScopeAccess.of(permissions, roleKeys);
    }

    private static <A extends Annotation> A find(Method method, Class<?> controllerType, Class<A> type) {
        A annotation = method.getAnnotation(type);
        return annotation != null ? annotation : controllerType.getAnnotation(type);
    }

    private static void addAll(Set<String> target, String[] values) {
        if (values != null) {
            Arrays.stream(values).filter(value -> value != null && !value.isBlank())
                    .map(String::trim).forEach(target::add);
        }
    }
}

package com.jimuqu.common.mybatis.interceptor;

import cn.xbatis.core.mybatis.mapper.intercept.Invocation;
import cn.xbatis.core.mybatis.mapper.intercept.MethodInterceptor;
import com.jimuqu.common.mybatis.annotation.DataPermission;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * XBatis数据权限方法拦截器
 * <p>
 * 拦截带有@DataPermission注解的方法，设置数据权限上下文
 * 在方法执行前将注解信息存入ThreadLocal，执行后清理
 *
 * @author chengliang4810
 * @version 1.0
 */
@Slf4j
public class XbatisDataPermissionMethodInterceptor implements MethodInterceptor {

    @Override
    public Object around(Invocation invocation) throws Throwable {
        Method method = invocation.getMethod();

        // 检查方法是否有@DataPermission注解
        DataPermission dataPermission = method.getAnnotation(DataPermission.class);
        if (dataPermission == null) {
            // 检查类上是否有注解
            dataPermission = method.getDeclaringClass().getAnnotation(DataPermission.class);
        }

        if (dataPermission != null) {
            log.debug("发现数据权限注解，方法: {}", method.getName());
            DataPermissionHolder.push(dataPermission);
            try {
                return invocation.proceed();
            } finally {
                DataPermissionHolder.pop();
                log.debug("清理数据权限注解，方法: {}", method.getName());
            }
        } else {
            return invocation.proceed();
        }
    }

}
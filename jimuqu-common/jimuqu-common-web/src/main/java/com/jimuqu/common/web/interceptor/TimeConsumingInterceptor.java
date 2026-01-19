package com.jimuqu.common.web.interceptor;

import cn.hutool.v7.core.map.Dict;
import cn.hutool.v7.core.map.MapUtil;
import com.jimuqu.common.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.text.StrUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Action;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

import java.util.List;
import java.util.Map;

/**
 * 耗时拦截器
 *
 * @author chengliang
 * @date 2025/12/14
 */
@Slf4j
@Component(index = -99)
public class TimeConsumingInterceptor implements RouterInterceptor {

    /**
     * 排除敏感属性字段
     */
    public static final String[] EXCLUDE_PROPERTIES = {"password", "oldPassword", "newPassword", "confirmPassword", "Authorization", "clientid"};

    /**
     * 执行拦截
     */
    @Override
    public void doIntercept(Context ctx, Handler mainHandler, RouterInterceptorChain chain) throws Throwable {

        Action action = ctx.action();
        if (action == null){
            chain.doIntercept(ctx, mainHandler);
            return;
        }
        // 获取请求参数
        Map<String, List<String>> paramValueMap = ctx.paramMap().toValuesMap();
        // 获取请求体
        Dict bodyDict = JsonUtil.toMap(ctx.body());

        // 移除敏感属性字段
        MapUtil.removeAny(paramValueMap, EXCLUDE_PROPERTIES);
        MapUtil.removeAny(bodyDict, EXCLUDE_PROPERTIES);

        System.err.println(StrUtil.format("开始请求[{}] ,请求方式:[{}], 参数: [{}], Body: [{}]", action.fullName(), ctx.method(), paramValueMap, bodyDict));

        // 开始计时
        long startTime = System.currentTimeMillis();
        try {
            chain.doIntercept(ctx, mainHandler);
        }finally {
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            // 输出执行时间
            System.err.println(StrUtil.format("结束请求[{}] ,耗时:[{}ms]\n", action.fullName(), executionTime));
        }
    }

}

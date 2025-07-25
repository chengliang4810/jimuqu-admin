package com.jimuqu.common.web.interceptor;

import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.text.StrUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Action;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

@Slf4j
@Component(index = -99)
public class TimeConsumingInterceptor implements RouterInterceptor {

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

        System.err.println(StrUtil.format("开始请求[{}] ,请求方式:[{}], 参数: [{}], Body: [{}]", action.fullName(), ctx.method(), ctx.paramMap().toValuesMap(), ctx.body()));

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

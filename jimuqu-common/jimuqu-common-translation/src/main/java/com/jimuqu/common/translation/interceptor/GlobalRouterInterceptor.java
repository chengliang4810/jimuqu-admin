package com.jimuqu.common.translation.interceptor;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.translation.service.TranslationService;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

@Component
public class GlobalRouterInterceptor implements RouterInterceptor {

    @Inject
    private TranslationService translationService;

    @Override
    public void doIntercept(Context ctx, Handler mainHandler, RouterInterceptorChain chain) throws Throwable {
        chain.doIntercept(ctx, mainHandler);
    }

    @Override
    public Object postResult(Context ctx, Object result) throws Throwable {

        //此处为拦截处理
        if (result != null && !(result instanceof Throwable) && ctx.action() != null) {
            // 继承BaseController的情况会固定返回R类型的对象
            if (result instanceof R resultR){
                if (R.FAIL != resultR.getCode()){
                    // 得到数据，进行翻译
                    translationService.translate(resultR.getData());
                }
            }
        }

        return result;
    }

}

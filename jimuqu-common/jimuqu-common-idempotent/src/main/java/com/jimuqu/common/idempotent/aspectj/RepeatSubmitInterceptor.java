package com.jimuqu.common.idempotent.aspectj;

import cn.dev33.satoken.SaManager;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.ObjUtil;
import cn.hutool.v7.crypto.SecureUtil;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.MessageUtils;
import com.jimuqu.common.idempotent.annotation.RepeatSubmit;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Managed;
import org.noear.solon.core.handle.Action;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;
import org.noear.solon.core.util.MultiMap;
import org.noear.solon.lang.Nullable;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import com.jimuqu.common.idempotent.validation.NoRepeatSubmitCheckerImpl;

import java.time.Duration;
import java.util.StringJoiner;

/**
 * 防止重复提交
 *
 * @author chengliang
 * @date 2025/12/14
 */
@Slf4j
@Managed(index = 800)
public class RepeatSubmitInterceptor implements RouterInterceptor {

    @Inject
    private NoRepeatSubmitCheckerImpl noRepeatSubmitChecker;

    @Override
    public void doIntercept(Context ctx, @Nullable Handler mainHandler, RouterInterceptorChain chain) throws Throwable {

        Action action = (mainHandler instanceof Action ? (Action) mainHandler : null);
        if (action == null) {
            chain.doIntercept(ctx, mainHandler);
            return;
        }

        RepeatSubmit repeatSubmit = action.method().getAnnotation(RepeatSubmit.class);
        NoRepeatSubmit noRepeatSubmit = action.method().getAnnotation(NoRepeatSubmit.class);

        // 无注解则不处理
        if (repeatSubmit == null && noRepeatSubmit == null) {
            chain.doIntercept(ctx, mainHandler);
            return;
        }

        if (repeatSubmit == null) {
            interceptSolonAnnotation(ctx, mainHandler, chain);
            return;
        }

        // 如果注解不为0 则使用注解数值
        long interval = repeatSubmit.timeUnit().toMillis(repeatSubmit.interval());

        if (interval < 1000) {
            throw new ServiceException("重复提交间隔时间不能小于'1'秒");
        }

        String nowParams = argsArrayToString(ctx.paramMap(), ctx.bodyNew());

        // 请求地址（作为存放cache的key值）
        String url = ctx.path();

        // 唯一值（没有消息头则使用请求地址）
        String submitKey = StrUtil.trimToEmpty(ctx.header(SaManager.getConfig().getTokenName()));

        submitKey = SecureUtil.md5(submitKey + ":" + nowParams);

        // 唯一标识（指定key + url + 消息头）
        String cacheRepeatKey = GlobalConstants.REPEAT_SUBMIT_KEY + url + submitKey;
        if (!noRepeatSubmitChecker.tryReserve(cacheRepeatKey, Duration.ofMillis(interval))) {
            throw new ServiceException(resolveMessage(repeatSubmit.message()));
        }

        try {
            chain.doIntercept(ctx, mainHandler);
            if (needClearCache(ctx)) {
                noRepeatSubmitChecker.release(cacheRepeatKey);
            }
        } catch (Throwable throwable) {
            noRepeatSubmitChecker.release(cacheRepeatKey);
            throw throwable;
        }
    }

    private void interceptSolonAnnotation(Context ctx, Handler mainHandler,
                                          RouterInterceptorChain chain) throws Throwable {
        try {
            chain.doIntercept(ctx, mainHandler);
            noRepeatSubmitChecker.complete(!needClearCache(ctx));
        } catch (Throwable throwable) {
            noRepeatSubmitChecker.complete(false);
            throw throwable;
        }
    }

    /**
     * 需要清除缓存
     *
     * @param ctx ctx
     * @return boolean
     */
    private boolean needClearCache(Context ctx) {
        // 代码异常
        if (ctx.result instanceof Exception){
            return true;
        }
        // 业务异常
        else if (ctx.result instanceof R<?> r && r.getCode() != R.SUCCESS) {
            return true;
        }
        return false;
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(MultiMap<String> paramsArray, String body) {
        StringJoiner params = new StringJoiner(" ");
        if (StrUtil.isNotBlank(body)) {
            params.add(body);
        }
        if (ObjUtil.isNotNull(paramsArray) && !paramsArray.isEmpty()) {
            params.add(JsonUtil.toString(paramsArray));
        }
        return params.toString();
    }

    static String resolveMessage(String message) {
        if (StrUtil.isWrap(message, "{", "}")) {
            return MessageUtils.message(StrUtil.sub(message, 1, -1));
        }
        return message;
    }

}

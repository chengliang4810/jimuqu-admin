package com.jimuqu.common.idempotent.validation;

import cn.dev33.satoken.SaManager;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.ObjUtil;
import cn.hutool.v7.crypto.SecureUtil;
import com.jimuqu.common.core.constant.GlobalConstants;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Init;
import org.noear.solon.core.handle.Context;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NoRepeatSubmitChecker;
import org.noear.solon.validation.ValidatorManager;

/**
 * 重复提交检查器
 *
 * @author chengliang
 * @date 2025/12/14
 */
@Slf4j
@Component
public class NoRepeatSubmitCheckerImpl implements NoRepeatSubmitChecker {

    @Inject
    private CacheService cacheService;

    @Init
    public void register() {
        ValidatorManager.setNoRepeatSubmitChecker(this);
    }

    @Override
    public boolean check(NoRepeatSubmit anno, Context ctx, String submitHash, int limitSeconds) {

        // 唯一值（没有消息头则使用请求地址）
        String submitKey = StrUtil.trimToEmpty(ctx.header(SaManager.getConfig().getTokenName()));

        submitKey = SecureUtil.md5(submitKey + ":" + submitHash + ":" + requestBody(ctx));

        // 唯一标识（指定key + url + 消息头）
        String cacheRepeatKey = GlobalConstants.REPEAT_SUBMIT_KEY + ctx.method() + ":" + ctx.url() + submitKey;
        String value = cacheService.get(cacheRepeatKey, String.class);

        // 存在相同请求
        if (ObjUtil.isNull(value)) {
            cacheService.store(cacheRepeatKey, "", limitSeconds);
            return true;
        }
        return false;
    }

    private String requestBody(Context ctx) {
        try {
            return StrUtil.trimToEmpty(ctx.body());
        } catch (Exception e) {
            log.warn("读取请求体失败，防重复提交校验退回框架摘要: {}", e.getMessage());
            return "";
        }
    }

}

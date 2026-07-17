package com.jimuqu.common.sse.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.sse.core.SseEmitterManager;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Produces;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.util.MimeType;
import org.noear.solon.web.sse.SseEmitter;

/**
 * SSE 控制器
 *
 * @author chengliang4810
 */
public class SseController {

    @Inject
    private SseEmitterManager sseEmitterManager;

    /**
     * 建立 SSE 连接
     */
    @Get
    @Mapping
    @Produces(MimeType.TEXT_EVENT_STREAM_UTF8_VALUE)
    public SseEmitter connect(Context context) {
        context.headerSet("Cache-Control", "no-cache");
        context.headerSet("X-Accel-Buffering", "no");
        String tokenValue = StpUtil.getTokenValue();
        Long userId = LoginHelper.getUserId();
        return sseEmitterManager.connect(userId, tokenValue);
    }

    /**
     * 关闭 SSE 连接
     */
    @Get
    @SaIgnore
    @Mapping(value = "close")
    public R<Void> close() {
        StpUtil.checkLogin();
        String tokenValue = StpUtil.getTokenValue();
        Long userId = LoginHelper.getUserId();
        sseEmitterManager.disconnect(userId, tokenValue);
        return R.ok();
    }

}

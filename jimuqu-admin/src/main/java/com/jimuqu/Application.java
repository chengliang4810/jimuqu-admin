package com.jimuqu;

import cn.hutool.v7.core.thread.ThreadUtil;
import com.jimuqu.common.ratelimit.annotation.RateLimit;
import com.jimuqu.common.ratelimit.enums.RateLimitAlgorithm;
import com.jimuqu.common.ratelimit.enums.RateLimitType;
import com.jimuqu.common.sse.utils.SseMessageUtil;
import com.jimuqu.domain.SystemVersion;
import org.dromara.autotable.solon.annotation.EnableAutoTable;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.SolonMain;

/**
 * 应用启动类
 *
 * @author chengliang
 * @since 2024/02/26
 */
@SolonMain
@Controller
@EnableAutoTable
public class Application {

    public static void main(String[] args) {
        Solon.start(Application.class, args);
        ThreadUtil.execAsync(() -> {
            while (true){
                ThreadUtil.sleep(1000);
                SseMessageUtil.sendMessage("测试消息");
            }
        });
    }

    /**
     * 获取应用版本号
     *
     * @return 版本号
     */
    @Get
    @Mapping
    @RateLimit(
            type = RateLimitType.IP,  // 基于IP限流
            permitsPerSecond = 1,                  // 提高到合理的值
            maxBurst = 3,
            window = 10,// 设置合理的突发值
            algorithm = RateLimitAlgorithm.FIXED_WINDOW,
            message = "请求过于频繁"
    )
    public SystemVersion version() {
        return SystemVersion.builder()
                .name("LayJava-Admin开源管理系统")
                .version(Solon.cfg().get("solon.app.version"))
                .build();
    }

}

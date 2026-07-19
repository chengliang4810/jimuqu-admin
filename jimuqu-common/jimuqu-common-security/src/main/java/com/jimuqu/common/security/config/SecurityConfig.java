package com.jimuqu.common.security.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.solon.integration.SaTokenInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.jimuqu.common.security.handler.SecurityExceptionHandler;
import com.jimuqu.common.security.properties.SecurityProperties;
import com.jimuqu.common.satoken.utils.ClientAccessValidator;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;

@Configuration
public class SecurityConfig {

    /**
     * sa令牌拦截器
     * 用于支持规划处理及注解处理
     *
     * @return {@link SaTokenInterceptor}
     */
    @Bean(index = -100)
    public SaTokenInterceptor saTokenInterceptor(@Inject SecurityProperties securityProperties) {
        String[] excludes = {};
        if (securityProperties != null && securityProperties.getExcludes() != null){
            excludes = securityProperties.getExcludes();
        }
        return new SaTokenInterceptor()
                // 指定 [拦截路由] 与 [放行路由]
                .addInclude("/**")
                .addExclude("/favicon.ico")
                // 排除不需要拦截的路径
                .addExclude(excludes)
                // 认证函数: 每次请求执行
                .setAuth(req -> {
                    SaRouter.match("/**", () -> {
                        StpUtil.checkLogin();
                        ClientAccessValidator.validateCurrent(Context.current());
                    });
                })
                .setError(new SecurityExceptionHandler())
                // 前置函数：在每次认证函数之前执行
                .setBeforeAuth(req -> {
                    // ---------- 设置一些安全响应头 ----------
                    SaHolder.getResponse()
                            // 服务器名称
                            .setServer("sa-server")
                            // 是否可以在iframe显示视图： DENY=不可以 | SAMEORIGIN=同域下可以 | ALLOW-FROM uri=指定域名下可以
                            .setHeader("X-Frame-Options", "SAMEORIGIN")
                            // 是否启用浏览器默认XSS防护： 0=禁用 | 1=启用 | 1; mode=block 启用, 并在检查到XSS攻击时，停止渲染页面
                            .setHeader("X-XSS-Protection", "1; mode=block")
                            // 禁用浏览器内容嗅探
                            .setHeader("X-Content-Type-Options", "nosniff");
                });
    }

    static boolean matchesIp(String rule, String ip) {
        return ClientAccessValidator.matchesIp(rule, ip);
    }

}

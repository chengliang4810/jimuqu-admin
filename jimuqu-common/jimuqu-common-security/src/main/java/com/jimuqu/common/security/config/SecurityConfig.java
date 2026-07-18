package com.jimuqu.common.security.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.solon.integration.SaTokenInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.jimuqu.common.security.handler.SecurityExceptionHandler;
import com.jimuqu.common.security.properties.SecurityProperties;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.satoken.utils.LoginHelper;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.List;
import java.util.Objects;

@Configuration
public class SecurityConfig {

    private static final String CLIENT_RULE_SEPARATOR_REGEX = "[,;\\r\\n]+";

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
                        validateClientAccessRules(Context.current());
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

    static void validateClientAccessRules(Context ctx) {
        String clientId = extra(LoginHelper.CLIENT_KEY);
        if (!Objects.equals(clientId, ctx.header(LoginHelper.CLIENT_KEY))
                && !Objects.equals(clientId, ctx.param(LoginHelper.CLIENT_KEY))) {
            throw NotLoginException.newInstance(StpUtil.getLoginType(), "-100",
                    "客户端ID与Token不匹配", StpUtil.getTokenValue());
        }

        String accessPath = extra(LoginHelper.CLIENT_ACCESS_PATH_KEY);
        if (StringUtil.isNotBlank(accessPath)) {
            List<String> paths = StringUtil.str2List(accessPath, CLIENT_RULE_SEPARATOR_REGEX, true, true);
            if (!StringUtil.matches(ctx.path(), paths)) {
                throw new NotPermissionException("当前客户端未授权访问该接口路径");
            }
        }

        String ipWhitelist = extra(LoginHelper.CLIENT_IP_WHITELIST_KEY);
        if (StringUtil.isNotBlank(ipWhitelist)) {
            List<String> rules = StringUtil.str2List(ipWhitelist, CLIENT_RULE_SEPARATOR_REGEX, true, true);
            if (rules.stream().noneMatch(rule -> matchesIp(rule, ctx.realIp()))) {
                throw new NotPermissionException("当前客户端IP不在白名单内");
            }
        }
    }

    private static String extra(String key) {
        Object value = StpUtil.getTokenSession().get(key);
        return value == null ? null : value.toString();
    }

    static boolean matchesIp(String rule, String ip) {
        if (StringUtil.isBlank(rule) || StringUtil.isBlank(ip)) {
            return false;
        }
        String value = rule.trim();
        if (value.equals(ip)) {
            return true;
        }
        if (value.contains("/")) {
            try {
                String[] parts = value.split("/", -1);
                if (parts.length != 2) {
                    return false;
                }
                byte[] network = InetAddress.getByName(parts[0]).getAddress();
                byte[] address = InetAddress.getByName(ip).getAddress();
                int bits = Integer.parseInt(parts[1]);
                int max = network.length * 8;
                if (network.length != address.length || bits < 0 || bits > max) {
                    return false;
                }
                BigInteger mask = bits == 0 ? BigInteger.ZERO
                        : BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE).shiftLeft(max - bits);
                return new BigInteger(1, network).and(mask).equals(new BigInteger(1, address).and(mask));
            } catch (Exception ignored) {
                return false;
            }
        }
        String regex = value.replace(".", "\\.").replace("*", ".*").replace("?", ".");
        return ip.matches(regex);
    }

}

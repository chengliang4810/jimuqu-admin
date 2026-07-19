package com.jimuqu.common.satoken.utils;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.jimuqu.common.core.utils.StringUtil;
import org.noear.solon.core.handle.Context;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.List;
import java.util.Objects;

/** 统一校验 HTTP 与 WebSocket 的客户端绑定、访问路径和来源 IP。 */
public final class ClientAccessValidator {

    private static final String CLIENT_RULE_SEPARATOR_REGEX = "[,;\\r\\n]+";

    private ClientAccessValidator() {
    }

    public static void validateCurrent(Context ctx) {
        validate(StpUtil.getTokenValue(), ctx.path(), ctx.realIp(),
                ctx.header(LoginHelper.CLIENT_KEY), ctx.param(LoginHelper.CLIENT_KEY));
    }

    public static void validateToken(String token, String clientId, String path, String ip) {
        validate(token, path, ip, clientId);
    }

    private static void validate(String token, String path, String ip, String... requestClientIds) {
        String clientId = extra(token, LoginHelper.CLIENT_KEY);
        boolean clientMatches = java.util.Arrays.stream(requestClientIds)
                .anyMatch(requestClientId -> Objects.equals(clientId, requestClientId));
        if (!clientMatches) {
            throw NotLoginException.newInstance(StpUtil.getLoginType(), "-100",
                    "客户端ID与Token不匹配", token);
        }

        String accessPath = extra(token, LoginHelper.CLIENT_ACCESS_PATH_KEY);
        if (StringUtil.isNotBlank(accessPath)) {
            List<String> paths = StringUtil.str2List(accessPath, CLIENT_RULE_SEPARATOR_REGEX, true, true);
            if (!StringUtil.matches(path, paths)) {
                throw new NotPermissionException("当前客户端未授权访问该接口路径");
            }
        }

        String ipWhitelist = extra(token, LoginHelper.CLIENT_IP_WHITELIST_KEY);
        if (StringUtil.isNotBlank(ipWhitelist)) {
            List<String> rules = StringUtil.str2List(ipWhitelist, CLIENT_RULE_SEPARATOR_REGEX, true, true);
            if (rules.stream().noneMatch(rule -> matchesIp(rule, ip))) {
                throw new NotPermissionException("当前客户端IP不在白名单内");
            }
        }
    }

    private static String extra(String token, String key) {
        Object value = null;
        try {
            value = StpUtil.stpLogic.getExtra(token, key);
        } catch (RuntimeException ignored) {
            // 兼容迁移前不支持 extra 的普通 Token。
        }
        if (value == null) {
            SaSession session = StpUtil.getTokenSessionByToken(token);
            value = session == null ? null : session.get(key);
        }
        return value == null ? null : value.toString();
    }

    public static boolean matchesIp(String rule, String ip) {
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

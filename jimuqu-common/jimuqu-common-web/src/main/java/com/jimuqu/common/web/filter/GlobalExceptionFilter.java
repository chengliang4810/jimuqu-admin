package com.jimuqu.common.web.filter;

import cn.hutool.v7.core.util.RandomUtil;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.exception.auth.AuthException;
import com.jimuqu.common.core.exception.base.BaseException;
import com.jimuqu.common.core.utils.ip.AddressUtil;
import com.jimuqu.common.web.validation.ValidationMessageResolver;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.exception.StatusException;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;
import org.noear.solon.validation.ValidatorException;
import org.noear.snack4.json.JsonParseException;

import java.io.IOException;

/**
 * 全局异常筛选器
 *
 * @author chengliang
 * @since 2024/02/26
 */
@Slf4j
@Component(index = 1)
public class GlobalExceptionFilter implements Filter {

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        try {
            chain.doFilter(ctx);

            if (!ctx.getHandled()) {
                drainRequestBody(ctx);
                ctx.status(200);
                if (hasRouteForAnotherMethod(ctx.method(), ctx.path())) {
                    ctx.render(R.fail(405, "Method Not Allowed: " + ctx.method() + " " + ctx.path()));
                } else {
                    ctx.render(R.fail(404, "请求地址不存在"));
                }
            }
        }
        // 参数验证异常
        catch (ValidatorException e) {
            ctx.status(200);
            ctx.render(R.fail(ValidationMessageResolver.errorCode(e), ValidationMessageResolver.resolve(e,
                    ctx.header("Content-Language"), ctx.header("Accept-Language"))));
        }
        // 权限异常
        catch (AuthException e) {
            drainRequestBody(ctx);
            String realIp = ctx.realIp();
            // 权限认证异常
            log.warn("权限异常: {}, 请求路径: {}, 请求地址: {}, 请求IP: {}", e.getMessage(), ctx.path(), AddressUtil.getRealAddressByIP(realIp), realIp);
            ctx.status(transportStatus(e.getCode()));
            ctx.render(R.fail(e.getCode(), e.getMessage()));
        }
        // 业务异常
        catch (ServiceException e) {
            log.error(e.getMessage());
            ctx.status(200);
            ctx.render(serviceError(e));
        }
        catch (BaseException e) {
            log.error(e.getMessage());
            ctx.status(200);
            ctx.render(baseError(e));
        }
        catch (JsonParseException e) {
            log.error("请求数据格式错误: {}", e.getMessage());
            ctx.status(200);
            ctx.render(R.fail(400, "请求数据格式错误"));
        }
        catch (StatusException e) {
            drainRequestBody(ctx);
            ctx.status(transportStatus(e.getCode()));
            ctx.render(statusError(e));
        }
        // 其他异常
        catch (Throwable e) {
            if (findCause(e, JsonParseException.class) != null) {
                log.error("请求数据格式错误: {}", e.getMessage());
                ctx.status(200);
                ctx.render(R.fail(400, "请求数据格式错误"));
                return;
            }
            if (isSseDisconnect(e, ctx.path(), Solon.cfg().get("sse.path", "/resource/message"))) {
                log.debug("SSE 客户端已断开: {}", ctx.path());
                return;
            }
            String exceptionName = e.getClass().getSimpleName();
            if ("NotLoginException".equals(exceptionName)) {
                ctx.status(401);
                ctx.render(R.fail(401, e.getMessage()));
                return;
            }
            if ("NotPermissionException".equals(exceptionName) || "NotRoleException".equals(exceptionName)) {
                ctx.status(403);
                ctx.render(R.fail(403, e.getMessage()));
                return;
            }
            String realIp = ctx.realIp();
            String errorId = newErrorId();
            log.error("系统异常: {}, 错误编号: {}, 请求路径: {}, 请求地址: {}, 请求IP: {}",
                    e.getMessage(), errorId, ctx.path(), AddressUtil.getRealAddressByIP(realIp), realIp, e);
            ctx.status(200);
            ctx.render(unexpectedError(e, errorId));
        }
    }

    static R<Void> serviceError(ServiceException exception) {
        Integer code = exception.getCode();
        return R.fail(code == null ? 500 : code, exception.getMessage());
    }

    static R<Void> baseError(BaseException exception) {
        Integer code = exception.getCode();
        return R.fail(code == null ? 500 : code, exception.getMessage());
    }

    static R<Void> statusError(StatusException exception) {
        return switch (exception.getCode()) {
            case 404 -> R.fail(404, "请求地址不存在");
            case 405 -> R.fail(405, exception.getMessage());
            default -> R.fail(exception.getCode(), exception.getMessage());
        };
    }

    static R<Void> unexpectedError(Throwable exception, String errorId) {
        String type = exception instanceof RuntimeException ? "未知" : "系统";
        return R.fail(500, "发生" + type + "异常，请联系管理员 [错误编号: " + errorId + "]");
    }

    static String newErrorId() {
        return RandomUtil.randomNumbers(8);
    }

    static int transportStatus(int code) {
        return code == 401 || code == 403 ? code : 200;
    }

    static <T extends Throwable> T findCause(Throwable exception, Class<T> type) {
        Throwable current = exception;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                break;
            }
            current = cause;
        }
        return null;
    }

    static boolean isSseDisconnect(Throwable exception, String requestPath, String ssePath) {
        return findCause(exception, IOException.class) != null
                && requestPath != null
                && ssePath != null
                && (requestPath.equals(ssePath) || requestPath.startsWith(ssePath + "/"));
    }

    static boolean hasRouteForAnotherMethod(String requestMethod, String requestPath) {
        return Solon.app().router().findAll().stream()
                .filter(route -> !route.method().name().equalsIgnoreCase(requestMethod))
                .anyMatch(route -> route.matches(route.method(), requestPath));
    }

    private void drainRequestBody(Context ctx) {
        try {
            ctx.body();
        } catch (Exception e) {
            log.debug("读取未处理请求体失败: {}", e.getMessage());
        }
    }

}

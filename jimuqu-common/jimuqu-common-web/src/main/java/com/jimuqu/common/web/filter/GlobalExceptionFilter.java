package com.jimuqu.common.web.filter;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.auth.AuthException;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.ip.AddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;
import org.noear.solon.validation.ValidatorException;

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
                ctx.render(R.fail(404, "资源不存在"));
            }
        }
        // 参数验证异常
        catch (ValidatorException e) {
            ctx.status(400);
            ctx.render(R.fail(400, e.getMessage()));
        }
        // 权限异常
        catch (AuthException e) {
            drainRequestBody(ctx);
            String realIp = ctx.realIp();
            // 权限认证异常
            log.warn("权限异常: {}, 请求路径: {}, 请求地址: {}, 请求IP: {}", e.getMessage(), ctx.path(), AddressUtil.getRealAddressByIP(realIp), realIp);
            // 设置响应状态码为 401， 如果全部返回200 则不需要下面这行
            ctx.status(e.getCode());
            ctx.render(R.fail(e.getCode(), e.getMessage()));
        }
        // 业务异常
        catch (ServiceException e) {
            log.error(e.getMessage());
            ctx.render(serviceError(e));
        }
        // 其他异常
        catch (Throwable e) {
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
            log.error("系统异常: {}, 请求路径: {}, 请求地址: {}, 请求IP: {}", e.getMessage(), ctx.path(), AddressUtil.getRealAddressByIP(realIp), realIp, e);
            ctx.render(R.fail(500, "发生未知异常，请联系管理员"));
        }
    }

    static R<Void> serviceError(ServiceException exception) {
        Integer code = exception.getCode();
        return R.fail(code == null ? 500 : code, exception.getMessage());
    }

    private void drainRequestBody(Context ctx) {
        try {
            ctx.body();
        } catch (Exception e) {
            log.debug("读取未授权请求体失败: {}", e.getMessage());
        }
    }

}

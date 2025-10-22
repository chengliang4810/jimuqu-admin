package com.jimuqu.common.web.filter;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.auth.AuthException;
import com.jimuqu.common.core.utils.ip.AddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
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
            ctx.render(R.fail(e.getCode(), e.getMessage()));
        }
        // 权限异常
        catch (AuthException e) {
            // 权限认证异常
            log.warn("权限异常: {}, 请求路径: {}, 请求IP: {} ", e.getMessage(), ctx.path(), AddressUtil.getRealAddressByIP(ctx.realIp()));
            // 设置响应状态码为 401， 如果全部返回200 则不需要下面这行
            ctx.status(e.getCode());
            ctx.render(R.fail(e.getCode(), e.getMessage()));
        }
        // 其他异常
        catch (Throwable e) {
            log.error(e.getMessage());
            if ("dev".equals(Solon.cfg().getProperty("solon.env"))) {
                e.printStackTrace();
            }
            ctx.render(R.fail(500, e.getMessage()));
        }
    }

}

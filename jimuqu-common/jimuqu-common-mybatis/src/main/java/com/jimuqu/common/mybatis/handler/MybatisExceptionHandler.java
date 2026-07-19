package com.jimuqu.common.mybatis.handler;

import com.jimuqu.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;

import java.sql.SQLException;
import java.util.Locale;

/**
 * Mybatis/Xbatis 异常处理器。
 *
 * @author Lion Li,chengliang4810
 */
@Slf4j
@Component(index = 2)
public class MybatisExceptionHandler implements Filter {

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        try {
            chain.doFilter(ctx);
        } catch (Throwable exception) {
            R<Void> response = resolve(exception);
            if (response == null) {
                throw exception;
            }
            log.error("请求地址'{}',数据库访问异常: {}", ctx.path(), exception.getMessage(), exception);
            ctx.setHandled(true);
            ctx.status(200);
            ctx.render(response);
        }
    }

    static R<Void> resolve(Throwable exception) {
        PersistenceException persistenceException = findCause(exception, PersistenceException.class);
        if (persistenceException != null && hasCauseNamed(persistenceException, "NotLoginException")) {
            return R.fail(401, "认证失败，无法访问系统资源");
        }
        SQLException sqlException = findCause(exception, SQLException.class);
        if (isDuplicateKey(sqlException)) {
            return R.fail(409, "数据库中已存在该记录，请联系管理员确认");
        }
        if (hasCauseNamed(exception, "CannotFindDataSourceException")) {
            return R.fail(500, "未找到数据源，请联系管理员确认");
        }
        if (sqlException != null && sqlException.getSQLState() != null
                && sqlException.getSQLState().startsWith("08")) {
            return R.fail(500, "数据库连接异常，请联系管理员确认");
        }
        if (persistenceException != null) {
            String message = exception.getMessage();
            return R.fail(500, message == null || message.isBlank()
                    ? "数据库访问异常，请联系管理员确认"
                    : message);
        }
        return null;
    }

    private static boolean isDuplicateKey(SQLException exception) {
        if (exception == null) {
            return false;
        }
        String state = exception.getSQLState();
        String message = String.valueOf(exception.getMessage()).toLowerCase(Locale.ROOT);
        return exception.getErrorCode() == 1062
                || "23505".equals(state)
                || message.contains("duplicate entry")
                || message.contains("unique constraint failed")
                || message.contains("sqlite_constraint_unique");
    }

    private static boolean hasCauseNamed(Throwable exception, String simpleName) {
        Throwable current = exception;
        while (current != null) {
            if (simpleName.equals(current.getClass().getSimpleName())) {
                return true;
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                break;
            }
            current = cause;
        }
        return false;
    }

    private static <T extends Throwable> T findCause(Throwable exception, Class<T> type) {
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

}

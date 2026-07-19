package com.jimuqu.common.web.core;

import cn.hutool.v7.core.util.RandomUtil;
import com.jimuqu.common.core.domain.PageResult;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.exception.base.BaseException;
import com.jimuqu.common.core.utils.ip.AddressUtil;
import com.jimuqu.common.web.validation.ValidationMessageResolver;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.core.handle.*;
import org.noear.solon.validation.ValidatorException;
import org.noear.solon.validation.annotation.Valid;

import java.io.File;

/**
 * Base控制器
 * 统一处理响应格式
 * 提供通用方法
 * 如果想自定义相应格式参考下方render内容进行调整与替换
 *
 * @author chengliang
 * @since 2024/06/23
 */
@Valid
@Slf4j
public class BaseController implements Render {

    /**
     * 响应返回结果
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected R<Void> toAjax(int rows) {
        return rows > 0 ? R.ok() : R.fail();
    }

    /**
     * 响应返回结果
     *
     * @param result 结果
     * @return 操作结果
     */
    protected R<Void> toAjax(boolean result) {
        return result ? R.ok() : R.fail();
    }

    /**
     * 通过BaseController 统一处理响应格式
     *
     * @param obj 数据
     * @param ctx 上下文
     * @throws Throwable
     */
    @Override
    public void render(Object obj, Context ctx) throws Throwable {

        if (obj == null) {
            ctx.render(R.ok());
            return;
        }

        if (obj instanceof String) {
            //普通字符串，封装result 返回
            ctx.render(normalizeResponse(obj));
        }
        // 模型视图
        else if (obj instanceof ModelAndView) {
            ctx.render(obj);
        }
        // 文件下载
        else if (obj instanceof DownloadedFile || obj instanceof File) {
            //文件下载
            ctx.render(obj);
        }
        // 其他
        else {
            //此处是重点，把一些特别的类型进行标准化转换
            if (obj instanceof Throwable err) {
                if (obj instanceof ServiceException exception) {
                    Integer code = exception.getCode();
                    obj = R.fail(code == null ? 500 : code, exception.getMessage());
                } else if (obj instanceof BaseException exception) {
                    obj = baseError(exception);
                } else if (obj instanceof ValidatorException validatorException) {
                    obj = validationError(validatorException,
                            ctx.header("Content-Language"), ctx.header("Accept-Language"));
                } else {
                    String realIp = ctx.realIp();
                    String errorId = RandomUtil.randomNumbers(8);
                    log.error("系统异常: {}, 错误编号: {}, 请求路径: {}, 请求地址: {}, 请求IP: {}",
                            err.getMessage(), errorId, ctx.path(), AddressUtil.getRealAddressByIP(realIp), realIp, err);
                    obj = unexpectedError(err, errorId);
                }
            }

            ctx.render(normalizeResponse(obj));
            //或者调用特定接口直接输出：ctx.outputAsJson(JSON.toJson(obj));
        }
    }

    /**
     * 将内部分页对象统一收敛为公开的 rows/total 契约，再套入标准响应。
     */
    static R<?> normalizeResponse(Object value) {
        if (value instanceof R<?> response) {
            Object normalizedData = normalizeData(response.getData());
            if (normalizedData == response.getData()) {
                return response;
            }
            R<Object> normalized = new R<>();
            normalized.setCode(response.getCode());
            normalized.setMsg(response.getMsg());
            normalized.setData(normalizedData);
            return normalized;
        }
        return R.ok(normalizeData(value));
    }

    private static Object normalizeData(Object value) {
        if (value instanceof PageResult<?> page) {
            return new PageResult<>(page.getRows(), page.getTotal());
        }
        return value;
    }

    static R<Void> validationError(ValidatorException exception, String acceptLanguage) {
        return R.fail(ValidationMessageResolver.errorCode(exception),
                ValidationMessageResolver.resolve(exception, acceptLanguage));
    }

    static R<Void> validationError(ValidatorException exception,
                                   String contentLanguage, String acceptLanguage) {
        return R.fail(ValidationMessageResolver.errorCode(exception),
                ValidationMessageResolver.resolve(exception, contentLanguage, acceptLanguage));
    }

    static R<Void> baseError(BaseException exception) {
        Integer code = exception.getCode();
        return R.fail(code == null ? 500 : code, exception.getMessage());
    }

    static R<Void> unexpectedError(Throwable exception, String errorId) {
        String type = exception instanceof RuntimeException ? "未知" : "系统";
        return R.fail(500, "发生" + type + "异常，请联系管理员 [错误编号: " + errorId + "]");
    }
}

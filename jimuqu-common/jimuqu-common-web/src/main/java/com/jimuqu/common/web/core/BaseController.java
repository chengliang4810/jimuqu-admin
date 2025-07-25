package com.jimuqu.common.web.core;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.ServiceException;
import cn.hutool.v7.core.text.StrUtil;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.core.handle.ModelAndView;
import org.noear.solon.core.handle.Render;
import org.noear.solon.validation.annotation.NotBlacklist;
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
@NotBlacklist
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
            ctx.render(R.ok(obj));
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
                err.printStackTrace();
                if (obj instanceof ServiceException exception) {
                    obj = R.fail(exception.getCode(), exception.getMessage());
                } else {
                    // 非手动校验或抛出的ServiceException
                    obj = R.fail(StrUtil.format("服务端异常, 请联系管理员, 异常信息: [{}]", err.getMessage()));
                }
            } else if (!(obj instanceof R<?>)) {
                //非Result结构构建为Result
                obj = R.ok(obj);
            }

            ctx.render(obj);
            //或者调用特定接口直接输出：ctx.outputAsJson(JSON.toJson(obj));
        }
    }
}

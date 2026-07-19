package com.jimuqu.common.log.event;

import com.jimuqu.common.satoken.utils.LoginHelper;
import lombok.Data;
import org.noear.solon.core.handle.Context;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录事件
 *
 * @author Lion Li,chengliang4810
 */

@Data
public class LogininforEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public LogininforEvent() {
        try {
            Context context = Context.current();
            clientId = context.header(LoginHelper.CLIENT_KEY);
            ipaddr = context.realIp();
            userAgent = context.header("User-Agent");
        } catch (RuntimeException ignored) {
            // 非 HTTP 场景允许发布登录事件，服务层仍会保存基础日志字段。
        }
    }

    /**
     * 用户账号
     */
    private String username;

    /**
     * 登录状态 0成功 1失败
     */
    private String status;

    /**
     * 提示消息
     */
    private String message;

    /** 请求使用的客户端 ID。 */
    private String clientId;

    /** 请求来源 IP。 */
    private String ipaddr;

    /** 请求 User-Agent。 */
    private String userAgent;

    /**
     * 其他参数
     */
    private Object[] args;

}

package com.jimuqu.auth.service.impl;

import com.jimuqu.auth.domain.vo.LoginVo;
import com.jimuqu.auth.service.AuthStrategy;
import com.jimuqu.auth.service.AuthStrategyService;
import com.jimuqu.common.core.domain.model.XcxLoginBody;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.system.domain.SysClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.validation.ValidUtils;

/**
 * 小程序登录策略。
 */
@Component("xcx" + AuthStrategy.BASE_NAME)
public class XcxAuthStrategy implements AuthStrategyService {

    @Override
    public LoginVo login(String body, SysClient client) {
        XcxLoginBody loginBody = JsonUtil.toObject(body, XcxLoginBody.class);
        ValidUtils.validateEntity(loginBody);
        throw new ServiceException("小程序登录未启用，请先配置小程序身份服务");
    }
}

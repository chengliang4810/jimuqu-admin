package com.jimuqu.auth.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.jimuqu.auth.domain.vo.LoginVo;
import com.jimuqu.auth.service.AuthStrategy;
import com.jimuqu.auth.service.SysLoginService;
import com.jimuqu.auth.service.SysRegisterService;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.domain.model.LoginBody;
import com.jimuqu.common.core.domain.model.RegisterBody;
import com.jimuqu.common.core.domain.model.SocialLoginBody;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.social.config.properties.SocialLoginConfigProperties;
import com.jimuqu.common.social.config.properties.SocialProperties;
import com.jimuqu.common.social.utils.SocialUtils;
import com.jimuqu.system.domain.SysClient;
import com.jimuqu.system.service.SysClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import cn.hutool.v7.core.util.ObjUtil;
import org.noear.solon.annotation.*;
import org.noear.solon.validation.ValidUtils;

/**
 * 认证
 *
 * @author Lion Li,chengliang4810
 */
@Slf4j
@SaIgnore
@Controller
@Mapping("/auth" )
@RequiredArgsConstructor
public class AuthController {

    private final SysLoginService loginService;
    private final SysClientService clientService;
    private final SocialProperties socialProperties;
    private final SysRegisterService registerService;

    /**
     * 登录方法
     *
     * @param body 登录信息
     * @return 结果
     */
    @Post
    @Mapping("/login" )
    public R<LoginVo> login(@Body String body) {
        LoginBody loginBody = JsonUtil.parseObject(body, LoginBody.class);
        ValidUtils.validateEntity(loginBody);
        // 授权类型和客户端id
        String clientId = loginBody.getClientId();
        String grantType = loginBody.getGrantType();
        SysClient client = clientService.queryByClientId(clientId);
        // 查询不到 client 或 client 内不包含 grantType
        if (ObjUtil.isNull(client) || !StringUtil.contains(client.getGrantType(), grantType)) {
            log.info("客户端id: {} 认证类型：{} 异常!.", clientId, grantType);
            return R.fail("认证权限类型错误" );
        } else if (!UserConstants.NORMAL.equals(client.getStatus())) {
            return R.fail("认证权限类型已禁用" );
        }
        // 登录
        LoginVo loginVo = AuthStrategy.login(body, client, grantType);

//        Long userId = LoginHelper.getUserId();
//        scheduledExecutorService.schedule(() -> {
//            WebSocketUtils.sendMessage(userId, "欢迎登录RuoYi-Vue-Plus后台管理系统");
//        }, 3, TimeUnit.SECONDS);
        return R.ok(loginVo);
    }

    /**
     * 第三方登录请求
     *
     * @param source 登录来源
     * @return 结果
     */
    @Get
    @Mapping("/binding/{source}" )
    public R<String> authBinding(String source) {
        SocialLoginConfigProperties obj = socialProperties.getType().get(source);
        if (ObjUtil.isNull(obj)) {
            return R.fail(source + "平台账号暂不支持" );
        }
        AuthRequest authRequest = SocialUtils.getAuthRequest(source, socialProperties);
        String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
        return R.ok("操作成功", authorizeUrl);
    }

    /**
     * 第三方登录回调业务处理 绑定授权
     *
     * @param loginBody 请求体
     * @return 结果
     */
    @Post
    @Mapping("/social/callback" )
    public R<Void> socialCallback(SocialLoginBody loginBody) {
        // 获取第三方登录信息
        AuthResponse<AuthUser> response = SocialUtils.loginAuth(
                loginBody.getSource(), loginBody.getSocialCode(),
                loginBody.getSocialState(), socialProperties);
        AuthUser authUserData = response.getData();
        // 判断授权响应是否成功
        if (!response.ok()) {
            return R.fail(response.getMsg());
        }
        // loginService.socialRegister(authUserData);
        return R.ok();
    }

    /**
     * 取消授权
     *
     * @param socialId socialId
     */
    @Delete
    @Mapping(value = "/unlock/{socialId}" )
    public R<Void> unlockSocial(Long socialId) {
        // Boolean rows = socialUserService.deleteWithValidById(socialId);
        // return rows ? R.ok() : R.fail("取消授权失败" );
        return R.ok();
    }

    /**
     * 退出登录
     */
    @Post
    @Mapping("/logout" )
    public R<Void> logout() {
        loginService.logout();
        return R.ok("退出成功" );
    }

    /**
     * 用户注册
     */
    @Post
    @Mapping("/register" )
    public R<Void> register(RegisterBody user) {
//        if (!configService.selectRegisterEnabled()) {
//            return R.fail("当前系统没有开启注册功能！" );
//        }
        registerService.register(user);
        return R.ok();
    }

}

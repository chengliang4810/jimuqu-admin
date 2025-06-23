package com.jimuqu.system.controller;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.xbatis.page.PageUtil;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.vo.SysUserOnlineVo;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;

import java.util.List;

/**
 * 在线用户
 */
@Post
@Controller
@Mapping("/system/user/online")
public class SysUserOnlineController extends BaseController {

    /**
     * 在线用户列表
     * <p>
     * SaToken 文档
     * https://sa-token.cc/doc.html#/up/search-session?id=_2%e3%80%81%e5%85%a8%e9%83%a8%e4%bc%9a%e8%af%9d%e6%a3%80%e7%b4%a2
     * <p>
     * SaToken 官方说明
     * <p>
     * 单机模式下：百万会话取出10条 Token 平均耗时 0.255s。
     * Redis模式下：百万会话取出10条 Token 平均耗时 3.322s。
     * <p>
     * 因此 在线用户没有百万会话没必要更换方式
     *
     * @return 用户在线列表
     */
    @Get
    @Mapping("/list")
    public Page<SysUserOnlineVo> list(PageQuery pageQuery) {

        int offset = PageUtil.getOffset(pageQuery.getCurrentPage(), pageQuery.getPageSize());
        //  此时账号A在 电脑、手机、平板 依次登录（共3次登录），账号B在 电脑、手机 依次登录（共2次登录），那么：
        //  StpUtil.searchTokenValue 将返回一共 5 个Token。
        //  StpUtil.searchSessionId 将返回一共 2 个 SessionId。
        // 请根据自己的需要修改调整

        // 获取已登录的Token
        List<String> tokenList = StpUtil.searchTokenSessionId("", offset, pageQuery.getPageSize(), false);
        List<SysUserOnlineVo> userOnlineVoList = tokenList.stream().map(token -> {
            // 获取token
            SaSession session =  StpUtil.getSessionBySessionId(token);
            // 登录用户的信息
            LoginUser loginUser = LoginHelper.getLoginUser(session.getToken());

            SysUserOnlineVo sysUserOnlineVo = new SysUserOnlineVo();
            sysUserOnlineVo.setTokenId(loginUser.getToken());
            sysUserOnlineVo.setDeptName(loginUser.getDeptName());
            sysUserOnlineVo.setNickName(loginUser.getNickname());
            sysUserOnlineVo.setUserName(loginUser.getUsername());
            sysUserOnlineVo.setClientKey(loginUser.getClientKey());
            sysUserOnlineVo.setDeviceType(loginUser.getDeviceType());
            sysUserOnlineVo.setIpaddr(loginUser.getIpaddr());
            sysUserOnlineVo.setLoginLocation(loginUser.getLoginLocation());
            sysUserOnlineVo.setBrowser(loginUser.getBrowser());
            sysUserOnlineVo.setOs(loginUser.getOs());
            sysUserOnlineVo.setLoginTime(loginUser.getLoginTime());
            return sysUserOnlineVo;

        }).toList();


        return Page.of(userOnlineVoList, tokenList.size());
    }

    /**
     * 强退用户
     *
     * @param tokenList 强退的token
     * @return 响应
     */
    @Mapping("/forceLogout/{tokenList}")
    public R<Void> forceLogout(List<String> tokenList) {
        try {
            for (String token : tokenList) {
                StpUtil.kickoutByTokenValue(token);
            }
        } catch (NotLoginException ignored) {
        }
        return R.ok();
    }


}

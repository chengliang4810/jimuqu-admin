package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.vo.SysUserOnlineVo;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.NoRepeatSubmit;

import java.util.List;
import java.util.Objects;

/**
 * 在线用户监控。
 */
@Controller
@Mapping("/monitor/online")
public class SysUserOnlineController extends BaseController {

    /**
     * 管理员查看全部在线会话。
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("monitor:online:list")
    public Page<SysUserOnlineVo> list(String ipaddr, String userName) {
        List<SysUserOnlineVo> rows = listAllOnline().stream()
                .filter(item -> ipaddr == null || ipaddr.isBlank() || Objects.equals(ipaddr, item.getIpaddr()))
                .filter(item -> userName == null || userName.isBlank() || Objects.equals(userName, item.getUserName()))
                .toList();
        return Page.of(rows, rows.size());
    }

    /**
     * 当前账号的在线设备。
     */
    @Get
    @Mapping
    public Page<SysUserOnlineVo> currentUserDevices() {
        String loginId = StpUtil.getLoginIdAsString();
        List<SysUserOnlineVo> rows = StpUtil.getTokenValueListByLoginId(loginId).stream()
                .map(this::toOnlineVo)
                .filter(Objects::nonNull)
                .toList();
        return Page.of(rows, rows.size());
    }

    /**
     * 管理员强制指定会话下线。
     */
    @Delete
    @Mapping("/{tokenId}")
    @SaCheckPermission("monitor:online:forceLogout")
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @NoRepeatSubmit
    public R<Void> forceLogout(String tokenId) {
        kickout(tokenId);
        return R.ok();
    }

    /**
     * 当前账号移除自己的指定设备。
     */
    @Delete
    @Mapping("/myself/{tokenId}")
    @Log(title = "在线设备", businessType = BusinessType.FORCE)
    @NoRepeatSubmit
    public R<Void> removeMyself(String tokenId) {
        if (ownsToken(StpUtil.getTokenValueListByLoginId(StpUtil.getLoginIdAsString()), tokenId)) {
            kickout(tokenId);
        }
        return R.ok();
    }

    static boolean ownsToken(List<String> tokenIds, String tokenId) {
        return tokenIds != null && tokenIds.contains(tokenId);
    }

    private List<SysUserOnlineVo> listAllOnline() {
        return StpUtil.searchTokenValue("", 0, -1, false).stream()
                .map(SysUserOnlineController::extractTokenValue)
                .map(this::toOnlineVo)
                .filter(Objects::nonNull)
                .toList();
    }

    private static String extractTokenValue(String searchResult) {
        int separator = searchResult.lastIndexOf(':');
        return separator < 0 ? searchResult : searchResult.substring(separator + 1);
    }

    private SysUserOnlineVo toOnlineVo(String tokenValue) {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser(tokenValue);
            if (loginUser == null) {
                return null;
            }
            return new SysUserOnlineVo()
                    .setTokenId(tokenValue)
                    .setDeptName(loginUser.getDeptName())
                    .setNickName(loginUser.getNickname())
                    .setUserName(loginUser.getUsername())
                    .setClientKey(loginUser.getClientKey())
                    .setDeviceType(loginUser.getDeviceType())
                    .setIpaddr(loginUser.getIpaddr())
                    .setLoginLocation(loginUser.getLoginLocation())
                    .setBrowser(loginUser.getBrowser())
                    .setOs(loginUser.getOs())
                    .setLoginTime(loginUser.getLoginTime());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void kickout(String tokenId) {
        try {
            StpUtil.kickoutByTokenValue(tokenId);
        } catch (NotLoginException ignored) {
        }
    }
}

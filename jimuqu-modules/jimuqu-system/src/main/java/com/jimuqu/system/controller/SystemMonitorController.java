package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.vo.SystemMonitorVo;
import com.jimuqu.system.service.SystemMonitorService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;

/**
 * 系统监控控制器
 *
 * @author chengliang4810
 * @since 2025-09-05
 */
@Controller
@RequiredArgsConstructor
@Mapping("/system/monitor")
public class SystemMonitorController extends BaseController {

    private final SystemMonitorService systemMonitorService;

    /**
     * 获取系统监控信息
     *
     * @return 系统监控信息
     */
    @Get
    @Mapping("/info")
    @SaCheckPermission("system:monitor:info")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    public SystemMonitorVo getSystemMonitorInfo() {
        return systemMonitorService.getSystemMonitorInfo();
    }
}
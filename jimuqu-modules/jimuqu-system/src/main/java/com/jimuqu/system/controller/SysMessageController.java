package com.jimuqu.system.controller;

import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.vo.SysMessageBoxVo;
import com.jimuqu.system.service.SysMessageService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;

/**
 * 消息盒子。
 */
@Controller
@RequiredArgsConstructor
@Mapping("/resource/message")
public class SysMessageController extends BaseController {

    private final SysMessageService messageService;

    @Get
    @Mapping("/box")
    public SysMessageBoxVo box() {
        return messageService.queryMessageBox(LoginHelper.getUserId());
    }
}

package com.jimuqu.system.controller;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.vo.SysSocialVo;
import com.jimuqu.system.service.SysSocialService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;

import java.util.List;

/**
 * 社会化账号绑定。
 */
@Controller
@RequiredArgsConstructor
@Mapping("/system/social")
public class SysSocialController {

    private final SysSocialService socialService;

    @Get
    @Mapping("/list")
    public R<List<SysSocialVo>> list() {
        return R.ok(socialService.queryListByUserId(LoginHelper.getUserId()));
    }
}

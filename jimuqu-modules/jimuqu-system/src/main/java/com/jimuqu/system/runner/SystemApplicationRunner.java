package com.jimuqu.system.runner;

import com.jimuqu.system.service.SystemSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.bean.LifecycleBean;

/**
 * 初始化 system 模块对应业务数据
 *
 * @author Lion Li,chengliang4810
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemApplicationRunner implements LifecycleBean {

    private final SystemSeedService systemSeedService;

    @Override
    public void start() {
        systemSeedService.initialize();
        log.info("系统基础数据初始化完成");
    }

}

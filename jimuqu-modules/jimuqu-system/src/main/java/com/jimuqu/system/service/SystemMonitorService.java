package com.jimuqu.system.service;

import com.jimuqu.system.domain.vo.SystemMonitorVo;

/**
 * 系统监控服务接口
 *
 * @author chengliang4810
 * @since 2025-09-05
 */
public interface SystemMonitorService {

    /**
     * 获取系统监控信息
     *
     * @return 系统监控信息
     */
    SystemMonitorVo getSystemMonitorInfo();
}
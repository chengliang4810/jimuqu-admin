package com.jimuqu.system.job;

import cn.hutool.v7.core.map.Dict;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.system.domain.SysJob;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 定时任务执行上下文
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
public class SysJobContext {

    @Getter
    private final SysJob job;

    @Getter
    private final String paramJson;

    private final Map<String, Object> params;

    public SysJobContext(SysJob job) {
        this.job = job;
        this.paramJson = job == null ? null : job.getHandlerParam();
        this.params = parseParams(this.paramJson);
    }

    /**
     * 获取参数Map，永远不返回null。
     */
    public Map<String, Object> getParams() {
        return params;
    }

    private Map<String, Object> parseParams(String paramJson) {
        Dict dict = JsonUtil.toMap(paramJson);
        Map<String, Object> map = new HashMap<>();
        if (dict != null) {
            for (String key : dict.keySet()) {
                map.put(key, dict.get(key));
            }
        }
        return map;
    }
}

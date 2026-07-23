package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.redis.utils.RedisUtils;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.vo.CacheListInfoVo;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.redisson.api.Node;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Redis 缓存监控。
 */
@Controller
@Mapping("/monitor/cache")
public class CacheController extends BaseController {

    @Get
    @Mapping
    @SaCheckPermission("monitor:cache:list")
    public CacheListInfoVo getInfo() {
        RedissonClient client = RedisUtils.getClient();
        Node node = client.getNodesGroup().getNodes().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Redis 节点不可用"));
        Map<String, String> rawInfo = node.info(Node.InfoSection.ALL);
        Map<String, String> commandInfo = node.info(Node.InfoSection.COMMANDSTATS);
        Properties info = new Properties();
        info.putAll(rawInfo);
        List<Map<String, String>> commandStats = new ArrayList<>();
        commandInfo.forEach((key, value) -> {
            Map<String, String> item = new HashMap<>(2);
            item.put("name", key.startsWith("cmdstat_") ? key.substring(8) : key);
            item.put("value", extractCalls(value));
            commandStats.add(item);
        });
        CacheListInfoVo result = new CacheListInfoVo();
        result.setInfo(info);
        result.setDbSize(client.getKeys().count());
        result.setCommandStats(commandStats);
        return result;
    }

    private String extractCalls(String value) {
        if (value == null) {
            return "0";
        }
        int start = value.indexOf("calls=");
        if (start < 0) {
            return "0";
        }
        start += "calls=".length();
        int end = value.indexOf(',', start);
        return end < 0 ? value.substring(start) : value.substring(start, end);
    }
}

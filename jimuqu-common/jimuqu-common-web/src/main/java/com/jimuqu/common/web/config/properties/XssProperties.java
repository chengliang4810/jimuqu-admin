package com.jimuqu.common.web.config.properties;

import lombok.Data;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * xss过滤 配置属性
 *
 * @author Lion Li,chengliang4810
 */
@Data
@Configuration
@Inject(value = "${xss}", required = false)
public class XssProperties {

    /**
     * 过滤开关
     */
    private boolean enabled = true;

    /**
     * 排除链接（多个用逗号分隔）
     */
    private List<String> excludeUrls = new ArrayList<>();

}

package com.jimuqu.common.social.config.properties;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;


/**
 * Social 配置属性
 *
 * @author thiszhc
 */
@Data
public class SocialProperties {

    /**
     * 是否启用
     */
    private Boolean enabled = false;

    /**
     * 授权类型
     */
    private Map<String, SocialLoginConfigProperties> type = new HashMap<>();

}

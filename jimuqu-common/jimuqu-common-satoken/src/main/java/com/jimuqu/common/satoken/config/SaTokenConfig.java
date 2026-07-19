package com.jimuqu.common.satoken.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import com.jimuqu.common.satoken.core.PrefixedSaTokenDaoForRedisson;
import com.jimuqu.common.satoken.core.SaPermissionImpl;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.cache.redisson.RedissonCacheService;

@Configuration
public class SaTokenConfig {

    /**
     * 创建 Sa-Token JWT 登录逻辑。
     *
     * @return JWT Simple 登录逻辑
     */
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    /**
     * 权限接口实现(使用bean注入方便用户替换)
     */
    @Bean
    public StpInterface stpInterface() {
        return new SaPermissionImpl();
    }

    /**
     * Sa-Token 持久层实现 [ Redisson客户端、Redis存储、snack3序列化 ]
     *
     * @param redissonCacheService solon redis缓存服务
     * @return Sa-Token 持久层实现
     */
    @Bean
    @Condition(onBean = RedissonCacheService.class)
    public SaTokenDao saTokenDaoForRedissonInit(@Inject RedissonCacheService redissonCacheService) {
        String keyHeader = Solon.cfg().get("jimuqu.cache.keyHeader", "");
        return new PrefixedSaTokenDaoForRedisson(redissonCacheService.client(), keyHeader);
    }

}

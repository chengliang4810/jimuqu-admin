package com.jimuqu.common.satoken.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.json.SaJsonTemplateForSnack4;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import com.jimuqu.common.satoken.core.PrefixedSaTokenDaoForRedisson;
import com.jimuqu.common.satoken.core.SaPermissionImpl;
import com.jimuqu.common.satoken.service.PermissionProvider;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.cache.redisson.RedissonCacheService;
import org.noear.snack4.Feature;

@Configuration
public class SaTokenConfig {

    @Bean
    public SaJsonTemplateForSnack4 saJsonTemplate() {
        SaJsonTemplateForSnack4 template = new SaJsonTemplateForSnack4();
        // Map 使用声明的泛型类型，避免具体实现类的元信息丢失嵌套 Long 类型。
        template.options.addFeatures(Feature.Write_NotMapClassName);
        return template;
    }

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
    public StpInterface stpInterface(@Inject PermissionProvider permissionProvider) {
        return new SaPermissionImpl(permissionProvider);
    }

    /**
     * Sa-Token 持久层实现 [ Redisson客户端、Redis存储、snack4序列化 ]
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

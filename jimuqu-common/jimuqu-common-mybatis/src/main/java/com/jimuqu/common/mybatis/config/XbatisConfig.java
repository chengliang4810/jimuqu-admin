package com.jimuqu.common.mybatis.config;

import cn.xbatis.core.XbatisGlobalConfig;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.mybatis.core.mapper.BaseMapper;
import com.jimuqu.common.satoken.utils.LoginHelper;
import cn.hutool.v7.core.text.StrUtil;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

@Configuration
public class XbatisConfig {

    @Bean
    public void init() {
        // 单Mapper
        XbatisGlobalConfig.setSingleMapperClass(BaseMapper.class);

        // 动态值设置 TODO 定义标准接口，实现自动注册
        // 当前登录用户的ID
        XbatisGlobalConfig.setDynamicValue("{CURRENT_USER_ID}", (entity, type) -> {
            if (type == Long.class) {
                return LoginHelper.getUserId();
            } else if (type == String.class) {
                return StrUtil.toStringOrNull(LoginHelper.getUserId());
            }
            throw new ServiceException("{CURRENT_USER_ID} 不支持的字段类型");
        });

        // 当前登录用户的部门Id
        XbatisGlobalConfig.setDynamicValue("{CURRENT_DEPT_ID}", (entity, type) -> {
            if (type == Long.class) {
                return LoginHelper.getDeptId();
            } else if (type == String.class) {
                return StrUtil.toStringOrNull(LoginHelper.getDeptId());
            }
            throw new ServiceException("{CURRENT_DEPT_ID} 不支持的字段类型");
        });
    }

}

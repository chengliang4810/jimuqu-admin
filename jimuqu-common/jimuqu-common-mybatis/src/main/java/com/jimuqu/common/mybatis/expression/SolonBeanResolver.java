package com.jimuqu.common.mybatis.expression;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;

/**
 * Solon Bean解析器
 * <p>
 * 提供Solon框架的Bean查找功能
 *
 * @author chengliang4810
 * @version 1.0
 */
@Slf4j
public class SolonBeanResolver implements SolonExpressionParser.BeanResolver {

    @Override
    public Object getBean(String name) {
        try {
            return Solon.context().getBean(name);
        } catch (Exception e) {
            log.warn("未找到Bean: {}", name, e);
            return null;
        }
    }

    /**
     * 获取指定类型的Bean
     *
     * @param type Bean类型
     * @return Bean实例
     */
    public <T> T getBean(Class<T> type) {
        try {
            return Solon.context().getBean(type);
        } catch (Exception e) {
            log.warn("未找到Bean: {}", type.getSimpleName(), e);
            return null;
        }
    }

}
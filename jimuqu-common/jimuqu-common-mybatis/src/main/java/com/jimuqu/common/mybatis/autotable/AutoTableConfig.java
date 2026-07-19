package com.jimuqu.common.mybatis.autotable;

import com.jimuqu.common.mybatis.autotable.adapter.CustomAutoTableClassScanner;
import com.jimuqu.common.mybatis.autotable.adapter.CustomAutoTableMetadataAdapter;
import org.dromara.autotable.core.AutoTableAnnotationFinder;
import org.dromara.autotable.core.AutoTableClassScanner;
import org.dromara.autotable.core.AutoTableMetadataAdapter;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

/**
 * 自动表配置
 *
 * @author chengliang
 * @date 2025/05/21
 */
@Configuration
public class AutoTableConfig {

    /**
     * 使用 AutoTable 原生注解查找，避免 Solon 深度合并重复索引时重复返回容器元素。
     */
    @Bean
    public AutoTableAnnotationFinder autoTableAnnotationFinder() {
        return new AutoTableAnnotationFinder() { };
    }

    /**
     * 自动表类扫描器
     *
     * @return {@link AutoTableClassScanner }
     */
    @Bean
    public AutoTableClassScanner autoTableClassScanner() {
        return new CustomAutoTableClassScanner();
    }

    /**
     * 自动表配置
     *
     * @return {@link AutoTableMetadataAdapter }
     **/
    @Bean
    public AutoTableMetadataAdapter autoTableMetadataAdapter() {
        return new CustomAutoTableMetadataAdapter();
    }

}

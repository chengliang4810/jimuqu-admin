package org.dromara.autotable.solon.adapter;

import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.solon.exception.DataSourceNotFoundException;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.dynamicds.DynamicDataSource;
import org.noear.solon.data.dynamicds.DynamicDs;
import org.noear.solon.data.dynamicds.DynamicDsKey;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * AutoTable 2.6.1 的 Solon 4 兼容适配器。
 */
@Component
public class SolonDataSourceHandler implements IDataSourceHandler {

    @Override
    public void useDataSource(String name) {
        Map<String, DataSource> sources = Solon.context().getBeansMapOfType(DataSource.class);
        DataSource source = sources.get(name);
        if (source instanceof DynamicDataSource dynamic) {
            source = dynamic.getDefaultTargetDataSource();
        }
        if (source != null) {
            DataSourceManager.setDataSource(source);
            return;
        }

        List<DynamicDataSource> dynamicSources = sources.values().stream()
                .filter(DynamicDataSource.class::isInstance)
                .map(DynamicDataSource.class::cast)
                .toList();
        if (dynamicSources.isEmpty()) {
            throw new DataSourceNotFoundException("未找到数据源");
        }
        DynamicDataSource dynamic = dynamicSources.get(0);
        DataSource target = name == null || name.isBlank()
                ? dynamic.getDefaultTargetDataSource()
                : dynamic.getTargetDataSource(name);
        DataSourceManager.setDataSource(target);
    }

    @Override
    public void clearDataSource(String name) {
        DataSourceManager.cleanDataSource();
    }

    @Override
    public String getDataSourceName(Class<?> entityClass) {
        DynamicDs annotation = entityClass.getAnnotation(DynamicDs.class);
        if (annotation != null) {
            return annotation.value();
        }
        String current = DynamicDsKey.current();
        if (current != null && !current.isBlank()) {
            return current;
        }
        return Solon.context().getWrap(DataSource.class).name();
    }
}

package com.jimuqu.system.autotable;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.callback.AutoTableFinishCallback;
import org.dromara.autotable.core.callback.CreateTableFinishCallback;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.core.initdata.InitDataHandler;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.TableMetadata;
import org.noear.solon.annotation.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 为部署者预先创建的空数据库补充 AutoTable 库级初始化数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmptyDatabaseInitDataCallback implements CreateTableFinishCallback, AutoTableFinishCallback {

    private final SysUserMapper userMapper;
    private final IDataSourceHandler dataSourceHandler;
    private final Set<Class<?>> createdTables = new HashSet<>();
    private String dataSourceName;
    private String databaseDialect;

    @Override
    public void afterCreateTable(String databaseDialect, TableMetadata tableMetadata) {
        createdTables.add(tableMetadata.getEntityClass());
        dataSourceName = DataSourceManager.getDatasourceName();
        this.databaseDialect = databaseDialect;
    }

    @Override
    public void finish(Set<Class<?>> tableClasses) {
        if (tableClasses.isEmpty() || !createdTables.containsAll(tableClasses)) {
            return;
        }

        IStrategy<?, ?> strategy = AutoTableGlobalConfig.instance().getStrategy(databaseDialect);
        if (strategy == null) {
            throw new IllegalStateException("未找到空数据库初始化所需的 AutoTable 方言：" + databaseDialect);
        }

        dataSourceHandler.useDataSource(dataSourceName);
        DataSourceManager.setDatasourceName(dataSourceName);
        IStrategy.setCurrentStrategy(strategy);
        try {
            if (QueryChain.of(userMapper).exists()) {
                return;
            }
            InitDataHandler.initDbData();
            if (!QueryChain.of(userMapper).exists()) {
                throw new IllegalStateException("AutoTable 已创建空数据库表结构，但未写入初始化数据");
            }
            log.info("AutoTable 已为预先创建的空数据库写入初始化数据");
        } finally {
            IStrategy.clean();
            DataSourceManager.cleanDatasourceName();
            dataSourceHandler.clearDataSource(dataSourceName);
        }
    }
}

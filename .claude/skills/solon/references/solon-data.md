# solon-data

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-data`

用途：数据访问、事务、数据源、缓存与 SQL 工具扩展集合。它不是单个 ORM，而是 Solon 数据能力的基础设施层。

## 直接子模块

- `solon-data` — 数据源与事务基础模块。插件入口 `org.noear.solon.data.integration.DataPlugin`，声明在 `solon-data/src/main/resources/META-INF/solon/solon.data.properties`。它在 `enableTransaction()` 开启时注册 `@Transaction` / 旧 `@Tran` 拦截器，并注册 `@Ds` 注入器与数据源自动构建逻辑。
- `solon-cache` — 缓存注解基础模块。插件入口 `org.noear.solon.data.cache.integration.CachePlugin`。注册本地缓存工厂；在 `enableCaching()` 开启时注册 `@Cache`、`@CachePut`、`@CacheRemove` 拦截器。
- `solon-cache-caffeine` — Caffeine 缓存工厂，插件 `org.noear.solon.cache.caffeine.integration.CaffeineCachePlugin`，注册工厂名 `caffeine`。
- `solon-cache-jedis` — Redis/Jedis 缓存工厂，插件 `org.noear.solon.cache.jedis.integration.RedisCachePlugin`，注册工厂名 `redis`、`jedis`。
- `solon-cache-redisson` — Redisson 缓存工厂，插件 `org.noear.solon.cache.redisson.integration.RedissonCachePlugin`。
- `solon-cache-spymemcached` — Memcached 缓存工厂，插件 `org.noear.solon.cache.spymemcached.integration.MemCachePlugin`。
- `solon-data-dynamicds` — 动态数据源切换模块。插件 `org.noear.solon.data.dynamicds.integration.DynamicDsPlugin`，注册 `@DynamicDs` 拦截器。
- `solon-data-shardingds` — ShardingSphere 数据源模块，POM 依赖 `solon-data`、`solon-config-yaml`、`shardingsphere-jdbc-core`。
- `solon-data-sqlutils` — SQLUtils 支持模块。插件 `org.noear.solon.data.sql.integration.SqlUtilsPlugin`，支持通过 `@Inject` 注入 `SqlUtils`。

## 关键源码与 API

- `solon-data/src/main/java/org/noear/solon/data/integration/DataPlugin.java`：事务、`@Ds`、数据源构建入口。
- `solon-data/src/main/java/org/noear/solon/data/integration/DataSourcesBuilder.java`：读取 `solon.dataSources` 自动构建数据源。
- `solon-data/src/main/java/org/noear/solon/data/datasource/DsUtils.java`：支持 `@type`、`type`、`class`、`dataSourceClassName` 指定数据源类型；`untransaction=true` 会包装为 `UntransactionDataSource`。
- `solon-data/src/main/java/org/noear/solon/data/annotation/Ds.java`：`@Ds` 可用于类型、字段、参数。
- `solon-data/src/main/java/org/noear/solon/data/annotation/Transaction.java`：新事务注解，支持 `policy`、`isolation`、`readOnly`、`message`。
- `solon-data/src/main/java/org/noear/solon/data/annotation/Tran.java`：旧事务注解，已废弃，新代码优先用 `@Transaction`。
- `solon-cache/src/main/java/org/noear/solon/data/annotation/Cache.java`、`CachePut.java`、`CacheRemove.java`：缓存注解。
- `solon-data-dynamicds/src/main/java/org/noear/solon/data/dynamicds/DynamicDs.java`：动态数据源注解，可用于类或方法。

## 配置与行为

- 自动数据源配置根：`solon.dataSources`。
- 数据源名以 `!` 结尾时，源码会去掉 `!` 并按 `DataSource.class` 类型注册为默认数据源。
- 数据源属性值支持 `ENC(...)`，会通过 Vault 能力解密。
- 缓存注解拦截器依赖 `context.app().enableCaching()`。
- 事务注解拦截器依赖 `context.app().enableTransaction()`。

## 依赖选择与坑点

- 基础事务/数据源：选 `solon-data`。
- 缓存注解：选 `solon-cache`，再按后端选择 Caffeine/Jedis/Redisson/Memcached。
- 新代码优先使用 `@Transaction`，不要继续使用已废弃的 `@Tran`。
- 只加缓存依赖不代表缓存注解生效；必须确认应用启用了 caching。
- 只加事务依赖不代表事务注解生效；必须确认应用启用了 transaction。
- ShardingSphere 模块依赖较重，并排除部分日志依赖，接入时注意日志冲突和包体积。

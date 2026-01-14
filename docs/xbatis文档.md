# Xbatis 框架文档

## 1. 框架概述

Xbatis 是基于 MyBatis 的高度 ORM 化数据库操作框架，强调"少写 SQL、链路化 DSL、跨数据库兼容"。

### 主要优势
- 多表关联、子查询、链路分页、自动 SQL 优化
- 内置 RETURNING 支持、批量操作、原生函数包装
- 单 Mapper 模式，支持一个 BasicMapper 横跨全部实体
- 多数据库函数库、数据库差异化、动态数据源路由
- 功能完备的注解生态：逻辑删除、多租户、乐观锁、结果映射

### 核心特征
- 极致轻量：对 MyBatis 仅做封装而非侵入式改造
- 高性能：保持接近手写 SQL 的执行效率
- 灵活易用：链式 API 接近自然语言，学习成本低
- 高可用：可覆盖超过 90% 常见 SQL 场景

---

## 2. 核心模块

| 模块 | 典型包路径 | 核心类型 |
| --- | --- | --- |
| 核心 Mapper | `cn.xbatis.core.mybatis.mapper` | `MybatisMapper<T>`, `BasicMapper` |
| 链式 DSL | `cn.xbatis.core.chain` | `QueryChain`, `InsertChain`, `UpdateChain`, `DeleteChain` |
| 全局配置 | `cn.xbatis.core.config` | `XbatisGlobalConfig` |
| 注解体系 | `cn.xbatis.db.annotations` | `@Table`, `@TableId`, `@TableField` 等 |
| 数据库函数 | `db.sql.api.impl.cmd` | `Methods` |

---

## 3. 实体类注解

### @Table（表映射）
```java
@Table
public class SysUser {
    @TableId
    private Integer id;
    private String userName;
}

// 指定表名和大小写规则
@Table(value = "sys_user", databaseCaseRule = DatabaseCaseRule.UPPERCASE)
public class SysUser { }
```

### @TableId（主键配置）
```java
@TableId(type = IdAutoType.AUTO)
private Integer id;

// 支持多数据库差异化配置
@TableId(type = IdAutoType.AUTO, dbType = DbType.MYSQL)
@TableId(type = IdAutoType.SEQUENCE, dbType = DbType.ORACLE)
private Long id;
```

### @TableField（列配置）
```java
// 基本配置
@TableField(value = "user_name", select = true)
private String userName;

// 默认值支持
@TableField(defaultValue = "{NOW}")  // 插入时自动填充当前时间
private LocalDateTime createTime;

// 永不更新
@TableField(neverUpdate = true)
private String createTime;

// 非持久化字段
@TableField(exists = false)
private String tempField;
```

### @LogicDelete（逻辑删除）
```java
@Table
public class SysUser {
    @TableId
    private Integer id;

    @LogicDelete(beforeValue = "0", afterValue = "1")
    private String delFlag;

    @LogicDeleteTime
    private LocalDateTime delTime;
}
```

### @TenantId（多租户）
```java
@Table
public class SysUser {
    @TenantId
    private String tenantId;
}

// 配置租户获取器
TenantContext.registerTenantGetter(() -> {
    return StpUtil.getLoginIdAsString(); // 从 Sa-Token 获取
});
```

### @Version（乐观锁）
```java
@Table
public class SysUser {
    @Version
    private Integer version;
}
```

---

## 4. Mapper 基础能力

### 单 Mapper 模式
```java
// 1. 定义基础 Mapper
public interface MybatisBasicMapper extends BasicMapper { }

// 2. 配置扫描
@MapperScan(basePackageClasses = MybatisBasicMapper.class,
            markerInterface = BasicMapper.class)

// 3. 注册全局单 Mapper
XbatisGlobalConfig.setSingleMapperClass(MybatisBasicMapper.class);

// 4. 使用
@Autowired
private MybatisBasicMapper mapper;

public void demo() {
    mapper.save(new SysUser());
    mapper.deleteById(SysUser.class, 1);

    QueryChain.of(mapper, SysUser.class)
        .eq(SysUser::getId, 1)
        .list();
}
```

---

## 5. 新增数据 (Save)

### Mapper 内置方法
```java
// 单个新增
mapper.save(entity);
mapper.save(entity, true);  // null 值也保存

// 批量新增
mapper.saveBatch(list);
mapper.saveBatch(list, saveFields);  // 指定列保存

// 保存或更新
mapper.saveOrUpdate(entity);
```

### InsertChain（推荐）
```java
InsertChain.of(sysUserMapper)
    .insert(SysUser.class)
    .set(SysUser::getUserName, "test")
    .set(SysUser::getPassword, "123456")
    .execute();

// 批量插入
InsertChain.of(sysUserMapper)
    .insert(SysUser.class)
    .values(Arrays.asList("user1", "user2"))
    .execute();

// INSERT ... SELECT
InsertChain.of(sysUserMapper)
    .insert(SysUser.class)
    .fields(SysUser::getUserName, SysUser::getRoleId)
    .fromSelect(Query.create()
        .select(SysUser2::getUserName, SysUser2::getRoleId)
        .from(SysUser2.class)
    )
    .execute();
```

### 冲突处理（重复键策略）
```java
// 忽略重复
mapper.save(entity, strategy -> {
    strategy.onConflict(action -> action.doNothing());
});

// 重复时更新
mapper.save(entity, strategy -> {
    strategy.onConflict(action -> action.doUpdate(update ->
        update.overwrite(SysUser::getUserName)
    ));
});

// InsertChain 冲突处理
InsertChain.of(sysUserMapper)
    .insert(SysUser.class)
    .values(data)
    .onConflict(action -> action.doUpdate(update ->
        update.overwrite(SysUser::getPassword)
    ))
    .execute();
```

---

## 6. 修改数据 (Update)

### Mapper 内置方法
```java
// 根据主键修改
mapper.update(entity);

// 强制更新指定字段（null 也会更新）
mapper.update(entity, forceUpdateFields);

// 根据 WHERE 批量修改
mapper.update(entity, where -> {
    where.gt(SysUser::getId, 100);
});
```

### UpdateChain（推荐）
```java
// 基本更新
UpdateChain.of(sysUserMapper)
    .update(SysUser.class)
    .set(SysUser::getUserName, "new name")
    .eq(SysUser::getId, 1)
    .execute();

// 列自增
UpdateChain.of(sysUserMapper)
    .set(SysUser::getVersion, c -> c.plus(1))
    .eq(SysUser::getId, 1)
    .execute();

// 带返回值
SysUser user = UpdateChain.of(sysUserMapper)
    .update(SysUser.class)
    .set(SysUser::getUserName, "new name")
    .eq(SysUser::getId, 1)
    .returning(SysUser.class)
    .returnType(SysUser.class)
    .executeAndReturning();
```

---

## 7. 删除数据 (Delete)

### Mapper 内置方法
```java
// 根据 ID 删除
mapper.deleteById(1);
mapper.deleteByIds(Arrays.asList(1, 2, 3));

// 根据实体删除
mapper.delete(entity);

// 根据 WHERE 删除
mapper.delete(where -> {
    where.gt(SysUser::getId, 100);
});
```

### DeleteChain（推荐）
```java
// 基本删除
DeleteChain.of(sysUserMapper)
    .eq(SysUser::getId, 1)
    .execute();

// 带返回值
List<SysUser> removed = DeleteChain.of(sysUserMapper)
    .in(SysUser::getId, 1, 2)
    .returning(SysUser.class)
    .returnType(SysUser.class)
    .executeAndReturningList();
```

---

## 8. 查询数据 (QueryChain)

### 基本查询
```java
// 单个查询（无结果返回 null）
SysUser user = QueryChain.of(sysUserMapper)
    .eq(SysUser::getId, 1)
    .get();

// 列表查询（无结果返回空列表）
List<SysUser> list = QueryChain.of(sysUserMapper)
    .like(SysUser::getUserName, "test")
    .list();

// 计数
long count = QueryChain.of(sysUserMapper)
    .eq(SysUser::getDelFlag, "0")
    .count();

// 存在性检查
boolean exists = QueryChain.of(sysUserMapper)
    .eq(SysUser::getId, 1)
    .exists();
```

### 条件构建
```java
// 等值/不等值
.eq(SysUser::getId, 1)
.ne(SysUser::getDelFlag, "1")

// 比较
.gt(SysUser::getId, 10)          // 大于
.ge(SysUser::getId, 10)          // 大于等于
.lt(SysUser::getId, 100)         // 小于
.le(SysUser::getId, 100)         // 小于等于

// 范围
.between(SysUser::getId, 1, 100)
.in(SysUser::getId, Arrays.asList(1, 2, 3))
.notIn(SysUser::getStatus, Arrays.asList(0, 1))

// 模糊查询
.like(SysUser::getUserName, "test")
.notLike(SysUser::getUserName, "admin")

// 空值判断
.isNull(SysUser::getDeleteTime)
.isNotNull(SysUser::getUpdateTime)

// 字符串空值判断
.empty(SysUser::getUserName)
.notEmpty(SysUser::getUserName)
```

### 搜索优化（忽略空值）
```java
// 方式1：使用 forSearch
SysUser user = QueryChain.of(sysUserMapper)
    .forSearch(true)  // 忽略 null、空字符串、自动 trim
    .eq(SysUser::getId, id)
    .like(SysUser::getUserName, userName)
    .get();

// 方式2：单独配置
QueryChain.of(sysUserMapper)
    .ignoreNullValueInCondition(true)
    .ignoreEmptyInCondition(true)
    .trimStringInCondition(true)

// 方式3：条件级忽略
QueryChain.of(sysUserMapper)
    .eq(SysUser::getId, id, Objects::nonNull)
    .like(SysUser::getUserName, userName, StringUtils::isNotBlank)
    .get();
```

### 分页查询
```java
// 基本分页
Pager<SysUser> pager = QueryChain.of(sysUserMapper)
    .eq(SysUser::getDelFlag, "0")
    .orderBy(SysUser::getId, false)
    .paging(Pager.of(1, 10));

// 获取结果
List<SysUser> list = pager.getResults();
long total = pager.getTotal();
int totalPage = pager.getTotalPage();

// 不执行 count（提升性能）
Pager<SysUser> pager = QueryChain.of(sysUserMapper)
    .paging(Pager.of(1, 10).setExecuteCount(false));
```

### 多表查询
```java
// JOIN 查询
SysUserRoleVo vo = QueryChain.of(sysUserMapper)
    .select(SysUser.class, SysRole.class)
    .from(SysUser.class)
    .join(SysUser::getRoleId, SysRole::getId)
    .eq(SysUser::getId, 1)
    .returnType(SysUserRoleVo.class)
    .get();

// LEFT JOIN
QueryChain.of(sysUserMapper)
    .select(SysUser.class, SysRole.class)
    .from(SysUser.class)
    .leftJoin(SysUser::getRoleId, SysRole::getId)
    .list();
```

### EXISTS 子查询
```java
// 单条件 EXISTS
int count = QueryChain.of(sysUserMapper)
    .exists(SysUser::getRoleId, SysRole::getId)
    .count();

// 多条件 EXISTS
int count = QueryChain.of(sysUserMapper)
    .exists(SysUser::getRoleId, SysRole::getId, (query, existsQuery) -> {
        existsQuery.eq(SysRole::getStatus, "1");
    })
    .count();

// NOT EXISTS
int count = QueryChain.of(sysUserMapper)
    .notExists(SysUser::getRoleId, SysRole::getId)
    .count();
```

### 聚合查询
```java
import static db.sql.api.impl.cmd.Methods.*;

// GROUP BY + HAVING
QueryChain.of(sysUserMapper)
    .select(SysUser::getRoleId, c -> count(c))
    .from(SysUser.class)
    .groupBy(SysUser::getRoleId)
    .having(SysUser::getRoleId, c -> c.count().gt(0))
    .list();

// SUM, AVG, MAX, MIN
QueryChain.of(sysUserMapper)
    .select(c -> sum(SysUser::getAmount))
    .select(c -> avg(SysUser::getScore))
    .select(c -> max(SysUser::getLevel))
    .select(c -> min(SysUser::getAge))
    .get();
```

### 省略写法（推荐）
```java
// 当 select/from/returnType 都是 Mapper 实体时，可省略
SysUser user = QueryChain.of(sysUserMapper)
    .eq(SysUser::getId, 1)
    .get();

// 等价于
SysUser user = QueryChain.of(sysUserMapper)
    .select(SysUser.class)
    .from(SysUser.class)
    .eq(SysUser::getId, 1)
    .returnType(SysUser.class)
    .get();
```

### 结果类型转换
```java
// 返回 VO
List<SysUserVo> list = QueryChain.of(sysUserMapper)
    .eq(SysUser::getDelFlag, "0")
    .returnType(SysUserVo.class)
    .list();

// 返回部分字段
SysUser user = QueryChain.of(sysUserMapper)
    .select(SysUser::getId, SysUser::getUserName)
    .eq(SysUser::getId, 1)
    .get();

// 返回单值
String name = QueryChain.of(sysUserMapper)
    .select(SysUser::getUserName)
    .eq(SysUser::getId, 1)
    .returnType(String.class)
    .get();
```

### 嵌套条件
```java
// AND 嵌套
QueryChain.of(sysUserMapper)
    .eq(SysUser::getDelFlag, "0")
    .andNested(q -> {
        q.like(SysUser::getUserName, "admin")
         .or()
         .like(SysUser::getNickName, "admin");
    })
    .list();

// OR 嵌套
QueryChain.of(sysUserMapper)
    .orNested(q -> {
        q.eq(SysUser::getUserName, "admin")
         .eq(SysUser::getStatus, "1");
    })
    .list();
```

---

## 9. VO 自动映射注解

### @ResultEntity（实体关联）
```java
@Data
@ResultEntity(SysUser.class)
public class SysUserVo {
    private Integer id;
    private String userName;

    // 嵌套对象
    @NestedResultEntity
    private SysRoleVo role;

    // 计算字段
    @ResultCalcField("count(1)")
    private Integer count;

    // 枚举值注入
    @PutEnumValue(source = SysUser.class, property = "status",
                  target = UserStatus.class)
    private String statusName;

    // 动态值注入
    @PutValue(source = SysUser.class, property = "deptId",
              factory = DeptFactory.class, method = "getDeptName")
    private String deptName;
}
```

### @ResultField（列映射）
```java
@ResultEntity(SysUser.class)
public class SysUserVo {
    @ResultField("user_name")  // 指定列名
    private String userName;

    @ResultField(value = {"nick_name", "real_name"})  // 多列取值
    private String displayName;
}
```

### @Fetch（自动轮询查询）
```java
@ResultEntity(SysUser.class)
public class SysUserVo {
    @Fetch(source = SysUser.class, property = "roleId",
           target = SysRole.class, targetProperty = "id")
    private SysRoleVo role;

    // 中间表关联
    @Fetch(source = SysUser.class, property = "userId",
           target = SysRole.class, targetProperty = "id",
           middle = SysUserRole.class,
           middleSourceProperty = "userId",
           middleTargetProperty = "roleId")
    private List<SysRoleVo> roles;

    // 限制返回条数
    @Fetch(source = SysUser.class, property = "deptId",
           target = SysDept.class, limit = 5)
    private List<SysDeptVo> depts;
}
```

---

## 10. 对象驱动的条件注解

### @ConditionTarget（目标实体）
```java
@Data
@ConditionTarget(SysUser.class)  // 指定目标实体
public class SysUserQuery {
    private Integer id;

    @Condition(value = Condition.Type.LIKE)
    private String userName;

    @Condition(value = Condition.Type.GTE)
    private LocalDateTime createTimeStart;

    @Condition(value = Condition.Type.LTE, toEndDayTime = true)
    private LocalDateTime createTimeEnd;
}

// 使用
QueryChain.of(sysUserMapper)
    .where(query)
    .list();
```

### @Conditions（多列条件）
```java
@Conditions(
    logic = Logic.OR,
    value = {
        @Condition(property = SysUser.Fields.userName, value = Condition.Type.LIKE),
        @Condition(property = SysUser.Fields.nickName, value = Condition.Type.LIKE)
    }
)
private String keyword;
```

### @ConditionGroup（条件分组）
```java
@ConditionGroup(value = {SysUserQuery.Fields.userName, SysUserQuery.Fields.nickName},
                logic = Logic.OR)
private String keyword;
```

---

## 11. 数据库函数

### 常用函数
```java
import static db.sql.api.impl.cmd.Methods.*;

// 聚合函数
count(c)
sum(c)
avg(c)
max(c)
min(c)

// 字符串函数
concat(c, "suffix")
upper(c)
lower(c)
substring(c, 1, 10)
charLength(c)

// 日期函数
currentDate()
dateDiff(c, c)
dateAdd(c, 1, "DAY")

// 数学函数
abs(c)
round(c)
ceil(c)
floor(c)
```

### SQL 模板
```java
// 普通模板
Methods.tpl("count({0}) + {1}", c, "1")

// 函数模板（可继续链式调用）
Methods.fTpl("CAST({0} AS CHAR)", c)

// 条件模板
Methods.cTpl("{0} + {1} = {2}", cs[0], cs[1], 2)
```

### 使用示例
```java
QueryChain.of(sysUserMapper)
    .select(SysUser::getId, c -> Methods.tpl("count({0})+{1}", c, "1"))
    .and(GetterFields.of(SysUser::getId, SysUser::getId),
         cs -> Methods.cTpl("{0}+{1}={2}", cs[0], cs[1], 2))
    .list();
```

---

## 12. 多数据库支持

### 数据库差异化
```java
QueryChain.of(sysUserMapper)
    .select(SysUser::getId)
    .dbAdapt((query, selector) -> selector
        .when(DbType.H2, db -> query.eq(SysUser::getId, 3))
        .when(DbType.MYSQL, db -> query.eq(SysUser::getId, 2))
        .otherwise(db -> query.eq(SysUser::getId, 1))
    )
    .get();
```

### 支持的数据库
- MySQL
- MariaDB
- Oracle
- PostgreSQL
- H2
- SQLite
- Kingbase
- openGauss

---

## 13. 动态数据源

### 配置
```yaml
spring:
  ds:
    routing:
      primary: master
      datasources:
        master:
          url: jdbc:mysql://localhost:3306/db
          username: root
          password: 123456
        slave:
          url: jdbc:mysql://localhost:3307/db
          username: root
          password: 123456
```

### 使用
```java
// 切换数据源
@DS("master")
public class MasterService {
    public List<SysUser> list() {
        return QueryChain.of(mapper).list();
    }
}

@DS("slave")
public List<SysUser> listFromSlave() {
    return QueryChain.of(mapper).list();
}
```

---

## 14. 全局配置

### XbatisGlobalConfig
```java
@Configuration
public class XbatisConfig {

    @PostConstruct
    public void initXbatis() {
        // 单 Mapper 模式
        XbatisGlobalConfig.setSingleMapperClass(MybatisBasicMapper.class);

        // 下划线策略
        XbatisGlobalConfig.setTableUnderline(true);
        XbatisGlobalConfig.setColumnUnderline(true);

        // 自定义动态值
        XbatisGlobalConfig.setDynamicValue("{day7}", (clazz, type) ->
            new LocalDate[]{
                LocalDate.now().minusDays(7),
                LocalDate.now()
            }
        );

        // 全局监听
        XbatisGlobalConfig.setGlobalOnInsertListener(entity -> {
            if (entity instanceof BaseEntity) {
                BaseEntity base = (BaseEntity) entity;
                base.setCreateTime(LocalDateTime.now());
                base.setCreateBy(StpUtil.getLoginIdAsString());
            }
        });

        XbatisGlobalConfig.setGlobalOnUpdateListener(entity -> {
            if (entity instanceof BaseEntity) {
                BaseEntity base = (BaseEntity) entity;
                base.setUpdateTime(LocalDateTime.now());
                base.setUpdateBy(StpUtil.getLoginIdAsString());
            }
        });

        // 逻辑删除拦截器
        XbatisGlobalConfig.setLogicDeleteInterceptor((entity, update) -> {
            if (entity instanceof BaseEntity) {
                update.set(BaseEntity::getDelTime, LocalDateTime.now());
                update.set(BaseEntity::getDelBy, StpUtil.getLoginIdAsString());
            }
        });
    }
}
```

---

## 15. 最佳实践

### 1. 条件忽略优先使用注解或内置方法
```java
// 推荐：使用 forSearch
QueryChain.of(mapper).forSearch(true).eq(field, value).list();

// 推荐：使用条件注解
@ConditionTarget(SysUser.class)
public class QueryREQ {
    @Condition(likeMode = LikeMode.LEFT)
    private String userName;
}
```

### 2. 统一使用方法引用
```java
// 推荐
QueryChain.of(mapper).eq(SysUser::getId, 1).list();

// 不推荐（硬编码）
QueryChain.of(mapper).eq("id", 1).list();
```

### 3. returnType 放在终止方法前
```java
// 推荐（规范）
QueryChain.of(mapper)
    .eq(SysUser::getId, 1)
    .returnType(SysUserVo.class)
    .get();
```

### 4. 批量操作使用批量 API
```java
// 推荐
mapper.saveBatch(list);
mapper.updateBatch(list);

// 不推荐
for (Entity e : list) { mapper.save(e); }
```

### 5. VO 注解仅用于返回对象
```java
// 正确：VO 使用结果映射注解
@ResultEntity(SysUser.class)
public class SysUserVo { }

// 错误：实体类不应使用结果映射注解
@Table
@ResultEntity(SysUserVo.class)  // 错误！
public class SysUser { }
```

---

## 16. 启动时安全检查（推荐）

### Solon 配置
```yaml
mybatis.master:
  pojoCheck:
    basePackages: com.jimuqu.**.pojo
    modelPackages: com.jimuqu.**.model
    resultEntityPackages: com.jimuqu.**.vo
    conditionTargetPackages: com.jimuqu.**.query
  mappers:
    - "com.jimuqu.**.mapper"
```

### Spring Boot 配置
```java
@Profile("dev")
@Configuration
@XbatisPojoCheckScan(
    basePackages = "com.jimuqu.**.pojo",
    resultEntityPackages = "com.jimuqu.**.vo",
    conditionTargetPackages = "com.jimuqu.**.query"
)
public class XbatisSafeCheckConfig { }
```

---

## 17. 常见问题

### Q: 如何临时关闭逻辑删除？
```java
// 方式1：使用 try-with-resources
try (LogicDeleteSwitch ignored = LogicDeleteSwitch.with(false)) {
    mapper.getById(1);
}

// 方式2：使用工具类
LogicDeleteUtil.execute(false, () -> mapper.getById(1));
```

### Q: 如何获取生成 SQL？
```yaml
# logback-spring.xml
<logger name="cn.xbatis" level="trace"/>
```

### Q: 事务下数据源切换不生效？
```java
// 使用 NOT_SUPPORTED 或 REQUIRES_NEW
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DS("slave")
public List<SysUser> listFromSlave() {
    return mapper.listAll();
}
```

### Q: 自定义 TypeHandler？
```java
@Component
public class JsonTypeHandler extends BaseTypeHandler<Object> {
    // 实现序列化/反序列化
}

// 使用
@TableField(typeHandler = JsonTypeHandler.class)
private Object metadata;
```

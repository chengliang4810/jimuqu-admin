---
title: 完整 CRUD 示例
description: 从实体到控制器的完整分层开发参考
---

## 增删改查功能目录结构要求

- **domain**: 存放实体类
    - `/`: 数据库实体类
    - `bo`: 业务对象
    - `vo`: 视图对象
    - `query`: 查询条件对象
- **mapper**: 存放数据库操作接口
- **service**: 存放业务逻辑接口
  - **impl**: 存放业务逻辑实现类
- **controller**: 存放控制器接口

## 示例代码

### 数据库实体类

~~~java
package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.*;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;

import java.io.Serial;

/**
 * 参数配置
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_config")
public class SysConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键设置方式: 每个实体必须设置主键
     */
    @TableId(value = IdAutoType.GENERATOR, generatorName = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "参数主键")
    private Long id;
    /**
     * 参数名称 字段不能为空， 字符串长度
     */
    @AutoColumn(comment = "参数名称", notNull = true, length = 100)
    private String configName;
    /**
     * 系统内置（Y是 N否）， 默认值的使用
     */
    @AutoColumn(comment = "系统内置（Y是 N否）", length = 1, defaultValue = "N")
    private String configType;
    /**
     * 备注: 字段类型的指定: 长文本使用 MysqlTypeConstant.TEXT
     */
    @AutoColumn(comment = "备注", type = MysqlTypeConstant.TEXT)
    private String remark;
}

~~~

### 业务对象 Bo示例
~~~java
package com.jimuqu.system.domain.bo;

import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysConfig;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.noear.solon.validation.annotation.*;

/**
 * 参数配置业务对象 sys_config
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysConfig.class, reverseConvertGenerate = false)
public class SysConfigBo extends BoBaseEntity {

    /**
     * 参数主键
     */
    @NotNull(message = "参数主键不能为空", groups = { UpdateGroup.class })
    private Long id;
    /**
     * 参数名称
     */
    @NotBlank(message = "参数名称不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String configName;
    /**
     * 参数键名
     */
    @NotBlank(message = "参数键名不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String configKey;
    /**
     * 参数键值
     */
    @NotBlank(message = "参数键值不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String configValue;
    /**
     * 系统内置（Y是 N否）
     */
    @NotBlank(message = "系统内置（Y是 N否）不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String configType;
    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String remark;

}

~~~

### 查询条件对象 Query示例


~~~java
package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.system.domain.SysConfig;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;

import static cn.xbatis.db.annotations.Condition.Type.*;

/**
 * 参数配置查询条件对象
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@FieldNameConstants
@ConditionTarget(SysConfig.class)
public class SysConfigQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 参数主键
     */
    @Condition(value = EQ)
    private Long id;
    /**
     * 参数名称
     */
    @Condition(value = LIKE)
    private String configName;
    /**
     * 参数键名
     */
    @Condition(value = EQ)
    private String configKey;
    /**
     * 参数键值
     */
    @Condition(value = EQ)
    private String configValue;
    /**
     * 系统内置（Y是 N否）
     */
    @Condition(value = EQ)
    private String configType;

    /**
     * 条件构建前执行
     */
    @Override
    public void beforeBuildCondition() {

    }

}

~~~
### 视图对象 Vo示例
~~~java
package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysConfig;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;

/**
 * 参数配置视图对象
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysConfig.class)
@AutoMapper(target = SysConfig.class)
public class SysConfigVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 参数主键
     */
    private Long id;
    /**
     * 参数名称
     */
    private String configName;
    /**
     * 参数键名
     */
    private String configKey;
    /**
     * 参数键值
     */
    private String configValue;
    /**
     * 系统内置（Y是 N否）
     */
    private String configType;
    /**
     * 备注
     */
    private String remark;
}

~~~

### 数据库操作接口 Mapper示例
> 不需要创建mapper的xml文件
~~~java
package com.jimuqu.system.mapper;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysConfig;
import com.jimuqu.system.domain.vo.SysConfigVo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 参数配置数据层
 * @author chengliang4810
 * @since 2025-05-27
 */
@Mapper
public interface SysConfigMapper extends BaseMapperPlus<SysConfig, SysConfigVo> {

    default SysConfig getByKey(String key) {
        return QueryChain.of(this)
                .where(where -> where.eq(SysConfig::getConfigKey, key))
                .get();
    }

    default SysConfigVo getVoByKey(String key) {
        return QueryChain.of(this)
                .where(where -> where.eq(SysConfig::getConfigKey, key))
                .returnType(SysConfigVo.class)
                .get();
    }

}
~~~

### service接口定义示例

~~~java
package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.bo.SysConfigBo;
import com.jimuqu.system.domain.vo.SysConfigVo;
import com.jimuqu.system.domain.query.SysConfigQuery;

import java.util.Collection;
import java.util.List;

/**
 * 参数配置Service接口
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
public interface SysConfigService {

    /**
     * 根据主键查询参数配置
     *
     * @param id 参数配置主键
     * @return {@link SysConfigVo } 参数配置视图对象
     */
   SysConfigVo queryById(Long id);

    /**
     * 查询参数配置分页列表
     *
     * @param query 查询条件对象
     * @param pageQuery 分页条件
     * @return {@link Page }<{@link SysConfigVo }> 参数配置分页对象
     */
    Page<SysConfigVo> queryPageList(SysConfigQuery query, PageQuery pageQuery);

   /**
     * 查询参数配置列表
     *
     * @param query 查询条件对象
     * @return {@link List }<{@link SysConfigVo }> 参数配置列表
     */
    List<SysConfigVo> queryList(SysConfigQuery query);

    /**
     * 新增参数配置
     *
     * @param bo 参数配置业务对象
     * @return {@link Boolean } 新增是否成功
     */
    Boolean insertByBo(SysConfigBo bo);

    /**
     * 更新参数配置
     *
     * @param bo 参数配置业务对象
     * @return {@link Boolean } 更新是否成功
     */
    Boolean updateByBo(SysConfigBo bo);

    /**
     * 批量删除代码生成模板信息
     *
     * @param ids 参数配置主键列表
     * @return {@link Integer } 删除成功条数
     */
    Integer deleteByIds(Collection<Long> ids);
}

~~~

### service 实现类示例

~~~java
package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysConfig;
import com.jimuqu.system.domain.bo.SysConfigBo;
import com.jimuqu.system.domain.vo.SysConfigVo;
import com.jimuqu.system.domain.query.SysConfigQuery;
import com.jimuqu.system.mapper.SysConfigMapper;
import com.jimuqu.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.List;


/**
 * 参数配置Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    /**
     * 查询参数配置
     */
    @Override
    public SysConfigVo queryById(Long id) {
        return sysConfigMapper.getVoById(id);
    }

    /**
     * 查询参数配置分页列表
     */
    @Override
    public Page<SysConfigVo> queryPageList(SysConfigQuery query, PageQuery pageQuery) {
        return buildQueryChain(query)
                .returnType(SysConfigVo.class)
                .paging(pageQuery.build());
    }

    /**
     * 查询参数配置列表
     */
    @Override
    public List<SysConfigVo> queryList(SysConfigQuery query) {
        QueryChain<SysConfig> queryChain = buildQueryChain(query);
        return queryChain.returnType(SysConfigVo.class).list();
    }

    /**
     * 构建查询条件
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysConfig> buildQueryChain(SysConfigQuery query) {
        return QueryChain.of(sysConfigMapper)
                .forSearch(true)
                .where(query);
    }

    /**
     * 新增参数配置
     */
    @Override
    public Boolean insertByBo(SysConfigBo bo) {
        SysConfig sysConfig = MapstructUtil.convert(bo, SysConfig.class);
        boolean flag = sysConfigMapper.save(sysConfig) > 0;
        bo.setId(sysConfig.getId());
        return flag;
    }

    /**
     * 修改参数配置
     */
    @Override
    public Boolean updateByBo(SysConfigBo bo) {
        SysConfig sysConfig = MapstructUtil.convert(bo, SysConfig.class);
        return sysConfigMapper.update(sysConfig) > 0;
    }

    /**
     * 批量删除参数配置
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        return sysConfigMapper.deleteByIds(ids);
    }
}
~~~

### controller接口定义示例

~~~java
package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.SysConfigBo;
import com.jimuqu.system.domain.vo.SysConfigVo;
import com.jimuqu.system.domain.query.SysConfigQuery;
import com.jimuqu.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 参数配置 Controller
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Post
@Controller
@RequiredArgsConstructor
@Mapping("/system/config")
public class SysConfigController extends BaseController {

    private final SysConfigService sysConfigService;

    /**
     * 查询参数配置列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:config:list")
    public Page<SysConfigVo> list(SysConfigQuery query, PageQuery pageQuery) {
        return sysConfigService.queryPageList(query, pageQuery);
    }

    /**
     * 获取参数配置详细信息
     *
     * @param id 参数配置主键
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:config:query")
    public SysConfigVo getInfo(@NotNull(message = "参数配置主键不能为空") Long id) {
        return sysConfigService.queryById(id);
    }

    /**
     * 新增参数配置
     */
    @Mapping("/add")
    @NoRepeatSubmit
    @SaCheckPermission("system:config:add")
    @Log(title = "新增参数配置", businessType = BusinessType.ADD)
    public Long add(@Validated(AddGroup.class) SysConfigBo bo) {
        boolean result = sysConfigService.insertByBo(bo);
        Assert.isTrue(result, "新增参数配置失败");
        return bo.getId();
    }

    /**
     * 更新参数配置
     */
    @NoRepeatSubmit
    @Mapping("/update")
    @SaCheckPermission("system:config:update")
    @Log(title = "更新参数配置", businessType = BusinessType.UPDATE)
    public void edit(@Validated(UpdateGroup.class) SysConfigBo bo) {
        boolean result = sysConfigService.updateByBo(bo);
        Assert.isTrue(result, "更新参数配置失败");
    }

    /**
     * 删除参数配置
     */
    @Mapping("/delete/{ids}")
    @SaCheckPermission("system:config:delete")
    @Log(title = "删除参数配置", businessType = BusinessType.DELETE)
    public Integer delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Integer num = sysConfigService.deleteByIds(ids);
        Assert.gtZero(num, "删除参数配置失败");
        return num;
    }

}
~~~

## BaseEntity说明
~~~java
package com.jimuqu.common.mybatis.core.entity;

import cn.xbatis.db.annotations.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity基类
 *
 * @author chengliang
 * @since 2024/06/13
 */

@Data
public class BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 创建部门
     */
    @TableField(defaultValue = "{CURRENT_DEPT_ID}")
    protected Long createDept;
    /**
     * 创建者
     */
    @TableField(defaultValue = "{CURRENT_USER_ID}")
    protected Long createBy;
    /**
     * 创建时间
     */
    @TableField(defaultValue = "{NOW}")
    protected Date createTime;
    /**
     * 更新者
     */
    @TableField(defaultValue = "{CURRENT_USER_ID}", updateDefaultValue = "{CURRENT_USER_ID}")
    protected Long updateBy;
    /**
     * 更新时间
     */
    @TableField(defaultValue = "{NOW}", updateDefaultValue = "{NOW}")
    protected Date updateTime;
}
~~~

## MysqlTypeConstant说明

~~~java
package org.dromara.autotable.annotation.mysql;

/**
 * @author don
 */
public interface MysqlTypeConstant {

    /**
     * 整数
     */
    String INT = "int";
    String TINYINT = "tinyint";
    String SMALLINT = "smallint";
    String MEDIUMINT = "mediumint";
    String BIGINT = "bigint";
    /**
     * 小数
     */
    String FLOAT = "float";
    String DOUBLE = "double";
    String DECIMAL = "decimal";
    /**
     * 字符串
     */
    String CHAR = "char";
    String VARCHAR = "varchar";
    String TEXT = "text";
    String TINYTEXT = "tinytext";
    String MEDIUMTEXT = "mediumtext";
    String LONGTEXT = "longtext";
    /**
     * 枚举
     */
    String ENUM = "enum";
    String SET = "set";
    /**
     * 日期
     */
    String YEAR = "year";
    String TIME = "time";
    String DATE = "date";
    String DATETIME = "datetime";
    String TIMESTAMP = "timestamp";
    /**
     * 二进制
     */
    String BIT = "bit";
    String BINARY = "binary";
    String VARBINARY = "varbinary";
    String BLOB = "blob";
    String TINYBLOB = "tinyblob";
    String MEDIUMBLOB = "mediumblob";
    String LONGBLOB = "longblob";
    /**
     * json
     */
    String JSON = "json";
}

~~~

### BoBaseEntity内容

~~~java
package com.jimuqu.common.mybatis.core.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * BoEntity基类
 *
 * @author chengliang
 * @date 2025/03/16
 */
@Data
public class BoBaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 自由拓展参数
     */
    private Map<String, Object> params = new HashMap<>();
}

~~~

### Condition注解说明

~~~java
package cn.xbatis.db.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(Conditions.class)
public @interface Condition {

    /**
     * 条件类型
     *
     * @return Type
     */
    Type value() default Type.EQ;

    /**
     * 目标实体类,如果在实体类里 或者类上指定了，则可不写
     *
     * @return 实体类
     */
    Class<?> target() default Void.class;

    /**
     * 属性
     *
     * @return 属性
     */
    String property() default "";

    /**
     * 存储层级
     *
     * @return
     */
    int storey() default 1;

    /**
     * like的方式 默认 %xx%
     *
     * @return LikeMode
     */
    LikeMode likeMode() default LikeMode.DEFAULT;

    /**
     * 将日期转成到这天的最后1秒
     * 只支持 lte 和 between的第2个参数
     * 支持类型为LocalDate/Date/String/Long/LocalDateTime
     *
     * @return
     */
    boolean toEndDayTime() default false;

    /**
     * 支持基本类型的默认值
     * 支持动态默认值，也可以自定义默认值；
     * 例如 官方的默认值 "{NOW}" "{TODAY}"
     * "{NOW}" 支持单个时间
     * "{TODAY}" 时间范围（数组类型或者集合类型字段）
     *
     * @return
     */
    String defaultValue() default "";

    enum Type {
        IGNORE,
        EQ,
        NE,
        IN,
        LT,
        LTE,
        GT,
        GTE,
        LIKE,
        NOT_LIKE,
        BETWEEN
    }

    enum LikeMode {
        NONE,
        DEFAULT,
        LEFT,
        RIGHT
    }
}

~~~



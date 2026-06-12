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
     * 参数主键
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "参数主键")
    private Long id;
    /**
     * 参数名称
     */
    @AutoColumn(comment = "参数名称", length = 100)
    private String configName;
    /**
     * 参数键名
     */
    @AutoColumn(comment = "参数键名", length = 100)
    private String configKey;
    /**
     * 参数键值
     */
    @AutoColumn(comment = "参数键值", length = 500)
    private String configValue;
    /**
     * 系统内置（Y是 N否）
     */
    @AutoColumn(comment = "系统内置（Y是 N否）", length = 1, defaultValue = "N")
    private String configType;
    /**
     * 备注
     */
    @AutoColumn(comment = "备注", length = 500)
    private String remark;

}

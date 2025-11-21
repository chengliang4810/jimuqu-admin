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
    private String configType;
    /**
     * 备注
     */
    private String remark;

}

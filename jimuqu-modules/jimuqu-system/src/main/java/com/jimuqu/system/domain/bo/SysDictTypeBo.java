package com.jimuqu.system.domain.bo;

import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysDictType;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.noear.solon.validation.annotation.*;

/**
 * 字典类型业务对象 sys_dict_type
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysDictType.class, reverseConvertGenerate = false)
public class SysDictTypeBo extends BoBaseEntity {

    /**
     * 字典主键
     */
    @NotNull(message = "字典主键不能为空", groups = { UpdateGroup.class })
    private Long dictId;
    /**
     * 字典key
     */
    private String dictKey;
    /**
     * 字典名称
     */
    @NotBlank(message = "字典名称不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String dictName;
    /**
     * 字典类型 L 列表 T 树
     */
    @NotBlank(message = "字典类型不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String dictType;
    /**
     * 系统内置（Y是 N否）
     */
    private String isBuiltIn;
    /**
     * 备注
     */
    private String remark;

}

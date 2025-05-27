package com.jimuqu.system.domain.bo;

import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysDictData;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.noear.solon.validation.annotation.*;

/**
 * 字典数据业务对象 sys_dict_data
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysDictData.class, reverseConvertGenerate = false)
public class SysDictDataBo extends BoBaseEntity {

    /**
     * 字典ID
     */
    @NotNull(message = "字典ID不能为空", groups = { UpdateGroup.class })
    private Long id;
    /**
     * 父级ID
     */
    @NotNull(message = "父级ID不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Long parentId;
    /**
     * 字典排序
     */
    @NotNull(message = "字典排序不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Long dictSort;
    /**
     * 字典标签
     */
    @NotBlank(message = "字典标签不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String dictLabel;
    /**
     * 字典键值
     */
    @NotBlank(message = "字典键值不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String dictValue;
    /**
     * 字典类型
     */
    @NotBlank(message = "字典类型不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String dictType;
    /**
     * 样式属性（其他样式扩展）
     */
    @NotBlank(message = "样式属性（其他样式扩展）不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String cssClass;
    /**
     * 表格回显样式
     */
    @NotBlank(message = "表格回显样式不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String listClass;
    /**
     * 是否默认（Y是 N否）
     */
    @NotBlank(message = "是否默认（Y是 N否）不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String isDefault;
    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String remark;

}

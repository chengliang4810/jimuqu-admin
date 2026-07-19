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
import org.noear.snack4.annotation.ONodeAttr;

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
    @ONodeAttr(name = "dictCode")
    private Long id;
    /**
     * 父级ID
     */
    private Long parentId;
    /**
     * 字典排序
     */
    @NotNull(message = "字典排序不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Integer dictSort;
    /**
     * 字典标签
     */
    @NotBlank(message = "字典标签不能为空", groups = { AddGroup.class, UpdateGroup.class })
    @Length(max = 100, message = "字典标签长度不能超过{max}个字符")
    private String dictLabel;
    /**
     * 字典键值
     */
    @NotBlank(message = "字典键值不能为空", groups = { AddGroup.class, UpdateGroup.class })
    @Length(max = 100, message = "字典键值长度不能超过{max}个字符")
    private String dictValue;
    /**
     * 字典类型
     */
    private String dictTypeKey;

    /**
     * Bell 兼容字段。
     */
    @NotBlank(message = "字典类型不能为空", groups = { AddGroup.class, UpdateGroup.class })
    @Length(max = 100, message = "字典类型长度不能超过{max}个字符")
    private String dictType;
    /**
     * 样式属性（其他样式扩展）
     */
    @Length(max = 100, message = "样式属性长度不能超过{max}个字符")
    private String cssClass;
    /**
     * 表格回显样式
     */
    private String listClass;
    /**
     * 是否默认（Y是 N否）
     */
    private String isDefault;
    /**
     * 备注
     */
    private String remark;

}

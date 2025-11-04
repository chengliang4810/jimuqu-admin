package com.jimuqu.system.domain.bo;

import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysPost;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.noear.solon.validation.annotation.*;

/**
 * 岗位信息业务对象 sys_post
 *
 * @author chengliang4810
 * @since 2025-06-04
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysPost.class, reverseConvertGenerate = false)
public class SysPostBo extends BoBaseEntity {

    /**
     * 岗位ID
     */
    @NotNull(message = "岗位ID不能为空", groups = { UpdateGroup.class })
    private Long postId;
    /**
     * 部门id
     */
    @NotNull(message = "部门id不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Long deptId;
    /**
     * 岗位编码
     */
    @NotBlank(message = "岗位编码不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String postCode;
    /**
     * 岗位类别编码
     */
    private String postCategory;
    /**
     * 岗位名称
     */
    @NotBlank(message = "岗位名称不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String postName;
    /**
     * 显示顺序
     */
    private Long postSort;
    /**
     * 状态（0正常 1停用）
     */
    private String status;
    /**
     * 备注
     */
    private String remark;

}
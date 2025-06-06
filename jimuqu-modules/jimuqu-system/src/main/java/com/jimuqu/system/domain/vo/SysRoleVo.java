package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.Ignore;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.system.domain.SysRole;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 角色信息视图对象
 * @author chengliang4810
 * @since 2025-06-05
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysRole.class)
@AutoMapper(target = SysRole.class)
public class SysRoleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    private Long id;
    /**
     * 角色名称
     */
    private String roleName;
    /**
     * 角色权限字符串
     */
    private String roleKey;
    /**
     * 显示顺序
     */
    private Integer roleSort;
    /**
     * 数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）
     */
    private String dataScope;
    /**
     * 菜单树选择项是否关联显示
     */
    private Boolean menuCheckStrictly;
    /**
     * 部门树选择项是否关联显示
     */
    private Boolean deptCheckStrictly;
    /**
     * 角色状态（0正常 1停用）
     */
    private String status;
    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 用户是否存在此角色标识 默认不存在
     */
    @Ignore
    private boolean flag = false;

    public boolean isSuperAdmin() {
        return UserConstants.SUPER_ADMIN_ID.equals(this.id);
    }

}

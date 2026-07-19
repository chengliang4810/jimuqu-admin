package com.jimuqu.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.core.xss.Xss;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysUser;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.noear.solon.validation.annotation.*;
import org.noear.snack4.annotation.ONodeAttr;

import java.util.List;

/**
 * 用户信息业务对象 sys_user
 *
 * @author chengliang4810
 * @since 2025-06-05
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysUser.class, reverseConvertGenerate = false)
public class SysUserBo extends BoBaseEntity {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空", groups = { UpdateGroup.class })
    @JsonProperty("userId")
    @ONodeAttr(name = "userId")
    private Long id;
    /**
     * 部门ID
     */
    @NotNull(message = "部门ID不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Long deptId;
    /**
     * 用户账号
     */
    @Xss(message = "用户账号不能包含脚本字符")
    @Length(min = 2, max = 30, message = "用户账号长度必须在{min}到{max}个字符之间")
    @NotBlank(message = "用户账号不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String userName;
    /**
     * 用户昵称
     */
    @Xss(message = "用户昵称不能包含脚本字符")
    @Length(min = 0, max = 30, message = "用户昵称长度不能超过{max}个字符")
    @NotBlank(message = "用户昵称不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String nickName;
    /**
     * 用户类型（sys_user系统用户）
     */
    private String userType;
    /**
     * 用户邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Length(min = 0, max = 50, message = "邮箱长度不能超过{max}个字符")
    private String email;
    /**
     * 手机号码
     */
    @JsonProperty("phoneNumber")
    @ONodeAttr(name = "phoneNumber")
    private String phonenumber;
    /**
     * 用户性别（0男 1女 2未知）
     */
    private String sex;
    /**
     * 头像地址
     */
    private Long avatar;
    /**
     * 密码
     */
    private String password;
    /**
     * 帐号状态（0正常 1停用）
     */
    private String status;
    /**
     * 备注
     */
    private String remark;

    /**
     * 角色组
     */
    @Size(min = 1, message = "用户角色不能为空")
    private List<Long> roleIds;

    /**
     * 岗位组
     */
    private List<Long> postIds;

    /**
     * 数据权限 当前角色ID
     */
    private Long roleId;

    public SysUserBo(Long userId) {
        this.id = userId;
    }

    public boolean isSuperAdmin() {
        return UserConstants.SUPER_ADMIN_ID.equals(this.id);
    }

}

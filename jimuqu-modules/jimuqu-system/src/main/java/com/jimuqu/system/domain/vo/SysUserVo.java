package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.Ignore;
import cn.xbatis.db.annotations.NestedResultEntity;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysUser;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户信息视图对象
 * @author chengliang4810
 * @since 2025-06-05
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysUser.class)
@AutoMapper(target = SysUser.class)
public class SysUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;
    /**
     * 部门ID
     */
    private Long deptId;
    /**
     * 用户账号
     */
    private String userName;
    /**
     * 用户昵称
     */
    private String nickName;
    /**
     * 用户类型（sys_user系统用户）
     */
    private String userType;
    /**
     * 用户邮箱
     */
    private String email;
    /**
     * 手机号码
     */
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
     * 最后登录IP
     */
    private String loginIp;
    /**
     * 最后登录时间
     */
    private Date loginDate;
    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 部门对象
     */
    @NestedResultEntity(target = SysDept.class)
    private SysDeptVo dept;

    /**
     * 角色对象
     */
     @NestedResultEntity(target = SysRole.class)
    private List<SysRoleVo> roles;

    /**
     * 角色组
     */
    @Ignore
    private List<Long> roleIds;

    /**
     * 岗位组
     */
    @Ignore
    private List<Long> postIds;

    /**
     * 数据权限 当前角色ID
     */
    @Ignore
    private Long roleId;

}

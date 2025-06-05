package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.Ignore;
import cn.xbatis.db.annotations.NestedResultEntity;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysUser;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * 用户信息视图对象 sys_user
 *
 * @author Michelle.Chung
 */
@Data
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
     * 租户ID
     */
    private String tenantId;

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
    // @Sensitive(strategy = SensitiveStrategy.EMAIL)
    private String email;

    /**
     * 手机号码
     */
    // @Sensitive(strategy = SensitiveStrategy.PHONE)
    private String phonenumber;

    /**
     * 用户性别（0男 1女 2未知）
     */
    private String sex;

    /**
     * 头像地址
     */
    // @Transactionslation(type = TransConstant.OSS_ID_TO_URL)
    private Long avatar;

    /**
     * 密码
     */
    private transient String password;

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
    //@RelationOneToOne(selfField = "deptId", joinSelfColumn = "dept_id", targetField = "deptId", joinTargetColumn = "dept_id", targetTable = "sys_dept")
    private SysDeptVo dept;

    /**
     * 角色对象
     */
    //@RelationManyToMany(selfField = "userId", joinSelfColumn = "user_id", targetTable = "sys_role", targetField = "roleId", joinTargetColumn = "role_id", joinTable = "sys_user_role")
     @NestedResultEntity(target = SysRole.class)
    private List<SysRoleVo> roles;

    /**
     * 角色组
     */
    @Ignore
    private Long[] roleIds;

    /**
     * 岗位组
     */
    @Ignore
    private Long[] postIds;

    /**
     * 数据权限 当前角色ID
     */
    @Ignore
    private Long roleId;

}

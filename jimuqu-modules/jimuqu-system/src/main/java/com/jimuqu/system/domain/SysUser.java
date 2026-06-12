package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;

import java.io.Serial;
import java.util.Date;

/**
 * 用户信息
 * @author chengliang4810
 * @since 2025-06-05
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_user")
public class SysUser extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "用户ID")
    private Long id;
    /**
     * 部门ID
     */
    @AutoColumn(comment = "部门ID")
    private Long deptId;
    /**
     * 用户账号
     */
    @AutoColumn(comment = "用户账号", length = 30)
    private String userName;
    /**
     * 用户昵称
     */
    @AutoColumn(comment = "用户昵称", length = 30)
    private String nickName;
    /**
     * 用户类型（pc_user系统用户）
     */
    @AutoColumn(comment = "用户类型（pc_user系统用户）", length = 10, defaultValue = "pc_user")
    private String userType;
    /**
     * 用户邮箱
     */
    @AutoColumn(comment = "用户邮箱", length = 50)
    private String email;
    /**
     * 手机号码
     */
    @AutoColumn(comment = "手机号码", length = 11)
    private String phonenumber;
    /**
     * 用户性别（0男 1女 2未知）
     */
    @AutoColumn(comment = "用户性别（0男 1女 2未知）", length = 1, defaultValue = "0")
    private String sex;
    /**
     * 头像地址
     */
    @AutoColumn(comment = "头像地址")
    private Long avatar;
    /**
     * 密码
     */
    @AutoColumn(comment = "密码", length = 100)
    private String password;
    /**
     * 帐号状态（0正常 1停用）
     */
    @AutoColumn(comment = "帐号状态（0正常 1停用）", length = 1, defaultValue = "0")
    private String status;
    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @AutoColumn(comment = "删除标志（0代表存在 1代表删除）", length = 1, defaultValue = "0")
    private String delFlag;
    /**
     * 最后登录IP
     */
    @AutoColumn(comment = "最后登录IP", length = 128)
    private String loginIp;
    /**
     * 最后登录时间
     */
    @AutoColumn(comment = "最后登录时间")
    private Date loginDate;
    /**
     * 备注
     */
    @AutoColumn(comment = "备注", length = 500)
    private String remark;


    public SysUser(Long userId) {
        this.id = userId;
    }

    public boolean isSuperAdmin() {
        return UserConstants.SUPER_ADMIN_ID.equals(this.id);
    }

}

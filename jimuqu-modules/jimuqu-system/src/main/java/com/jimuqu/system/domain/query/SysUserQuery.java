package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import cn.xbatis.db.annotations.Ignore;
import com.jimuqu.common.core.utils.DateUtil;
import com.jimuqu.system.domain.SysUser;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.xbatis.db.annotations.Condition.Type.BETWEEN;
import static cn.xbatis.db.annotations.Condition.Type.EQ;
import static cn.xbatis.db.annotations.Condition.Type.IGNORE;
import static cn.xbatis.db.annotations.Condition.Type.LIKE;

/**
 * 用户信息查询条件对象
 * @author chengliang4810
 * @since 2025-06-05
 */
@Data
@FieldNameConstants
@ConditionTarget(SysUser.class)
public class SysUserQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    @Condition(IGNORE)
    private Map<String, Object> params = new HashMap<>();

    /**
     * 用户ID
     */
    @Condition(value = EQ)
    private Long id;
    /**
     * 部门ID
     */
    @Ignore
    private Long deptId;
    /** 角色用户分配查询条件。 */
    @Ignore
    private Long roleId;
    /**
     * 用户账号
     */
    @Condition(value = LIKE)
    private String userName;
    /**
     * 用户昵称
     */
    @Condition(value = LIKE)
    private String nickName;
    /**
     * 用户类型（sys_user系统用户）
     */
    @Condition(value = EQ)
    private String userType;
    /**
     * 用户邮箱
     */
    @Condition(value = EQ)
    private String email;
    /**
     * 手机号码
     */
    @Condition(value = LIKE)
    private String phonenumber;

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    /**
     * Bell 前端使用的手机号查询参数。
     */
    public String getPhoneNumber() {
        return phonenumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phonenumber = phoneNumber;
    }
    /**
     * 用户性别（0男 1女 2未知）
     */
    @Condition(value = EQ)
    private String sex;
    /**
     * 头像地址
     */
    @Condition(value = EQ)
    private Long avatar;
    /**
     * 密码
     */
    @Condition(value = EQ)
    private String password;
    /**
     * 帐号状态（0正常 1停用）
     */
    @Condition(value = EQ)
    private String status;
    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @Condition(value = EQ)
    private String delFlag;
    /**
     * 最后登录IP
     */
    @Condition(value = EQ)
    private String loginIp;
    /**
     * 最后登录时间
     */
    @Condition(value = EQ)
    private Date loginDate;
    /**
     * 备注
     */
    @Condition(value = EQ)
    private String remark;

    /**
     * 创建时间范围
     */
    @Condition(BETWEEN)
    private List<Date> createTime;

    /**
     * 条件构建前执行
     */
    @Override
    public void beforeBuildCondition() {
        Object beginTime = params.get("beginTime");
        Object endTime = params.get("endTime");
        if (beginTime != null && endTime != null) {
            createTime = List.of(
                    DateUtil.dateTime(DateUtil.YYYY_MM_DD_HH_MM_SS, beginTime.toString()),
                    DateUtil.dateTime(DateUtil.YYYY_MM_DD_HH_MM_SS, endTime.toString()));
        }
    }

}

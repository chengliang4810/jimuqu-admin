package com.jimuqu.system.domain.bo;

import com.jimuqu.common.core.xss.Xss;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.noear.solon.validation.annotation.Email;
import org.noear.solon.validation.annotation.Size;
import org.noear.snack4.annotation.ONodeAttr;


/**
 * 个人信息业务处理
 *
 * @author Michelle.Chung
 */

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SysUserProfileBo extends BoBaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    @Xss(message = "用户昵称不能包含脚本字符")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过{max}个字符")
    private String nickName;

    /**
     * 用户邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过{max}个字符")
    private String email;

    /**
     * 手机号码
     */
    @ONodeAttr(name = "phoneNumber")
    private String phonenumber;

    /**
     * 用户性别（0男 1女 2未知）
     */
    private String sex;

    /**
     * 头像文件ID
     */
    private Long avatar;

}

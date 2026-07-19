package com.jimuqu.system.domain.bo;

import com.jimuqu.common.core.constant.RegexConstants;
import com.jimuqu.common.core.sensitive.annotation.Sensitive;
import com.jimuqu.common.core.sensitive.enums.SensitiveType;
import com.jimuqu.common.core.xss.Xss;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.noear.solon.validation.annotation.Email;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.Pattern;
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
     * 用户昵称
     */
    @Xss(message = "用户昵称不能包含脚本字符")
    @Length(min = 0, max = 30, message = "用户昵称长度不能超过{max}个字符")
    private String nickName;

    /**
     * 用户邮箱
     */
    @Sensitive(type = SensitiveType.EMAIL)
    @Email(message = "邮箱格式不正确")
    @Length(min = 0, max = 50, message = "邮箱长度不能超过{max}个字符")
    private String email;

    /**
     * 手机号码
     */
    @Sensitive(type = SensitiveType.MOBILE)
    @Pattern(value = RegexConstants.MOBILE, message = "手机号格式不正确")
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

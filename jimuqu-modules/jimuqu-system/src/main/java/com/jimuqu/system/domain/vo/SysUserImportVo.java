package com.jimuqu.system.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import com.jimuqu.common.excel.annotation.ExcelDictFormat;
import com.jimuqu.common.excel.convert.ExcelDictConvert;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.noear.solon.validation.annotation.Email;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.NotNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户对象导入VO
 *
 * @author Lion Li,chengliang4810
 */

@Data
@NoArgsConstructor
// @Accessors(chain = true) // 导入不允许使用 会找不到set方法
public class SysUserImportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户序号")
    private Long userId;

    /**
     * 部门ID
     */
    @ExcelProperty(value = "部门编号")
    @NotNull(message = "部门编号不能为空")
    private Long deptId;

    /**
     * 用户账号
     */
    @ExcelProperty(value = "登录名称")
    @NotBlank(message = "登录名称不能为空")
    @Length(max = 30, message = "登录名称长度不能超过30个字符")
    private String userName;

    /**
     * 用户昵称
     */
    @ExcelProperty(value = "用户名称")
    @NotBlank(message = "用户名称不能为空")
    @Length(max = 30, message = "用户名称长度不能超过30个字符")
    private String nickName;

    /**
     * 用户邮箱
     */
    @ExcelProperty(value = "用户邮箱")
    @Email(message = "用户邮箱格式不正确")
    @Length(max = 50, message = "用户邮箱长度不能超过50个字符")
    private String email;

    /**
     * 手机号码
     */
    @ExcelProperty(value = "手机号码")
    private String phonenumber;

    /**
     * 用户性别
     */
    @ExcelProperty(value = "用户性别", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_user_sex")
    private String sex;

    /**
     * 帐号状态（0正常 1停用）
     */
    @ExcelProperty(value = "帐号状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

}

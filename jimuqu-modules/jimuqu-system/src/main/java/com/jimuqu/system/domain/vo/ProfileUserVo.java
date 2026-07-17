package com.jimuqu.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 当前用户个人中心资料，不应用列表脱敏规则。 */
@Data
public class ProfileUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("userId")
    @ONodeAttr(name = "userId")
    private Long id;
    private Long deptId;
    private String userName;
    private String nickName;
    private String userType;
    private String email;
    @JsonProperty("phoneNumber")
    @ONodeAttr(name = "phoneNumber")
    private String phonenumber;
    private String sex;
    private Long avatar;
    private String avatarUrl;
    private String loginIp;
    private Date loginDate;
    private String deptName;
}

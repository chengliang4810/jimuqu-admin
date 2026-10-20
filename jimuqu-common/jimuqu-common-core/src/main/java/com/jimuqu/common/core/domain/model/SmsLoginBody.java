package com.jimuqu.common.core.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.solon.validation.annotation.NotBlank;
import org.apache.fory.json.annotation.JsonProperty;

/**
 * 短信登录对象
 *
 * @author Lion Li,chengliang4810
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class SmsLoginBody extends LoginBody {

    /**
     * 手机号
     */
    @NotBlank(message = "{user.phonenumber.not.blank}")
    @JsonProperty("phoneNumber")
    private String phonenumber;

    /**
     * 短信code
     */
    @NotBlank(message = "{sms.code.not.blank}")
    private String smsCode;

}

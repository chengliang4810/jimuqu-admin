package com.jimuqu.system.domain.bo;

import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Pattern;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对象存储配置业务对象。
 */
@Data
public class SysOssConfigBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "主键不能为空", groups = UpdateGroup.class)
    private Long ossConfigId;
    @NotBlank(message = "配置key不能为空", groups = {AddGroup.class, UpdateGroup.class})
    @Length(min = 2, max = 100, message = "configKey长度必须介于2和100之间")
    private String configKey;
    @NotBlank(message = "accessKey不能为空", groups = {AddGroup.class, UpdateGroup.class})
    @Length(min = 2, max = 100, message = "accessKey长度必须介于2和100之间")
    private String accessKey;
    @NotBlank(message = "secretKey不能为空", groups = {AddGroup.class, UpdateGroup.class})
    @Length(min = 2, max = 100, message = "secretKey长度必须介于2和100之间")
    private String secretKey;
    @NotBlank(message = "桶名称不能为空", groups = {AddGroup.class, UpdateGroup.class})
    @Length(min = 2, max = 100, message = "bucketName长度必须介于2和100之间")
    private String bucketName;
    private String prefix;
    @NotBlank(message = "访问站点不能为空", groups = {AddGroup.class, UpdateGroup.class})
    @Length(min = 2, max = 100, message = "endpoint长度必须介于2和100之间")
    private String endpoint;
    private String domainUrl;
    private String isHttps;
    private String region;
    private String status;
    private String ext1;
    private String remark;
    @NotBlank(message = "桶权限类型不能为空", groups = {AddGroup.class, UpdateGroup.class})
    @Pattern(value = "[012]", message = "桶权限类型必须为0、1或2")
    private String accessPolicy;
}

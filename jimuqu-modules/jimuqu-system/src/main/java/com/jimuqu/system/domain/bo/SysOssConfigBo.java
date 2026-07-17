package com.jimuqu.system.domain.bo;

import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;
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

    private Long ossConfigId;
    @NotBlank(message = "配置key不能为空")
    private String configKey;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String prefix;
    private String endpoint;
    private String domainUrl;
    private String isHttps;
    private String region;
    private String status;
    private String ext1;
    private String remark;
    @NotBlank(message = "桶权限类型不能为空")
    @Pattern(value = "[012]", message = "桶权限类型必须为0、1或2")
    private String accessPolicy;
}

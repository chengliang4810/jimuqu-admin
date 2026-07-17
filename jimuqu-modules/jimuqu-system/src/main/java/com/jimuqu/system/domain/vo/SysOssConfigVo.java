package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysOssConfig;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@ResultEntity(SysOssConfig.class)
@AutoMapper(target = SysOssConfig.class)
public class SysOssConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long ossConfigId;
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
    private String accessPolicy;
}

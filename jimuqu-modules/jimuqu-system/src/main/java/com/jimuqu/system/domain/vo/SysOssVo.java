package com.jimuqu.system.domain.vo;

import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.enums.TransType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Bell OSS 文件视图。
 */
@Data
@Accessors(chain = true)
public class SysOssVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String ossId;
    private String fileName;
    private String originalName;
    private String fileSuffix;
    private String url;
    private String ext1;
    private Date createTime;
    private Long createBy;
    @Trans(type = TransType.USER_NAME, field = "createBy")
    private String createByName;
    private String service;
}

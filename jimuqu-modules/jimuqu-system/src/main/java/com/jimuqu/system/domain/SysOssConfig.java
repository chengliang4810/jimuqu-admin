package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.dromara.autotable.annotation.AutoColumn;

/**
 * 对象存储配置。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Table("sys_oss_config")
public class SysOssConfig extends BaseEntity {

    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "OSS配置ID")
    private Long ossConfigId;

    @AutoColumn(comment = "配置key", length = 100, notNull = true)
    private String configKey;
    @AutoColumn(comment = "accessKey", length = 255)
    private String accessKey;
    @AutoColumn(comment = "secretKey", length = 255)
    private String secretKey;
    @AutoColumn(comment = "桶名称", length = 255)
    private String bucketName;
    @AutoColumn(comment = "前缀", length = 255)
    private String prefix;
    @AutoColumn(comment = "访问站点", length = 255)
    private String endpoint;
    @AutoColumn(comment = "自定义域名", length = 255)
    private String domainUrl;
    @AutoColumn(comment = "是否https", length = 1, defaultValue = "0")
    private String isHttps;
    @AutoColumn(comment = "区域", length = 100)
    private String region;
    @AutoColumn(comment = "是否默认（Y是 N否）", length = 1, defaultValue = "N")
    private String status;
    @AutoColumn(comment = "扩展字段", length = 500)
    private String ext1;
    @AutoColumn(comment = "备注", length = 500)
    private String remark;
    @AutoColumn(comment = "桶权限类型", length = 1, defaultValue = "0")
    private String accessPolicy;
}

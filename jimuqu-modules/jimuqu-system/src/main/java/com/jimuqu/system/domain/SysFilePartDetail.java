package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.mybatis.core.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

import java.io.Serial;

/**
 * 文件分片信息，仅在手动分片上传时使用
 * @author chengliang4810
 * @since 2025-06-24
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_file_part_detail")
public class SysFilePartDetail extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分片id
     */
    @TableId(value = IdAutoType.GENERATOR, generatorName = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "分片id", length = 32)
    private String id;
    /**
     * 存储平台
     */
    @AutoColumn(comment = "存储平台", length = 32)
    private String platform;
    /**
     * 上传ID，仅在手动分片上传时使用
     */
    @AutoColumn(comment = "上传ID，仅在手动分片上传时使用", length = 128)
    private String uploadId;
    /**
     * 分片 ETag
     */
    @AutoColumn(comment = "分片 ETag", length = 255)
    private String eTag;
    /**
     * 分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000
     */
    @AutoColumn(comment = "分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000")
    private Integer partNumber;
    /**
     * 文件大小，单位字节
     */
    @AutoColumn(comment = "文件大小，单位字节")
    private Long partSize;
    /**
     * 哈希信息
     */
    @AutoColumn(comment = "哈希信息", type = MysqlTypeConstant.TEXT)
    private String hashInfo;

}

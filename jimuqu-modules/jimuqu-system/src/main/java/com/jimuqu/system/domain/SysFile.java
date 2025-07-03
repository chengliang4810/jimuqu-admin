package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.*;
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
 * 文件记录
 * @author chengliang4810
 * @since 2025-06-24
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_file")
public class SysFile extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件id
     */
    @TableId(value = IdAutoType.GENERATOR, generatorName = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "文件id", length = 32)
    private String id;
    /**
     * 文件访问地址
     */
    @AutoColumn(comment = "文件访问地址", length = 512)
    private String url;
    /**
     * 文件大小，单位字节
     */
    @AutoColumn(comment = "文件大小，单位字节")
    private Long size;
    /**
     * 文件名称
     */
    @AutoColumn(comment = "文件名称", length = 256)
    private String filename;
    /**
     * 原始文件名
     */
    @AutoColumn(comment = "原始文件名", length = 256)
    private String originalFilename;
    /**
     * 基础存储路径
     */
    @AutoColumn(comment = "基础存储路径", length = 256)
    private String basePath;
    /**
     * 存储路径
     */
    @AutoColumn(comment = "存储路径", length = 256)
    private String path;
    /**
     * 文件扩展名
     */
    @AutoColumn(comment = "文件扩展名", length = 32)
    private String ext;
    /**
     * MIME类型
     */
    @AutoColumn(comment = "MIME类型", length = 128)
    private String contentType;
    /**
     * 存储平台
     */
    @AutoColumn(comment = "存储平台", length = 32)
    private String platform;
    /**
     * 缩略图访问路径
     */
    @AutoColumn(comment = "缩略图访问路径", length = 512)
    private String thUrl;
    /**
     * 缩略图名称
     */
    @AutoColumn(comment = "缩略图名称", length = 256)
    private String thFilename;
    /**
     * 缩略图大小，单位字节
     */
    @AutoColumn(comment = "缩略图大小，单位字节")
    private Long thSize;
    /**
     * 缩略图MIME类型
     */
    @AutoColumn(comment = "缩略图MIME类型", length = 128)
    private String thContentType;
    /**
     * 文件所属对象id
     */
    @AutoColumn(comment = "文件所属对象id", length = 32)
    private String objectId;
    /**
     * 文件所属对象类型，例如用户头像，评价图片
     */
    @AutoColumn(comment = "文件所属对象类型，例如用户头像，评价图片", length = 32)
    private String objectType;
    /**
     * 文件元数据
     */
    @AutoColumn(comment = "文件元数据", type = MysqlTypeConstant.TEXT)
    private String metadata;
    /**
     * 文件用户元数据
     */
    @AutoColumn(comment = "文件用户元数据", type = MysqlTypeConstant.TEXT)
    private String userMetadata;
    /**
     * 缩略图元数据
     */
    @AutoColumn(comment = "缩略图元数据", type = MysqlTypeConstant.TEXT)
    private String thMetadata;
    /**
     * 缩略图用户元数据
     */
    @AutoColumn(comment = "缩略图用户元数据", type = MysqlTypeConstant.TEXT)
    private String thUserMetadata;
    /**
     * 附加属性
     */
    @AutoColumn(comment = "附加属性", type = MysqlTypeConstant.TEXT)
    private String attr;
    /**
     * 文件ACL
     */
    @AutoColumn(comment = "文件ACL", length = 32)
    private String fileAcl;
    /**
     * 缩略图文件ACL
     */
    @AutoColumn(comment = "缩略图文件ACL", length = 32)
    private String thFileAcl;
    /**
     * 哈希信息
     */
    @AutoColumn(comment = "哈希信息", type = MysqlTypeConstant.TEXT)
    private String hashInfo;
    /**
     * 上传ID，仅在手动分片上传时使用
     */
    @AutoColumn(comment = "上传ID，仅在手动分片上传时使用", length = 128)
    private String uploadId;
    /**
     * 上传状态，仅在手动分片上传时使用，1：初始化完成，2：上传完成
     */
    @AutoColumn(comment = "上传状态，仅在手动分片上传时使用，1：初始化完成，2：上传完成")
    private Long uploadStatus;

}

package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysFile;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 文件记录视图对象
 * @author chengliang4810
 * @since 2025-06-24
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysFile.class)
@AutoMapper(target = SysFile.class)
public class SysFileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long createBy;
    private Date createTime;

    /**
     * 文件id
     */
    private String id;
    /**
     * 文件访问地址
     */
    private String url;
    /**
     * 文件大小，单位字节
     */
    private Long size;
    /**
     * 文件名称
     */
    private String filename;
    /**
     * 原始文件名
     */
    private String originalFilename;
    /**
     * 基础存储路径
     */
    private String basePath;
    /**
     * 存储路径
     */
    private String path;
    /**
     * 文件扩展名
     */
    private String ext;
    /**
     * MIME类型
     */
    private String contentType;
    /**
     * 存储平台
     */
    private String platform;
    /**
     * 缩略图访问路径
     */
    private String thUrl;
    /**
     * 缩略图名称
     */
    private String thFilename;
    /**
     * 缩略图大小，单位字节
     */
    private Long thSize;
    /**
     * 缩略图MIME类型
     */
    private String thContentType;
    /**
     * 文件所属对象id
     */
    private String objectId;
    /**
     * 文件所属对象类型，例如用户头像，评价图片
     */
    private String objectType;
    /**
     * 文件元数据
     */
    private String metadata;
    /**
     * 文件用户元数据
     */
    private String userMetadata;
    /**
     * 缩略图元数据
     */
    private String thMetadata;
    /**
     * 缩略图用户元数据
     */
    private String thUserMetadata;
    /**
     * 附加属性
     */
    private String attr;
    /**
     * 文件ACL
     */
    private String fileAcl;
    /**
     * 缩略图文件ACL
     */
    private String thFileAcl;
    /**
     * 哈希信息
     */
    private String hashInfo;
    /**
     * 上传ID，仅在手动分片上传时使用
     */
    private String uploadId;
    /**
     * 上传状态，仅在手动分片上传时使用，1：初始化完成，2：上传完成
     */
    private Long uploadStatus;

}

package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.system.domain.SysFileDetail;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;

import static cn.xbatis.db.annotations.Condition.Type.*;

/**
 * 文件记录查询条件对象
 * @author chengliang4810
 * @since 2025-06-24
 */
@Data
@FieldNameConstants
@ConditionTarget(SysFileDetail.class)
public class SysFileDetailQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件id
     */
    @Condition(value = EQ)
    private String id;
    /**
     * 文件访问地址
     */
    @Condition(value = EQ)
    private String url;
    /**
     * 文件大小，单位字节
     */
    @Condition(value = EQ)
    private Long size;
    /**
     * 文件名称
     */
    @Condition(value = EQ)
    private String filename;
    /**
     * 原始文件名
     */
    @Condition(value = EQ)
    private String originalFilename;
    /**
     * 基础存储路径
     */
    @Condition(value = EQ)
    private String basePath;
    /**
     * 存储路径
     */
    @Condition(value = EQ)
    private String path;
    /**
     * 文件扩展名
     */
    @Condition(value = EQ)
    private String ext;
    /**
     * MIME类型
     */
    @Condition(value = EQ)
    private String contentType;
    /**
     * 存储平台
     */
    @Condition(value = EQ)
    private String platform;
    /**
     * 缩略图访问路径
     */
    @Condition(value = EQ)
    private String thUrl;
    /**
     * 缩略图名称
     */
    @Condition(value = EQ)
    private String thFilename;
    /**
     * 缩略图大小，单位字节
     */
    @Condition(value = EQ)
    private Long thSize;
    /**
     * 缩略图MIME类型
     */
    @Condition(value = EQ)
    private String thContentType;
    /**
     * 文件所属对象id
     */
    @Condition(value = EQ)
    private String objectId;
    /**
     * 文件所属对象类型，例如用户头像，评价图片
     */
    @Condition(value = EQ)
    private String objectType;
    /**
     * 文件元数据
     */
    @Condition(value = EQ)
    private String metadata;
    /**
     * 文件用户元数据
     */
    @Condition(value = EQ)
    private String userMetadata;
    /**
     * 缩略图元数据
     */
    @Condition(value = EQ)
    private String thMetadata;
    /**
     * 缩略图用户元数据
     */
    @Condition(value = EQ)
    private String thUserMetadata;
    /**
     * 附加属性
     */
    @Condition(value = EQ)
    private String attr;
    /**
     * 文件ACL
     */
    @Condition(value = EQ)
    private String fileAcl;
    /**
     * 缩略图文件ACL
     */
    @Condition(value = EQ)
    private String thFileAcl;
    /**
     * 哈希信息
     */
    @Condition(value = EQ)
    private String hashInfo;
    /**
     * 上传ID，仅在手动分片上传时使用
     */
    @Condition(value = EQ)
    private String uploadId;
    /**
     * 上传状态，仅在手动分片上传时使用，1：初始化完成，2：上传完成
     */
    @Condition(value = EQ)
    private Long uploadStatus;

    /**
     * 条件构建前执行
     */
    @Override
    public void beforeBuildCondition() {
        
    }

}

package com.jimuqu.system.controller;

import cn.hutool.core.lang.Dict;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.query.SysFileQuery;
import com.jimuqu.system.domain.vo.SysOssUploadVo;
import com.jimuqu.system.domain.vo.SysOssVo;
import com.jimuqu.system.service.SysFileService;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotEmpty;

import java.util.List;

/**
 * Bell OSS 文件管理。
 */
@Controller
@RequiredArgsConstructor
@Mapping("/resource/oss")
public class SysFileController extends BaseController {

    private final SysFileService sysFileService;
    private final FileStorageService fileStorageService;

    @Get
    @Mapping("/list")
    @SaCheckPermission("system:oss:list")
    public Page<SysOssVo> list(SysFileQuery query, PageQuery pageQuery) {
        return sysFileService.queryOssPageList(query, pageQuery);
    }

    @Get
    @Mapping("/listByIds/{ids}")
    @SaCheckPermission("system:oss:query")
    public List<SysOssVo> listByIds(@NotEmpty(message = "主键不能为空") List<String> ids) {
        return sysFileService.queryOssByIds(ids);
    }

    @Post
    @Mapping("/upload")
    @NoRepeatSubmit
    @SaCheckPermission("system:oss:upload")
    @Log(title = "OSS对象存储", businessType = BusinessType.UPLOAD)
    public SysOssUploadVo upload(UploadedFile file, String ossExt) {
        UploadPretreatment upload = fileStorageService.of(
                file.getContent(), file.getName(), file.getContentType(), file.getContentSize());
        if (ossExt != null && !ossExt.isBlank()) {
            Dict ext = JsonUtil.toObject(ossExt, Dict.class);
            ext.set("fileSize", file.getContentSize());
            ext.set("contentType", file.getContentType());
            ext.set("uploadIp", Context.current().realIp());
            upload.setAttr(ext);
        }
        FileInfo info = upload.upload();
        Assert.notNull(info, "文件上传失败");
        return new SysOssUploadVo(info.getUrl(), info.getOriginalFilename(), info.getId());
    }

    @Get
    @Mapping("/download/{id}")
    @SaCheckPermission("system:oss:download")
    public DownloadedFile download(String id) {
        return sysFileService.download(id);
    }

    @Delete
    @Mapping("/{ids}")
    @SaCheckPermission("system:oss:remove")
    @Log(title = "OSS对象存储", businessType = BusinessType.DELETE)
    public void delete(@NotEmpty(message = "主键不能为空") List<String> ids) {
        Assert.isTrue(sysFileService.deleteOssByIds(ids), "删除文件失败");
    }
}

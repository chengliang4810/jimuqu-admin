package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.query.SysFileQuery;
import com.jimuqu.system.domain.vo.SysFileVo;
import com.jimuqu.system.service.SysFileService;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;

import java.util.List;

/**
 * 文件记录 Controller
 *
 * @author chengliang4810
 * @since 2025-06-24
 */
@Post
@Controller
@RequiredArgsConstructor
@Mapping("/system/file")
public class SysFileController extends BaseController {

    private final SysFileService sysFileService;
    private final FileStorageService fileStorageService;

    /**
     * 查询文件记录列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:file:list")
    public Page<SysFileVo> list(SysFileQuery query, PageQuery pageQuery) {
        return sysFileService.queryPageList(query, pageQuery);
    }

    /**
     * 获取文件记录详细信息
     *
     * @param id 文件记录主键
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:file:detail")
    public SysFileVo getInfo(@NotNull(message = "文件记录主键不能为空") String id) {
        return sysFileService.queryById(id);
    }

    /**
     * 上传文件
     */
    @Mapping("/upload")
    @NoRepeatSubmit
    @SaCheckPermission("system:file:upload")
    @Log(title = "上传文件", businessType = BusinessType.UPLOAD)
    public FileInfo upload(UploadedFile file) {
        return fileStorageService.of(file).upload();
    }

    /**
     * 删除文件记录
     */
    @Mapping("/delete/{ids}")
    @SaCheckPermission("system:file:delete")
    @Log(title = "删除文件记录", businessType = BusinessType.DELETE)
    public Integer delete(@NotEmpty(message = "主键不能为空") List<String> ids) {
        Integer num = sysFileService.deleteByIds(ids);
        Assert.gtZero(num, "删除文件记录失败");
        return num;
    }

}
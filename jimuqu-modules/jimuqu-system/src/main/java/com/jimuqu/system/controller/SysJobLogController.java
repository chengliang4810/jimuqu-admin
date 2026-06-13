package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.query.SysJobLogQuery;
import com.jimuqu.system.domain.vo.SysJobLogVo;
import com.jimuqu.system.service.SysJobLogService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 定时任务运行日志Controller
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Post
@Controller
@RequiredArgsConstructor
@Mapping("/system/job/log")
public class SysJobLogController extends BaseController {

    private final SysJobLogService sysJobLogService;

    /**
     * 查询运行日志列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:job:log")
    public Page<SysJobLogVo> list(SysJobLogQuery query, PageQuery pageQuery) {
        return sysJobLogService.queryPageList(query, pageQuery);
    }

    /**
     * 获取运行日志详细信息
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:job:log")
    public SysJobLogVo getInfo(@NotNull(message = "运行日志主键不能为空") Long id) {
        return sysJobLogService.queryById(id);
    }

    /**
     * 删除运行日志
     */
    @Mapping("/delete/{ids}")
    @SaCheckPermission("system:job:log")
    @Log(title = "删除定时任务运行日志", businessType = BusinessType.DELETE)
    public Integer delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Integer num = sysJobLogService.deleteByIds(ids);
        Assert.gtZero(num, "删除定时任务运行日志失败");
        return num;
    }

    /**
     * 清空运行日志
     */
    @Mapping("/clear")
    @SaCheckPermission("system:job:log")
    @Log(title = "清空定时任务运行日志", businessType = BusinessType.CLEAN)
    public Integer clear() {
        return sysJobLogService.clear();
    }

    /**
     * 下载运行日志详情
     */
    @Get
    @Mapping("/download/{id}")
    @SaCheckPermission("system:job:log")
    public DownloadedFile download(@NotNull(message = "运行日志主键不能为空") Long id) {
        SysJobLogVo log = sysJobLogService.queryById(id);
        Assert.notNull(log, "运行日志不存在");
        if (StringUtil.isNotBlank(log.getResultFilePath())) {
            Path path = Paths.get(log.getResultFilePath());
            Assert.isTrue(Files.isRegularFile(path), "运行结果文件不存在");
            try {
                String contentType = StringUtil.defaultIfBlank(log.getResultContentType(), "application/octet-stream");
                String fileName = StringUtil.defaultIfBlank(log.getResultFileName(), "job-result-" + id);
                return new DownloadedFile(contentType, Files.readAllBytes(path), fileName);
            } catch (Exception e) {
                throw new IllegalStateException("下载运行结果文件失败", e);
            }
        }
        byte[] content = JsonUtil.toStringFormat(log).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new DownloadedFile("text/plain;charset=UTF-8", content, "job-log-" + id + ".txt");
    }
}

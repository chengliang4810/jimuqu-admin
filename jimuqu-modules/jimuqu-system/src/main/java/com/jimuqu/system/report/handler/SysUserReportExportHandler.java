package com.jimuqu.system.report.handler;

import cn.hutool.v7.core.collection.ListUtil;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.excel.core.LargeExcelExportRequest;
import com.jimuqu.common.excel.core.LargeExcelExportResult;
import com.jimuqu.common.excel.utils.LargeExcelExportUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.query.SysUserQuery;
import com.jimuqu.system.domain.vo.SysUserExportVo;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.job.SysJobContext;
import com.jimuqu.system.job.SysJobHandler;
import com.jimuqu.system.report.annotation.ScheduledReport;
import com.jimuqu.system.report.domain.SysUserReportExportParam;
import com.jimuqu.system.service.SysUserService;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.List;

/**
 * 系统用户报表大数据导出handler。
 */
@Component
@ScheduledReport(value = "system:user:large-export", name = "系统用户报表")
public class SysUserReportExportHandler {

    private static final int DEFAULT_PAGE_SIZE = 5_000;

    @Inject
    private SysUserService sysUserService;

    /**
     * 生成系统用户报表临时文件。
     * <p>
     * 调度任务可将返回结果的path/openInputStream/writeTo交给x-file-storage保存。
     */
    @SysJobHandler(value = "system:user:large-export", name = "系统用户报表")
    public LargeExcelExportResult export(SysJobContext context) {
        return export(parseParam(context));
    }

    public LargeExcelExportResult export(SysUserReportExportParam param) {
        SysUserReportExportParam exportParam = param == null ? new SysUserReportExportParam() : param;
        int pageSize = exportParam.getPageSize() == null ? DEFAULT_PAGE_SIZE : exportParam.getPageSize();
        SysUserQuery query = exportParam.getQuery();

        LargeExcelExportRequest.Builder<SysUserExportVo> builder = LargeExcelExportRequest.<SysUserExportVo>builder()
                .fileName("系统用户报表")
                .sheetName("用户数据")
                .head(SysUserExportVo.class)
                .pageSize(pageSize)
                .rowsPerFile(exportParam.getRowsPerFile())
                .forceZip(exportParam.getZip())
                .fetcher((page, size) -> queryPage(query, page, size));
        return LargeExcelExportUtil.exportToTempFile(builder.build());
    }

    private SysUserReportExportParam parseParam(SysJobContext context) {
        if (context == null || StringUtil.isBlank(context.getParamJson())) {
            return new SysUserReportExportParam();
        }
        SysUserReportExportParam param = JsonUtil.toObject(context.getParamJson(), SysUserReportExportParam.class);
        return param == null ? new SysUserReportExportParam() : param;
    }

    private List<SysUserExportVo> queryPage(SysUserQuery query, int page, int pageSize) {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setCurrentPage(page);
        pageQuery.setPageSize(pageSize);
        Page<SysUserVo> pageData = sysUserService.queryPageList(query, pageQuery);
        if (pageData == null || pageData.getItems() == null) {
            return ListUtil.zero();
        }
        return MapstructUtil.convert(pageData.getItems(), SysUserExportVo.class);
    }
}

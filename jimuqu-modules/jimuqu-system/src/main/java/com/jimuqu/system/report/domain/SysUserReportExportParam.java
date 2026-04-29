package com.jimuqu.system.report.domain;

import com.jimuqu.system.domain.query.SysUserQuery;

import java.io.Serial;
import java.io.Serializable;

/**
 * 系统用户大数据报表导出参数。
 */
public class SysUserReportExportParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分页查询条数。
     */
    private Integer pageSize;

    /**
     * 单个xlsx文件最大数据行数。
     */
    private Integer rowsPerFile;

    /**
     * 是否强制zip输出。
     */
    private Boolean zip;

    /**
     * 用户查询条件。
     */
    private SysUserQuery query = new SysUserQuery();

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getRowsPerFile() {
        return rowsPerFile;
    }

    public void setRowsPerFile(Integer rowsPerFile) {
        this.rowsPerFile = rowsPerFile;
    }

    public Boolean getZip() {
        return zip;
    }

    public void setZip(Boolean zip) {
        this.zip = zip;
    }

    public SysUserQuery getQuery() {
        if (query == null) {
            query = new SysUserQuery();
        }
        return query;
    }

    public void setQuery(SysUserQuery query) {
        this.query = query;
    }
}

package com.layjava.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.layjava.common.core.domain.R;
import com.layjava.common.core.utils.DateUtil;
import com.layjava.common.log.annotation.Log;
import com.layjava.common.log.enums.BusinessType;
import com.layjava.common.mybatis.core.page.PageQuery;
import com.layjava.common.mybatis.core.page.PageResult;
import com.layjava.common.mybatis.helper.DataBaseHelper;
import com.layjava.common.web.core.BaseController;
import com.layjava.generator.domain.GenTable;
import com.layjava.generator.domain.GenTableColumn;
import com.layjava.generator.service.IGenTableService;
import lombok.RequiredArgsConstructor;
import org.dromara.hutool.core.convert.ConvertUtil;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.validation.annotation.Validated;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成 操作处理
 *
 * @author Lion Li
 */
@Controller
@Mapping("/tool/gen" )
@RequiredArgsConstructor
public class GenController extends BaseController {

    private final IGenTableService genTableService;

    /**
     * 查询代码生成列表
     */
    @Get
    @Mapping("/list" )
    @SaCheckPermission("tool:gen:list" )
    public PageResult<GenTable> genList(GenTable genTable, PageQuery pageQuery) {
        return genTableService.selectPageGenTableList(genTable, pageQuery);
    }

    /**
     * 查询代码生成业务详情
     *
     * @param tableId 表ID
     */
    @Get
    @Mapping("/{tableId}" )
    @SaCheckPermission("tool:gen:query" )
    public R<Map<String, Object>> getInfo(Long tableId) {
        GenTable table = genTableService.selectGenTableById(tableId);
        List<GenTable> tables = genTableService.selectGenTableAll();
        List<GenTableColumn> list = genTableService.selectGenTableColumnListByTableId(tableId);
        Map<String, Object> result = new HashMap<>(3);
        result.put("info", table);
        result.put("rows", list);
        result.put("tables", tables);
        return R.ok(result);
    }

    /**
     * 查询数据库列表
     */
    @Get
    @Mapping("/db/list" )
    @SaCheckPermission("tool:gen:list" )
    public PageResult<GenTable> dataList(GenTable genTable, PageQuery pageQuery) {
        return genTableService.selectPageDbTableList(genTable, pageQuery);
    }

    /**
     * 查询数据表字段列表
     *
     * @param tableId 表ID
     */
    @Get
    @Mapping("/column/{tableId}" )
    @SaCheckPermission("tool:gen:list" )
    public PageResult<GenTableColumn> columnList(Long tableId) {
        List<GenTableColumn> list = genTableService.selectGenTableColumnListByTableId(tableId);
        return PageResult.build(list);
    }

    /**
     * 导入表结构（保存）
     *
     * @param tables 表名串
     */
    @Post
    @Mapping("/import/table" )
    @SaCheckPermission("tool:gen:import" )
    @Log(title = "代码生成", businessType = BusinessType.IMPORT)
    public R<Void> importTableSave(String tables, String dataName) {
        String[] tableNames = ConvertUtil.toStrArray(tables);
        // 查询表信息
        List<GenTable> tableList = genTableService.selectDbTableListByNames(tableNames, dataName);
        genTableService.importGenTable(tableList, dataName);
        return R.ok();
    }

    /**
     * 修改保存代码生成业务
     */
    @Put
    @Mapping
    @SaCheckPermission("tool:gen:edit" )
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    public R<Void> editSave(@Validated GenTable genTable) {
        genTableService.validateEdit(genTable);
        genTableService.updateGenTable(genTable);
        return R.ok();
    }

    /**
     * 删除代码生成
     *
     * @param tableIds 表ID串
     */
    @Delete
    @Mapping("/{tableIds}" )
    @SaCheckPermission("tool:gen:remove" )
    @Log(title = "代码生成", businessType = BusinessType.DELETE)
    public R<Void> remove(Long[] tableIds) {
        genTableService.deleteGenTableByIds(tableIds);
        return R.ok();
    }

    /**
     * 预览代码
     *
     * @param tableId 表ID
     */
    @Get
    @Mapping("/code/preview/{tableId}" )
    @SaCheckPermission("tool:gen:preview" )
    public R<Map<String, String>> preview(Long tableId) throws IOException {
        Map<String, String> dataMap = genTableService.previewCode(tableId);
        return R.ok(dataMap);
    }

    /**
     * 同步数据库
     *
     * @param tableId 表ID
     */
    @Get
    @Mapping("/sync/db/{tableId}" )
    @SaCheckPermission("tool:gen:edit" )
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    public R<Void> syncDb(Long tableId) {
        genTableService.synchDb(tableId);
        return R.ok();
    }

    /**
     * 批量生成代码
     *
     * @param tableIdStr 表ID串
     */
    @Get
    @Mapping("/code/{tableIdStr}" )
    @SaCheckPermission("tool:gen:code" )
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    public DownloadedFile batchGenCode(String tableIdStr) throws IOException {
        String[] tableIds = ConvertUtil.toStrArray(tableIdStr);
        byte[] data = genTableService.downloadCode(tableIds);
        return genCode(data);
    }

    /**
     * 生成zip文件
     */
    private DownloadedFile genCode(byte[] data) throws IOException {
        return new DownloadedFile("application/octet-stream", data, "code-" + DateUtil.dateTimeNow() + ".zip" );
    }

    /**
     * 查询数据源名称列表
     */
    @Get
    @Mapping(value = "/datasource/name" )
    @SaCheckPermission("tool:gen:list" )
    public R<Object> getCurrentDataSourceNameList() {
        return R.ok(DataBaseHelper.getDataSourceNameList());
    }
}

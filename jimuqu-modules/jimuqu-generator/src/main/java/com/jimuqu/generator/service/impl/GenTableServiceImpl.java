package com.jimuqu.generator.service.impl;

import cn.xbatis.core.sql.executor.Where;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.IdUtil;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.StreamUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.generator.constant.GenConstants;
import com.jimuqu.generator.domain.GenTable;
import com.jimuqu.generator.domain.GenTableColumn;
import com.jimuqu.generator.domain.GenTemplate;
import com.jimuqu.generator.domain.vo.GenTableVo;
import com.jimuqu.generator.domain.vo.GenTemplateVo;
import com.jimuqu.generator.mapper.GenTableColumnMapper;
import com.jimuqu.generator.mapper.GenTableMapper;
import com.jimuqu.generator.mapper.GenTemplateMapper;
import com.jimuqu.generator.service.IGenTableService;
import com.jimuqu.generator.util.GenUtils;
import com.jimuqu.generator.util.VelocityInitializer;
import com.jimuqu.generator.util.VelocityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.anyline.metadata.Column;
import org.anyline.metadata.Table;
import org.anyline.proxy.ServiceProxy;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.io.IoUtil;
import cn.hutool.v7.core.map.Dict;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.ObjUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;
import org.noear.solon.data.dynamicds.DynamicDs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * 业务 服务层实现
 *
 * @author Lion Li,chengliang4810
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenTableServiceImpl implements IGenTableService {

    private final GenTableMapper baseMapper;
    private final GenTemplateMapper templateMapper;
    private final GenTableColumnMapper genTableColumnMapper;

    private static final String[] TABLE_IGNORE = new String[]{"sj_", "act_", "flw_", "gen_"};

    /**
     * 查询业务字段列表
     *
     * @param tableId 业务字段编号
     * @return 业务字段集合
     */
    @Override
    public List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId) {
        return QueryChain.of(genTableColumnMapper)
                .eq(GenTableColumn::getTableId, tableId)
                .orderBy(GenTableColumn::getSort)
                .list();
    }

    /**
     * 查询业务信息
     *
     * @param id 业务ID
     * @return 业务信息
     */
    @Override
    public GenTableVo selectGenTableById(Long id) {
        GenTableVo genTable = baseMapper.selectGenTableById(id);
        setTableFromOptions(genTable);
        return genTable;
    }

    @Override
    public Page<GenTable> selectPageGenTableList(GenTable genTable, PageQuery pageQuery) {
        return buildQueryChain(genTable).paging(pageQuery.build());
    }

    /**
     * 构建查询条件
     *
     * @param genTable 查询条件对象
     * @return 查询条件对象
     */
    private QueryChain<GenTable> buildQueryChain(GenTable genTable) {
        Map<String, Object> params = genTable.getParams();
        return QueryChain.of(baseMapper)
                .forSearch(true)
                .eq(GenTable::getDataName, genTable.getDataName())
                .and(StrUtil.isNotBlank(genTable.getTableName()), GenTable::getTableName, c -> c.lower().like(genTable.getTableName().toLowerCase()))
                .and(StrUtil.isNotBlank(genTable.getTableComment()), GenTable::getTableComment, c -> c.lower().like(genTable.getTableComment().toLowerCase()))
                .between(GenTable::getCreateTime, params.get("beginTime"), params.get("endTime"));
    }

    /**
     * 查询数据库列表
     *
     * @param genTable  包含查询条件的GenTable对象
     * @param pageQuery 包含分页信息的PageQuery对象
     * @return 包含分页结果的Page对象
     */
    @Override
//    @DynamicDs("${genTable.dataName}")
    public Page<GenTable> selectPageDbTableList(GenTable genTable, PageQuery pageQuery) {
        // 获取查询条件
        String tableName = genTable.getTableName();
        String tableComment = genTable.getTableComment();
        LinkedHashMap<String, Table<?>> tablesMap = ServiceProxy.service().metadata().tables();
        if (CollUtil.isEmpty(tablesMap)) {
            return Page.of();
        }
        List<String> tableNames = baseMapper.selectTableNameList(genTable.getDataName());
        String[] tableArrays;
        if (CollUtil.isNotEmpty(tableNames)) {
            tableArrays = tableNames.toArray(new String[0]);
        } else {
            tableArrays = new String[0];
        }
        // 过滤并转换表格数据
        List<GenTable> tables = tablesMap.values().stream()
                .filter(x -> !startWithAnyIgnoreCase(x.getName(), TABLE_IGNORE))
                .filter(x -> {
                    if (CollUtil.isEmpty(tableNames)) {
                        return true;
                    }
                    return !StringUtils.equalsAnyIgnoreCase(x.getName(), tableArrays);
                })
                .filter(x -> {
                    boolean nameMatches = true;
                    boolean commentMatches = true;
                    // 进行表名称的模糊查询
                    if (StringUtils.isNotBlank(tableName)) {
                        nameMatches = StringUtils.containsIgnoreCase(x.getName(), tableName);
                    }
                    // 进行表描述的模糊查询
                    if (StringUtils.isNotBlank(tableComment)) {
                        commentMatches = StringUtils.containsIgnoreCase(x.getComment(), tableComment);
                    }
                    // 同时匹配名称和描述
                    return nameMatches && commentMatches;
                })
                .map(x -> {
                    GenTable gen = new GenTable();
                    gen.setTableName(x.getName());
                    gen.setTableComment(x.getComment());
                    gen.setCreateTime(x.getCreateTime());
                    gen.setUpdateTime(x.getUpdateTime());
                    return gen;
                }).toList();

        Page<GenTable> page = pageQuery.build();
        List<GenTable> tableList = ListUtil.page(tables, page.getCurrentPage() - 1, page.getPageSize());
        return Page.<GenTable>of(tableList, tables.size());
    }

    public static boolean startWithAnyIgnoreCase(CharSequence cs, CharSequence... searchCharSequences) {
        // 判断是否是以指定字符串开头
        for (CharSequence searchCharSequence : searchCharSequences) {
            if (StringUtils.startsWithIgnoreCase(cs, searchCharSequence)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查询据库列表
     *
     * @param tableNames 表名称组
     * @param dataName   数据源名称
     * @return 数据库表集合
     */
    @Override
//    @DynamicDs("${dataName}")
    public List<GenTableVo> selectDbTableListByNames(List<String> tableNames, String dataName) {
        Set<String> tableNameSet = new HashSet<>(tableNames);
        LinkedHashMap<String, Table<?>> tablesMap = ServiceProxy.service().metadata().tables();

        if (CollUtil.isEmpty(tablesMap)) {
            return new ArrayList<>();
        }

        List<Table<?>> tableList = tablesMap.values().stream()
                .filter(x -> !cn.hutool.core.util.StrUtil.containsAnyIgnoreCase(x.getName(), TABLE_IGNORE))
                .filter(x -> tableNameSet.contains(x.getName())).toList();

        if (CollUtil.isEmpty(tableList)) {
            return new ArrayList<>();
        }
        return tableList.stream().map(x -> {
            GenTableVo gen = new GenTableVo();
            gen.setDataName(dataName);
            gen.setTableName(x.getName());
            gen.setTableComment(x.getComment());
            gen.setCreateTime(x.getCreateTime());
            gen.setUpdateTime(x.getUpdateTime());
            return gen;
        }).toList();
    }

    /**
     * 查询所有表信息
     *
     * @return 表信息集合
     */
    @Override
    public List<GenTable> selectGenTableAll() {
        return baseMapper.selectGenTableAll();
    }

    /**
     * 修改业务
     *
     * @param genTable 业务信息
     */
    @Override
    @Transaction
    public void updateGenTable(GenTable genTable) {
        String options = JsonUtil.toJsonString(genTable.getParams());
        genTable.setOptions(options);
        int row = baseMapper.update(genTable);
        if (row > 0) {
            for (GenTableColumn cenTableColumn : genTable.getColumns()) {
                genTableColumnMapper.update(cenTableColumn);
            }
        }
    }

    /**
     * 删除业务对象
     *
     * @param tableIds 需要删除的数据ID
     */
    @Override
    @Transaction
    public int deleteGenTableByIds(Long[] tableIds) {
        List<Long> ids = Arrays.asList(tableIds);
        int deleteRows = baseMapper.deleteByIds(ids);
        genTableColumnMapper.delete(Where.create().in(GenTableColumn::getTableId, ids));
        return deleteRows;
    }

    /**
     * 导入表结构
     *
     * @param tableList 导入表列表
     * @param dataName  数据源名称
     */
    @Transaction
    @Override
    public void importGenTable(List<GenTableVo> tableList, String dataName) {
        Long operId = LoginHelper.getUserId();
        try {
            for (GenTableVo table : tableList) {
                String tableName = table.getTableName();
                GenUtils.initTable(table, operId);
                table.setDataName(dataName);
                GenTable genTable = MapstructUtil.convert(table, GenTable.class);
                int row = baseMapper.save(genTable);
                if (row > 0) {
                    table.setId(genTable.getId());
                    // 保存列信息
                    List<GenTableColumn> genTableColumns = this.selectDbTableColumnsByName(tableName, dataName);
                    List<GenTableColumn> saveColumns = new ArrayList<>();
                    for (GenTableColumn column : genTableColumns) {
                        GenUtils.initColumnField(column, table);
                        saveColumns.add(column);
                    }
                    if (CollUtil.isNotEmpty(saveColumns)) {
                        genTableColumnMapper.saveBatch(saveColumns);
                    }
                }
            }
        } catch (Exception e) {
            throw new ServiceException("导入失败：" + e.getMessage());
        }
    }

    /**
     * 根据表名称查询列信息
     *
     * @param tableName 表名称
     * @param dataName  数据源名称
     * @return 列信息
     */
    @Override
    @DynamicDs("${dataName}")
    public List<GenTableColumn> selectDbTableColumnsByName(String tableName, String dataName) {
        // .service(dataName)
        LinkedHashMap<String, Column> columns = ServiceProxy.metadata().columns(tableName);
        List<GenTableColumn> tableColumns = new ArrayList<>();
        columns.forEach((columnName, column) -> {
            GenTableColumn tableColumn = new GenTableColumn();
            tableColumn.setIsPk(column.isPrimaryKey() ? "1" : "0");
            tableColumn.setColumnName(column.getName());
            tableColumn.setColumnComment(column.getComment());
            tableColumn.setLength(column.getLength());
            tableColumn.setDefaultValue(Optional.ofNullable(column.getDefaultValue()).map(StrUtil::toString).orElse(""));
            tableColumn.setColumnType(column.getTypeName().toLowerCase());
            tableColumn.setSort(column.getPosition());
            tableColumn.setIsRequired(column.isNullable() ? "0" : "1");
            tableColumn.setIsIncrement(column.isAutoIncrement() ? "1" : "0");
            tableColumns.add(tableColumn);
        });
        return tableColumns;
    }

    /**
     * 预览代码
     *
     * @param tableId 表编号
     * @return 预览数据列表
     */
    @Override
    public Map<String, String> previewCode(Long tableId) {
        Map<String, String> dataMap = new LinkedHashMap<>();
        // 查询表信息
        GenTableVo table = baseMapper.selectGenTableById(tableId);
        List<Long> menuIds = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            menuIds.add(IdUtil.getId());
        }
        table.setMenuIds(menuIds);
        // 设置主键列信息
        setPkColumn(table);
        VelocityInitializer.initVelocity();
        // 获取模板列表
        StringResourceRepository repository = StringResourceLoader.getRepository();

        VelocityContext context = VelocityUtils.prepareContext(table);

        List<GenTemplate> genTemplateList = templateMapper.list(Where.create().eq(GenTemplate::getEnable, true));
        for (GenTemplate genTemplate : genTemplateList) {
            String templateName = genTemplate.getName() + ".vm";
            // 获取字符串资源库
            repository.putStringResource(templateName, genTemplate.getContent());
            // 获取模板
            Template tpl = Velocity.getTemplate(templateName);
            // 渲染模板
            StringWriter sw = new StringWriter();
            tpl.merge(context, sw);
            dataMap.put(templateName, sw.toString());
        }
        return dataMap;
    }

    /**
     * 生成代码（下载方式）
     *
     * @param tableId 表名称
     * @return 数据
     */
    @Override
    public byte[] downloadCode(Long tableId) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        generatorCode(tableId, zip);
        IoUtil.closeIfPossible(zip);
        return outputStream.toByteArray();
    }

    /**
     * 同步数据库
     *
     * @param tableId 表名称
     */
    @Transaction
    @Override
    public void synchDb(Long tableId) {
        GenTableVo table = baseMapper.selectGenTableById(tableId);
        List<GenTableColumn> tableColumns = table.getColumns();
        Map<String, GenTableColumn> tableColumnMap = StreamUtil.toIdentityMap(tableColumns, GenTableColumn::getColumnName);

        List<GenTableColumn> dbTableColumns = this.selectDbTableColumnsByName(table.getTableName(), table.getDataName());
        if (CollUtil.isEmpty(dbTableColumns)) {
            throw new ServiceException("同步数据失败，原表结构不存在");
        }
        List<String> dbTableColumnNames = StreamUtil.toList(dbTableColumns, GenTableColumn::getColumnName);

        List<GenTableColumn> saveColumns = new ArrayList<>();
        dbTableColumns.forEach(column -> {
            GenUtils.initColumnField(column, table);
            if (tableColumnMap.containsKey(column.getColumnName())) {
                GenTableColumn prevColumn = tableColumnMap.get(column.getColumnName());
                column.setId(prevColumn.getId());
                if (column.isList()) {
                    // 如果是列表，继续保留查询方式/字典类型选项
                    column.setDictType(prevColumn.getDictType());
                    column.setQueryType(prevColumn.getQueryType());
                }
                if (StringUtils.isNotEmpty(prevColumn.getIsRequired()) && !column.isPk()
                        && (column.isInsert() || column.isEdit())
                        && ((column.isUsableColumn()) || (!column.isSuperColumn()))) {
                    // 如果是(新增/修改&非主键/非忽略及父属性)，继续保留必填/显示类型选项
                    column.setIsRequired(prevColumn.getIsRequired());
                    column.setHtmlType(prevColumn.getHtmlType());
                }
            }
            saveColumns.add(column);
        });

        if (CollUtil.isNotEmpty(saveColumns)) {
            saveColumns.forEach(genTableColumnMapper::saveOrUpdate);
        }

        List<GenTableColumn> delColumns = StreamUtil.filter(tableColumns, column -> !dbTableColumnNames.contains(column.getColumnName()));
        if (CollUtil.isNotEmpty(delColumns)) {
            List<Long> ids = StreamUtil.toList(delColumns, GenTableColumn::getId);
            if (CollUtil.isNotEmpty(ids)) {
                genTableColumnMapper.deleteByIds(ids);
            }
        }
    }

    /**
     * 批量生成代码（下载方式）
     *
     * @param tableIds 表ID数组
     * @return 数据
     */
    @Override
    public byte[] downloadCode(List<String> tableIds) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        for (String tableId : tableIds) {
            generatorCode(Long.parseLong(tableId), zip);
        }
        IoUtil.closeIfPossible(zip);
        return outputStream.toByteArray();
    }

    /**
     * 查询表信息并生成代码
     */
    private void generatorCode(Long tableId, ZipOutputStream zip) {
        // 查询表信息
        GenTableVo table = baseMapper.selectGenTableById(tableId);
        List<Long> menuIds = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            menuIds.add(IdUtil.getId());
        }
        table.setMenuIds(menuIds);
        // 设置主键列信息
        setPkColumn(table);

        VelocityInitializer.initVelocity();

        StringResourceRepository repository = StringResourceLoader.getRepository();
        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<GenTemplateVo> genTemplateList = templateMapper.selectEnableList();

        System.out.println("gentemplateList;   " + genTemplateList);

        for (GenTemplateVo genTemplate : genTemplateList) {
            String templateName = genTemplate.getName() + ".vm";
            // 获取字符串资源库
            repository.putStringResource(templateName, genTemplate.getContent());
            // 获取模板
            Template tpl = Velocity.getTemplate(templateName);
            // 渲染模板
            StringWriter sw = new StringWriter();
            tpl.merge(context, sw);
            try {
                // 添加到zip
                zip.putNextEntry(new ZipEntry(VelocityUtils.getFileName(genTemplate.getPath(), table)));
                IoUtil.writeUtf8(zip, false, sw.toString());
                IoUtil.closeIfPossible(sw);
                zip.flush();
                zip.closeEntry();
            } catch (IOException e) {
                log.error("渲染模板失败，表名：" + table.getTableName(), e);
            }
        }
    }

    /**
     * 设置主键列信息
     *
     * @param table 业务表信息
     */
    public void setPkColumn(GenTableVo table) {
        for (GenTableColumn column : table.getColumns()) {
            if (column.isPk()) {
                table.setPkColumn(column);
                break;
            }
        }
        if (ObjUtil.isNull(table.getPkColumn())) {
            table.setPkColumn(table.getColumns().get(0));
        }

    }

    /**
     * 设置代码生成其他选项值
     *
     * @param genTable 设置后的生成对象
     */
    public void setTableFromOptions(GenTableVo genTable) {
        Dict paramsObj = JsonUtil.parseMap(genTable.getOptions());
        if (ObjUtil.isNotNull(paramsObj)) {
            String treeCode = paramsObj.getStr(GenConstants.TREE_CODE);
            String treeParentCode = paramsObj.getStr(GenConstants.TREE_PARENT_CODE);
            String treeName = paramsObj.getStr(GenConstants.TREE_NAME);
            Long parentMenuId = paramsObj.getLong(GenConstants.PARENT_MENU_ID);
            String parentMenuName = paramsObj.getStr(GenConstants.PARENT_MENU_NAME);

            genTable.setTreeCode(treeCode);
            genTable.setTreeParentCode(treeParentCode);
            genTable.setTreeName(treeName);
            genTable.setParentMenuId(parentMenuId);
            genTable.setParentMenuName(parentMenuName);
        }
    }

    /**
     * 获取代码生成地址
     *
     * @param table    业务表信息
     * @param template 模板文件路径
     * @return 生成地址
     */
    public static String getGenPath(GenTableVo table, String template) {
        String genPath = table.getGenPath();
        if (StringUtils.equals(genPath, "/")) {
            return System.getProperty("user.dir") + File.separator + "src" + File.separator + VelocityUtils.getFileName(template, table);
        }
        return genPath + File.separator + VelocityUtils.getFileName(template, table);
    }

}


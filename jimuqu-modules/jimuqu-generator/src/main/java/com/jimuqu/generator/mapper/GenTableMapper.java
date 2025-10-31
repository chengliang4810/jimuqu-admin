package com.jimuqu.generator.mapper;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.generator.domain.GenTable;
import com.jimuqu.generator.domain.GenTableColumn;
import com.jimuqu.generator.domain.vo.GenTableVo;
import org.noear.solon.data.dynamicds.DynamicDs;

import java.util.List;

/**
 * 业务 数据层
 *
 * @author Lion Li,chengliang4810
 */
public interface GenTableMapper extends BaseMapperPlus<GenTable, GenTable> {

    /**
     * 查询所有表信息
     *
     * @return 表信息集合
     */
    default List<GenTable> selectGenTableAll(){
        return QueryChain.of(this)
                .leftJoin(GenTable::getId, GenTableColumn::getTableId)
                .list();
    }

    /**
     * 查询表ID业务信息
     *
     * @param id 业务ID
     * @return 业务信息
     */
    default GenTableVo selectGenTableById(Long id) {
        return QueryChain.of(this)
                .leftJoin(GenTable::getId, GenTableColumn::getTableId)
                .eq(GenTable::getId, id)
                .limit(-1)
                .returnType(GenTableVo.class)
                .get();
    };

    /**
     * 查询表名称业务信息
     *
     * @param tableName 表名称
     * @return 业务信息
     */
    GenTable selectGenTableByName(String tableName);

    /**
     * 查询指定数据源下的所有表名列表
     *
     * @param dataName 数据源名称，用于选择不同的数据源
     * @return 当前数据库中的表名列表
     *
     * @DS("") 使用默认数据源执行查询操作
     */
    @DynamicDs
    List<String> selectTableNameList(String dataName);

}

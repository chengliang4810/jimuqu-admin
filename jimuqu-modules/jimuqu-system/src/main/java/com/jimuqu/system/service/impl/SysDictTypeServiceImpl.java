package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.service.DictService;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysDictType;
import com.jimuqu.system.domain.SysDictData;
import com.jimuqu.system.domain.bo.SysDictTypeBo;
import com.jimuqu.system.domain.vo.SysDictTypeVo;
import com.jimuqu.system.domain.query.SysDictTypeQuery;
import com.jimuqu.system.mapper.SysDictTypeMapper;
import com.jimuqu.system.mapper.SysDictDataMapper;
import com.jimuqu.system.service.SysDictTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;


/**
 * 字典类型Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysDictTypeServiceImpl implements SysDictTypeService, DictService {

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictDataMapper sysDictDataMapper;

    /**
     * 查询字典类型
     */
    @Override
    public SysDictTypeVo queryById(Long id) {
        return sysDictTypeMapper.getVoById(id);
    }

    /**
     * 查询字典类型分页列表
     */
    @Override
    public Page<SysDictTypeVo> queryPageList(SysDictTypeQuery query, PageQuery pageQuery) {
        return buildQueryChain(query)
                .returnType(SysDictTypeVo.class)
                .paging(pageQuery.build());
    }

    /**
     * 查询字典类型列表
     */
    @Override
    public List<SysDictTypeVo> queryList(SysDictTypeQuery query) {
        QueryChain<SysDictType> queryChain = buildQueryChain(query);
        return queryChain.returnType(SysDictTypeVo.class).list();
    }

    /**
     * 构建查询条件
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysDictType> buildQueryChain(SysDictTypeQuery query) {
        return QueryChain.of(sysDictTypeMapper)
                .forSearch(true)
                .where(query);
    }

    /**
     * 新增字典类型
     */
    @Override
    public Boolean insertByBo(SysDictTypeBo bo) {
        SysDictType sysDictType = MapstructUtil.convert(bo, SysDictType.class);
        boolean flag = sysDictTypeMapper.save(sysDictType) > 0;
        bo.setDictId(sysDictType.getDictId());
        return flag;
    }

    /**
     * 修改字典类型
     */
    @Override
    public Boolean updateByBo(SysDictTypeBo bo) {
        SysDictType sysDictType = MapstructUtil.convert(bo, SysDictType.class);
        return sysDictTypeMapper.update(sysDictType) > 0;
    }

    /**
     * 批量删除字典类型
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        return sysDictTypeMapper.deleteByIds(ids);
    }

    @Override
    public String getDictLabel(String dictType, String dictValue, String separator) {
        if (dictType == null || dictValue == null) {
            return "";
        }

        // 处理多个字典值的情况
        if (dictValue.contains(separator)) {
            String[] values = dictValue.split(separator);
            StringBuilder result = new StringBuilder();
            for (String value : values) {
                String label = getSingleDictLabel(dictType, value.trim());
                if (result.length() > 0) {
                    result.append(separator);
                }
                result.append(label);
            }
            return result.toString();
        }

        return getSingleDictLabel(dictType, dictValue);
    }

    @Override
    public String getDictValue(String dictType, String dictLabel, String separator) {
        if (dictType == null || dictLabel == null) {
            return "";
        }

        // 处理多个字典标签的情况
        if (dictLabel.contains(separator)) {
            String[] labels = dictLabel.split(separator);
            StringBuilder result = new StringBuilder();
            for (String label : labels) {
                String value = getSingleDictValue(dictType, label.trim());
                if (result.length() > 0) {
                    result.append(separator);
                }
                result.append(value);
            }
            return result.toString();
        }

        return getSingleDictValue(dictType, dictLabel);
    }

    @Override
    public Map<String, String> getAllDictByDictType(String dictType) {
        if (dictType == null) {
            return Map.of();
        }

        List<SysDictData> dictDataList = QueryChain.of(sysDictDataMapper)
                .eq(SysDictData::getDictTypeKey, dictType)
                .list();

        return dictDataList.stream()
                .collect(Collectors.toMap(
                        SysDictData::getDictValue,
                        SysDictData::getDictLabel,
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 根据字典类型和字典值获取单个字典标签
     */
    private String getSingleDictLabel(String dictType, String dictValue) {
        SysDictData dictData = QueryChain.of(sysDictDataMapper)
                .eq(SysDictData::getDictTypeKey, dictType)
                .eq(SysDictData::getDictValue, dictValue)
                .get();
        return dictData != null ? dictData.getDictLabel() : "";
    }

    /**
     * 根据字典类型和字典标签获取单个字典值
     */
    private String getSingleDictValue(String dictType, String dictLabel) {
        SysDictData dictData = QueryChain.of(sysDictDataMapper)
                .eq(SysDictData::getDictTypeKey, dictType)
                .eq(SysDictData::getDictLabel, dictLabel)
                .get();
        return dictData != null ? dictData.getDictValue() : "";
    }
}

package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import cn.hutool.v7.core.util.ObjUtil;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysConfig;
import com.jimuqu.system.domain.bo.SysConfigBo;
import com.jimuqu.system.domain.vo.SysConfigVo;
import com.jimuqu.system.domain.query.SysConfigQuery;
import com.jimuqu.system.mapper.SysConfigMapper;
import com.jimuqu.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.List;


/**
 * 参数配置Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    /**
     * 查询参数配置
     */
    @Override
    public SysConfigVo queryById(Long id) {
        return sysConfigMapper.getVoById(id);
    }

    /**
     * 查询参数配置分页列表
     */
    @Override
    public Page<SysConfigVo> queryPageList(SysConfigQuery query, PageQuery pageQuery) {
        return buildQueryChain(query)
                .returnType(SysConfigVo.class)
                .paging(pageQuery.build());
    }

    /**
     * 查询参数配置列表
     */
    @Override
    public List<SysConfigVo> queryList(SysConfigQuery query) {
        QueryChain<SysConfig> queryChain = buildQueryChain(query);
        return queryChain.returnType(SysConfigVo.class).list();
    }

    /**
     * 构建查询条件
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysConfig> buildQueryChain(SysConfigQuery query) {
        return QueryChain.of(sysConfigMapper)
                .forSearch(true)
                .where(query);
    }

    /**
     * 新增参数配置
     */
    @Override
    public Boolean insertByBo(SysConfigBo bo) {
        SysConfig sysConfig = MapstructUtil.convert(bo, SysConfig.class);
        boolean flag = sysConfigMapper.save(sysConfig) > 0;
        bo.setId(sysConfig.getId());
        return flag;
    }

    /**
     * 修改参数配置
     */
    @Override
    public Boolean updateByBo(SysConfigBo bo) {
        SysConfig sysConfig = MapstructUtil.convert(bo, SysConfig.class);
        return sysConfigMapper.update(sysConfig) > 0;
    }

    @Override
    public Boolean updateByKey(SysConfigBo bo) {
        SysConfig current = QueryChain.of(sysConfigMapper)
                .eq(SysConfig::getConfigKey, bo.getConfigKey())
                .get();
        if (current == null) {
            return false;
        }
        bo.setId(current.getId());
        return updateByBo(bo);
    }

    @Override
    public boolean selectRegisterEnabled() {
        SysConfig config = QueryChain.of(sysConfigMapper)
                .eq(SysConfig::getConfigKey, "sys.account.registerUser")
                .select(SysConfig::getConfigValue)
                .get();
        return config != null && Boolean.parseBoolean(config.getConfigValue());
    }

    @Override
    public boolean checkConfigKeyUnique(SysConfigBo bo) {
        return !QueryChain.of(sysConfigMapper)
                .eq(SysConfig::getConfigKey, bo.getConfigKey())
                .ne(ObjUtil.isNotNull(bo.getId()), SysConfig::getId, bo.getId())
                .exists();
    }

    /**
     * 批量删除参数配置
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        return sysConfigMapper.deleteByIds(ids);
    }
}

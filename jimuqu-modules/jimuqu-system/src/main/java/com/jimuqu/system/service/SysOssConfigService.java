package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysOssConfig;
import com.jimuqu.system.domain.bo.SysOssConfigBo;
import com.jimuqu.system.domain.query.SysOssConfigQuery;
import com.jimuqu.system.domain.vo.SysOssConfigVo;
import com.jimuqu.system.mapper.SysOssConfigMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.List;

/**
 * 对象存储配置服务。
 */
@Component
@RequiredArgsConstructor
public class SysOssConfigService {

    private final SysOssConfigMapper mapper;

    public Page<SysOssConfigVo> queryPage(SysOssConfigQuery query, PageQuery pageQuery) {
        return QueryChain.of(mapper)
                .forSearch(true)
                .where(query)
                .orderBy(SysOssConfig::getOssConfigId)
                .returnType(SysOssConfigVo.class)
                .paging(pageQuery.build());
    }

    public SysOssConfigVo queryById(Long id) {
        return mapper.getVoById(id);
    }

    @Transaction
    public int insert(SysOssConfigBo bo) {
        assertConfigKeyUnique(bo);
        SysOssConfig entity = toEntity(bo);
        if (entity.getStatus() == null) {
            entity.setStatus("N");
        }
        if ("Y".equals(entity.getStatus())) {
            disableCurrentDefault();
        }
        int rows = mapper.save(entity);
        bo.setOssConfigId(entity.getOssConfigId());
        return rows;
    }

    @Transaction
    public int update(SysOssConfigBo bo) {
        assertConfigKeyUnique(bo);
        if ("Y".equals(bo.getStatus())) {
            disableCurrentDefault();
        }
        return mapper.update(toEntity(bo));
    }

    public int delete(List<Long> ids) {
        return mapper.deleteByIds(ids);
    }

    @Transaction
    public int changeStatus(SysOssConfigBo bo) {
        if ("Y".equals(bo.getStatus())) {
            disableCurrentDefault();
        }
        return mapper.update(new SysOssConfig()
                        .setOssConfigId(bo.getOssConfigId())
                        .setStatus(bo.getStatus()));
    }

    private void disableCurrentDefault() {
        mapper.update(new SysOssConfig().setStatus("N"),
                where -> where.eq(SysOssConfig::getStatus, "Y"));
    }

    private void assertConfigKeyUnique(SysOssConfigBo bo) {
        boolean exists = QueryChain.of(mapper)
                .where(where -> where.eq(SysOssConfig::getConfigKey, bo.getConfigKey())
                        .ne(bo.getOssConfigId() != null, SysOssConfig::getOssConfigId, bo.getOssConfigId()))
                .exists();
        Assert.isFalse(exists, "配置key已存在: " + bo.getConfigKey());
    }

    private SysOssConfig toEntity(SysOssConfigBo bo) {
        return new SysOssConfig()
                .setOssConfigId(bo.getOssConfigId())
                .setConfigKey(bo.getConfigKey())
                .setAccessKey(bo.getAccessKey())
                .setSecretKey(bo.getSecretKey())
                .setBucketName(bo.getBucketName())
                .setPrefix(bo.getPrefix())
                .setEndpoint(bo.getEndpoint())
                .setDomainUrl(bo.getDomainUrl())
                .setIsHttps(bo.getIsHttps())
                .setRegion(bo.getRegion())
                .setStatus(bo.getStatus())
                .setExt1(bo.getExt1())
                .setRemark(bo.getRemark())
                .setAccessPolicy(bo.getAccessPolicy());
    }
}

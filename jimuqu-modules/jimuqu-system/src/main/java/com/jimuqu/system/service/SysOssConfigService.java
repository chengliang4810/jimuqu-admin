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
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.MinioFileStorage;
import org.dromara.x.file.storage.core.platform.MinioFileStorageClientFactory;
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
    private final FileStorageService fileStorageService;

    public void initPlatforms() {
        QueryChain.of(mapper).list().forEach(config -> {
            registerPlatform(config);
            if ("Y".equals(config.getStatus()) && fileStorageService.getFileStorage(config.getConfigKey()) != null) {
                fileStorageService.getProperties().setDefaultPlatform(config.getConfigKey());
            }
        });
    }

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
        if (rows > 0) {
            registerPlatform(entity);
        }
        bo.setOssConfigId(entity.getOssConfigId());
        return rows;
    }

    @Transaction
    public int update(SysOssConfigBo bo) {
        assertConfigKeyUnique(bo);
        SysOssConfig old = mapper.getById(bo.getOssConfigId());
        if ("Y".equals(bo.getStatus())) {
            disableCurrentDefault();
        }
        SysOssConfig entity = toEntity(bo);
        int rows = mapper.update(entity);
        if (rows > 0) {
            if (old != null && !old.getConfigKey().equals(entity.getConfigKey())) {
                removePlatform(old.getConfigKey());
            }
            removePlatform(entity.getConfigKey());
            registerPlatform(entity);
        }
        return rows;
    }

    public int delete(List<Long> ids) {
        List<String> configKeys = QueryChain.of(mapper)
                .select(SysOssConfig::getConfigKey)
                .where(where -> where.in(SysOssConfig::getOssConfigId, ids))
                .list().stream().map(SysOssConfig::getConfigKey).toList();
        int rows = mapper.deleteByIds(ids);
        if (rows > 0) {
            configKeys.forEach(this::removePlatform);
        }
        return rows;
    }

    @Transaction
    public int changeStatus(SysOssConfigBo bo) {
        if ("Y".equals(bo.getStatus())) {
            SysOssConfig config = mapper.getById(bo.getOssConfigId());
            Assert.notNull(config, "存储配置不存在");
            registerPlatform(config);
            Assert.notNull(fileStorageService.getFileStorage(config.getConfigKey()),
                    "存储平台未注册: " + config.getConfigKey());
            disableCurrentDefault();
        }
        int rows = mapper.update(new SysOssConfig()
                        .setOssConfigId(bo.getOssConfigId())
                        .setStatus(bo.getStatus()));
        if (rows > 0 && "Y".equals(bo.getStatus())) {
            SysOssConfig config = mapper.getById(bo.getOssConfigId());
            fileStorageService.getProperties().setDefaultPlatform(config.getConfigKey());
        }
        return rows;
    }

    private void disableCurrentDefault() {
        mapper.update(new SysOssConfig().setStatus("N"),
                where -> where.eq(SysOssConfig::getStatus, "Y"));
    }

    private void registerPlatform(SysOssConfig config) {
        if (config.getEndpoint() == null || config.getEndpoint().isBlank()
                || fileStorageService.getFileStorage(config.getConfigKey()) != null) {
            return;
        }
        FileStorageProperties.MinioConfig properties = new FileStorageProperties.MinioConfig();
        properties.setPlatform(config.getConfigKey());
        properties.setAccessKey(config.getAccessKey());
        properties.setSecretKey(config.getSecretKey());
        properties.setEndPoint(endpoint(config));
        properties.setBucketName(config.getBucketName());
        properties.setDomain(config.getDomainUrl());
        properties.setBasePath(config.getPrefix());
        fileStorageService.getFileStorageList().add(
                new MinioFileStorage(properties, new MinioFileStorageClientFactory(properties)));
    }

    private String endpoint(SysOssConfig config) {
        if (config.getEndpoint().startsWith("http://") || config.getEndpoint().startsWith("https://")) {
            return config.getEndpoint();
        }
        return ("Y".equals(config.getIsHttps()) ? "https://" : "http://") + config.getEndpoint();
    }

    private void removePlatform(String configKey) {
        FileStorage platform = fileStorageService.getFileStorage(configKey);
        if (platform != null && fileStorageService.getFileStorageList().remove(platform)) {
            platform.close();
        }
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

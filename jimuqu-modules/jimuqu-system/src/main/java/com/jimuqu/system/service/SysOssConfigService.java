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
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.platform.AmazonS3V2FileStorage;
import org.dromara.x.file.storage.core.platform.AmazonS3V2FileStorageClientFactory.AmazonS3V2Client;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.FileStorageClientFactory;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 对象存储配置服务。
 */
@Component
@RequiredArgsConstructor
public class SysOssConfigService {

    static final Set<Long> SYSTEM_CONFIG_IDS = Set.of(1761900000000000001L);
    private static final String DEFAULT_REGION = "us-east-1";
    private static final List<String> CLOUD_SERVICE_MARKERS = List.of("aliyun", "qcloud", "qiniu", "obs");

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
        QueryChain<SysOssConfig> chain = QueryChain.of(mapper)
                .forSearch(true)
                .where(query)
                .orderBy(SysOssConfig::getOssConfigId);
        return pageQuery.applyOrder(chain)
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
            if ("Y".equals(entity.getStatus())) {
                useDefaultPlatform(entity);
            }
        }
        bo.setOssConfigId(entity.getOssConfigId());
        return rows;
    }

    @Transaction
    public int update(SysOssConfigBo bo) {
        assertConfigKeyUnique(bo);
        SysOssConfig old = Assert.notNull(mapper.getById(bo.getOssConfigId()), "存储配置不存在");
        if ("Y".equals(bo.getStatus())) {
            disableCurrentDefault();
        }
        SysOssConfig entity = toEntity(bo);
        clearOptionalFields(entity, bo);
        int rows = mapper.update(entity);
        if (rows > 0) {
            if (old != null && !old.getConfigKey().equals(entity.getConfigKey())) {
                removePlatform(old.getConfigKey());
            }
            removePlatform(entity.getConfigKey());
            registerPlatform(entity);
            if ("Y".equals(entity.getStatus())) {
                useDefaultPlatform(entity);
            }
        }
        return rows;
    }

    public int delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Assert.isFalse(ids.stream().anyMatch(java.util.Objects::isNull), "存储配置ID不能为空");
        List<Long> requested = ids.stream().distinct().toList();
        Assert.isFalse(requested.stream().anyMatch(SYSTEM_CONFIG_IDS::contains), "系统内置, 不可删除!");
        List<SysOssConfig> configs = QueryChain.of(mapper)
                .where(where -> where.in(SysOssConfig::getOssConfigId, requested))
                .list();
        Assert.isTrue(configs.size() == requested.size(), "存储配置不存在");
        List<String> configKeys = configs.stream().map(SysOssConfig::getConfigKey).toList();
        int rows = mapper.deleteByIds(requested);
        if (rows > 0) {
            configKeys.forEach(this::removePlatform);
        }
        return rows;
    }

    @Transaction
    public int changeStatus(SysOssConfigBo bo) {
        SysOssConfig config = mapper.getById(bo.getOssConfigId());
        Assert.notNull(config, "存储配置不存在");
        registerPlatform(config);
        Assert.notNull(fileStorageService.getFileStorage(config.getConfigKey()),
                "存储平台未注册: " + config.getConfigKey());
        disableCurrentDefault();
        int rows = mapper.update(new SysOssConfig()
                        .setOssConfigId(bo.getOssConfigId())
                        .setStatus(bo.getStatus()));
        if (rows > 0) {
            useDefaultPlatform(config);
        }
        return rows;
    }

    private void disableCurrentDefault() {
        mapper.update(new SysOssConfig().setStatus("N"),
                where -> where.eq(SysOssConfig::getStatus, "Y"));
    }

    private void useDefaultPlatform(SysOssConfig config) {
        Assert.notNull(fileStorageService.getFileStorage(config.getConfigKey()),
                "存储平台未注册: " + config.getConfigKey());
        fileStorageService.getProperties().setDefaultPlatform(config.getConfigKey());
    }

    private void registerPlatform(SysOssConfig config) {
        if (config.getEndpoint() == null || config.getEndpoint().isBlank()
                || fileStorageService.getFileStorage(config.getConfigKey()) != null) {
            return;
        }
        fileStorageService.getFileStorageList().add(createS3Platform(config));
    }

    static AmazonS3V2FileStorage createS3Platform(SysOssConfig config) {
        String endpoint = endpoint(config);
        String region = config.getRegion() == null || config.getRegion().isBlank()
                ? DEFAULT_REGION : config.getRegion().trim();
        boolean pathStyleAccess = usePathStyleAccess(config);
        FileStorageProperties.AmazonS3V2Config properties = new FileStorageProperties.AmazonS3V2Config();
        properties.setPlatform(config.getConfigKey());
        properties.setAccessKey(config.getAccessKey());
        properties.setSecretKey(config.getSecretKey());
        properties.setRegion(region);
        properties.setEndPoint(endpoint);
        properties.setBucketName(config.getBucketName());
        properties.setDomain(bucketUrl(config, pathStyleAccess));
        properties.setBasePath(basePath(config.getPrefix()));
        properties.setDefaultAcl(defaultAcl(config.getAccessPolicy()));
        return new AmazonS3V2FileStorage(properties,
                new BellS3ClientFactory(properties, pathStyleAccess, presignerEndpoint(config)));
    }

    static boolean usePathStyleAccess(SysOssConfig config) {
        String endpoint = config.getEndpoint() == null ? "" : config.getEndpoint().toLowerCase(Locale.ROOT);
        return CLOUD_SERVICE_MARKERS.stream().noneMatch(endpoint::contains);
    }

    static String defaultAcl(String accessPolicy) {
        if (accessPolicy == null) {
            return null;
        }
        return switch (accessPolicy) {
            case "0" -> Constant.ACL.PRIVATE;
            case "1" -> Constant.ACL.PUBLIC_READ_WRITE;
            case "2" -> Constant.ACL.PUBLIC_READ;
            default -> null;
        };
    }

    private static String endpoint(SysOssConfig config) {
        return normalizeUrl(config, config.getEndpoint());
    }

    private static String presignerEndpoint(SysOssConfig config) {
        String value = config.getDomainUrl() == null || config.getDomainUrl().isBlank()
                ? config.getEndpoint() : config.getDomainUrl();
        return normalizeUrl(config, value);
    }

    private static String bucketUrl(SysOssConfig config, boolean pathStyleAccess) {
        String base = stripTrailingSlash(presignerEndpoint(config));
        String protocol = "Y".equals(config.getIsHttps()) ? "https://" : "http://";
        String address = removeProtocol(base);
        return pathStyleAccess
                ? protocol + address + "/" + config.getBucketName() + "/"
                : protocol + config.getBucketName() + "." + address + "/";
    }

    private static String normalizeUrl(SysOssConfig config, String value) {
        return ("Y".equals(config.getIsHttps()) ? "https://" : "http://") + removeProtocol(value);
    }

    private static String removeProtocol(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.regionMatches(true, 0, "http://", 0, 7)) {
            return normalized.substring(7);
        }
        if (normalized.regionMatches(true, 0, "https://", 0, 8)) {
            return normalized.substring(8);
        }
        return normalized;
    }

    private static String stripTrailingSlash(String value) {
        String normalized = value;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String basePath(String prefix) {
        String normalized = prefix == null ? "" : prefix.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? "" : normalized + "/";
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
        Assert.isFalse(exists, "操作配置'" + bo.getConfigKey() + "'失败, 配置key已存在!");
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

    private void clearOptionalFields(SysOssConfig entity, SysOssConfigBo bo) {
        entity.setPrefix(bo.getPrefix() == null ? "" : bo.getPrefix());
        entity.setRegion(bo.getRegion() == null ? "" : bo.getRegion());
        entity.setExt1(bo.getExt1() == null ? "" : bo.getExt1());
        entity.setRemark(bo.getRemark() == null ? "" : bo.getRemark());
    }

    /** 为 x-file-storage 注入与 6.X 一致的 S3 路径风格和预签名端点。 */
    private static final class BellS3ClientFactory implements FileStorageClientFactory<AmazonS3V2Client> {

        private final FileStorageProperties.AmazonS3V2Config properties;
        private final boolean pathStyleAccess;
        private final String presignerEndpoint;
        private volatile AmazonS3V2Client client;

        private BellS3ClientFactory(FileStorageProperties.AmazonS3V2Config properties,
                                    boolean pathStyleAccess, String presignerEndpoint) {
            this.properties = properties;
            this.pathStyleAccess = pathStyleAccess;
            this.presignerEndpoint = presignerEndpoint;
        }

        @Override
        public String getPlatform() {
            return properties.getPlatform();
        }

        @Override
        public AmazonS3V2Client getClient() {
            AmazonS3V2Client result = client;
            if (result == null) {
                synchronized (this) {
                    result = client;
                    if (result == null) {
                        result = createClient();
                        client = result;
                    }
                }
            }
            return result;
        }

        private AmazonS3V2Client createClient() {
            StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
            Region region = Region.of(properties.getRegion());
            S3Configuration configuration = S3Configuration.builder()
                    .pathStyleAccessEnabled(pathStyleAccess)
                    .build();
            S3Client s3Client = S3Client.builder()
                    .credentialsProvider(credentials)
                    .region(region)
                    .endpointOverride(URI.create(properties.getEndPoint()))
                    .serviceConfiguration(configuration)
                    .build();
            S3Presigner presigner = S3Presigner.builder()
                    .credentialsProvider(credentials)
                    .region(region)
                    .endpointOverride(URI.create(presignerEndpoint))
                    .serviceConfiguration(configuration)
                    .build();
            AmazonS3V2Client result = new AmazonS3V2Client(
                    properties.getAccessKey(), properties.getSecretKey(),
                    properties.getRegion(), properties.getEndPoint());
            result.setClient(s3Client);
            result.setPresigner(presigner);
            return result;
        }

        @Override
        public synchronized void close() {
            if (client != null) {
                client.close();
                client = null;
            }
        }
    }
}

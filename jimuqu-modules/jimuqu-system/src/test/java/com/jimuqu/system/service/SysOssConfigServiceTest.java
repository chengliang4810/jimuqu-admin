package com.jimuqu.system.service;

import com.jimuqu.system.domain.SysOssConfig;
import com.jimuqu.system.domain.bo.SysOssConfigBo;
import com.jimuqu.system.mapper.SysOssConfigMapper;
import org.junit.jupiter.api.Test;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.platform.AmazonS3V2FileStorage;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysOssConfigServiceTest {

    @Test
    void mixedExistingAndMissingIdsDoNotDeleteAnyConfiguration() {
        AtomicBoolean deleteInvoked = new AtomicBoolean();
        SysOssConfig existing = new SysOssConfig().setOssConfigId(2L).setConfigKey("existing");
        SysOssConfigMapper mapper = mapper((method, args) -> {
            if ("list".equals(method)) {
                return List.of(existing);
            }
            if ("deleteByIds".equals(method)) {
                deleteInvoked.set(true);
                return 1;
            }
            return null;
        });

        SysOssConfigService service = new SysOssConfigService(mapper, null);

        assertThrows(RuntimeException.class, () -> service.delete(List.of(2L, 999L)));
        assertFalse(deleteInvoked.get(), "存在任一无效 ID 时不得删除有效 OSS 配置");
    }

    @Test
    void updatingMissingConfigurationDoesNotDisableCurrentDefault() {
        AtomicBoolean updateInvoked = new AtomicBoolean();
        SysOssConfigMapper mapper = mapper((method, args) -> {
            if ("exists".equals(method)) {
                return false;
            }
            if ("getById".equals(method)) {
                return null;
            }
            if ("update".equals(method)) {
                updateInvoked.set(true);
                return 1;
            }
            return null;
        });
        SysOssConfigBo bo = new SysOssConfigBo();
        bo.setOssConfigId(999L);
        bo.setConfigKey("missing");
        bo.setStatus("Y");

        assertThrows(RuntimeException.class, () -> new SysOssConfigService(mapper, null).update(bo));
        assertFalse(updateInvoked.get(), "确认目标存在之前不得关闭当前默认 OSS 配置");
    }

    @Test
    void systemConfigIdsMatchSeededDefaultConfiguration() {
        assertEquals(Set.of(1761900000000000001L), SysOssConfigService.SYSTEM_CONFIG_IDS);
    }

    @Test
    void mapsCloudS3RegionUrlPrefixAndAccessPolicy() {
        SysOssConfig config = config("aliyun", "oss-cn-beijing.aliyuncs.com")
                .setRegion("cn-beijing")
                .setPrefix("/images/")
                .setAccessPolicy("2");

        AmazonS3V2FileStorage storage = SysOssConfigService.createS3Platform(config);

        assertFalse(SysOssConfigService.usePathStyleAccess(config));
        assertEquals("cn-beijing", storage.getRegion());
        assertEquals("http://bucket.oss-cn-beijing.aliyuncs.com/", storage.getDomain());
        assertEquals("images/", storage.getBasePath());
        assertEquals(Constant.ACL.PUBLIC_READ, storage.getDefaultAcl());
        assertNotNull(storage.getClient().getClient());
        assertNotNull(storage.getClient().getPresigner());
        storage.close();
    }

    @Test
    void mapsMinioCompatibleEndpointToPathStyleAndDefaultRegion() {
        SysOssConfig config = config("minio", "127.0.0.1:9000")
                .setIsHttps("Y")
                .setAccessPolicy("0");

        AmazonS3V2FileStorage storage = SysOssConfigService.createS3Platform(config);

        assertTrue(SysOssConfigService.usePathStyleAccess(config));
        assertEquals("us-east-1", storage.getRegion());
        assertEquals("https://127.0.0.1:9000/bucket/", storage.getDomain());
        assertEquals(Constant.ACL.PRIVATE, storage.getDefaultAcl());
        assertNull(SysOssConfigService.defaultAcl("custom"));
        assertNotNull(storage.getClient().getClient());
        assertNotNull(storage.getClient().getPresigner());
        storage.close();
    }

    private SysOssConfig config(String key, String endpoint) {
        return new SysOssConfig()
                .setConfigKey(key)
                .setAccessKey("access")
                .setSecretKey("secret")
                .setBucketName("bucket")
                .setEndpoint(endpoint)
                .setIsHttps("N");
    }

    private SysOssConfigMapper mapper(MapperCall call) {
        return (SysOssConfigMapper) Proxy.newProxyInstance(
                SysOssConfigMapper.class.getClassLoader(), new Class<?>[]{SysOssConfigMapper.class},
                (proxy, method, args) -> {
                    if ("getEntityType".equals(method.getName())) {
                        return SysOssConfig.class;
                    }
                    Object result = call.invoke(method.getName(), args);
                    if (result != null || !method.getReturnType().isPrimitive()) {
                        return result;
                    }
                    return method.getReturnType() == boolean.class ? false : 0;
                });
    }

    @FunctionalInterface
    private interface MapperCall {
        Object invoke(String method, Object[] args);
    }
}

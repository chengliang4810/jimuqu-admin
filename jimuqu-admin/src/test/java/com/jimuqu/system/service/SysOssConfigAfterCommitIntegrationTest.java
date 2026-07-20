package com.jimuqu.system.service;

import com.jimuqu.common.sms.config.SmsConfig;
import com.jimuqu.system.domain.SysOssConfig;
import com.jimuqu.system.domain.bo.SysOssConfigBo;
import com.jimuqu.system.mapper.SysOssConfigMapper;
import com.jimuqu.system.service.impl.AfterCommitTaskExecutor;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.data.annotation.TransactionAnno;
import org.noear.solon.data.tran.TranPolicy;
import org.noear.solon.data.tran.TranUtils;
import org.noear.solon.test.SolonTest;

import java.lang.reflect.Proxy;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(SmsConfig.class)
@SolonTest(scanning = false, enableHttp = false, debug = false, delay = 0)
public class SysOssConfigAfterCommitIntegrationTest {

    @Test
    void platformStateChangesAfterCommitBeforeTransactionReturns() throws Throwable {
        SysOssConfigMapper mapper = mapper((method, args) -> {
            if ("exists".equals(method)) {
                return false;
            }
            if ("save".equals(method)) {
                ((SysOssConfig) args[0]).setOssConfigId(9L);
                return 1;
            }
            return null;
        });
        FileStorageService storageService = mock(FileStorageService.class);
        CopyOnWriteArrayList<FileStorage> platforms = new CopyOnWriteArrayList<>();
        FileStorageProperties properties = new FileStorageProperties();
        properties.setDefaultPlatform("previous");
        when(storageService.getFileStorageList()).thenReturn(platforms);
        when(storageService.getProperties()).thenReturn(properties);
        when(storageService.getFileStorage("transactional")).thenAnswer(invocation -> platforms.stream()
                .filter(storage -> "transactional".equals(storage.getPlatform()))
                .findFirst().orElse(null));
        SysOssConfigService service = new SysOssConfigService(
                mapper, storageService, new AfterCommitTaskExecutor());
        SysOssConfigBo bo = new SysOssConfigBo();
        bo.setConfigKey("transactional");
        bo.setAccessKey("access");
        bo.setSecretKey("secret");
        bo.setBucketName("bucket");
        bo.setEndpoint("127.0.0.1:9000");
        bo.setIsHttps("N");
        bo.setStatus("Y");

        TranUtils.execute(new TransactionAnno().policy(TranPolicy.required), () -> {
            assertEquals(1, service.insert(bo));
            assertTrue(platforms.isEmpty(), "事务提交前不得改变 JVM 中的存储平台");
            assertEquals("previous", properties.getDefaultPlatform());
        });

        assertEquals(1, platforms.size());
        assertEquals("transactional", properties.getDefaultPlatform());
        platforms.forEach(FileStorage::close);
    }

    @Test
    void statusChangesKeepStaticallyConfiguredPlatformRegistered() throws Throwable {
        SysOssConfig config = new SysOssConfig()
                .setOssConfigId(1761900000000000001L)
                .setConfigKey("default")
                .setStatus("Y");
        SysOssConfigMapper mapper = mapper((method, args) -> {
            if ("getById".equals(method)) {
                return config;
            }
            if ("update".equals(method)) {
                return 1;
            }
            return null;
        });
        FileStorage platform = mock(FileStorage.class);
        when(platform.getPlatform()).thenReturn("default");
        CopyOnWriteArrayList<FileStorage> platforms = new CopyOnWriteArrayList<>();
        platforms.add(platform);
        FileStorageService storageService = mock(FileStorageService.class);
        FileStorageProperties properties = new FileStorageProperties();
        properties.setDefaultPlatform("previous");
        when(storageService.getProperties()).thenReturn(properties);
        when(storageService.getFileStorageList()).thenReturn(platforms);
        when(storageService.getFileStorage("default")).thenAnswer(invocation -> platforms.stream()
                .filter(storage -> "default".equals(storage.getPlatform()))
                .findFirst().orElse(null));
        SysOssConfigService service = new SysOssConfigService(
                mapper, storageService, new AfterCommitTaskExecutor());
        SysOssConfigBo bo = new SysOssConfigBo();
        bo.setOssConfigId(config.getOssConfigId());

        for (String status : new String[]{"N", "Y"}) {
            properties.setDefaultPlatform("previous");
            bo.setStatus(status);
            TranUtils.execute(new TransactionAnno().policy(TranPolicy.required), () -> {
                assertEquals(1, service.changeStatus(bo));
                assertEquals("previous", properties.getDefaultPlatform());
                assertSame(platform, platforms.get(0));
            });
            assertEquals("default", properties.getDefaultPlatform());
            assertSame(platform, platforms.get(0));
        }
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

package com.jimuqu.system.service.impl;

import com.jimuqu.common.cache.VersionedCacheNamespace;
import com.jimuqu.common.core.constant.CacheConstants;
import com.jimuqu.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.Test;
import org.noear.solon.data.cache.LocalCacheService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class SysConfigServiceImplParityTest {

    @Test
    void resetMakesDeletedConfigCacheUnreachable() {
        LocalCacheService cacheService = new LocalCacheService(3600);
        try {
            VersionedCacheNamespace namespace =
                    new VersionedCacheNamespace(cacheService, CacheConstants.SYS_CONFIG_KEY);
            namespace.store("deleted.config", "stale", 3600);
            assertEquals("stale", namespace.get("deleted.config", String.class));

            new SysConfigServiceImpl(mock(SysConfigMapper.class), cacheService).resetConfigCache();

            assertNull(namespace.get("deleted.config", String.class));
            namespace.store("deleted.config", "fresh", 3600);
            assertEquals("fresh", namespace.get("deleted.config", String.class));
        } finally {
            cacheService.clear();
        }
    }
}

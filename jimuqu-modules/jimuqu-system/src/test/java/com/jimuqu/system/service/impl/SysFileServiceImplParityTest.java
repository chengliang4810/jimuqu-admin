package com.jimuqu.system.service.impl;

import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysFile;
import com.jimuqu.system.domain.SysOssConfig;
import com.jimuqu.system.domain.query.SysFileQuery;
import com.jimuqu.system.domain.vo.SysFileVo;
import com.jimuqu.system.domain.vo.SysOssVo;
import com.jimuqu.system.mapper.SysFileMapper;
import com.jimuqu.system.mapper.SysOssConfigMapper;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SysFileServiceImplParityTest {

    @Test
    void mixedExistingAndMissingIdsAreValidatedBeforePhysicalDeletion() {
        SysFile existing = new SysFile().setId("file-1").setPlatform("private").setUrl("stored-url");
        SysFileMapper mapper = fileMapper((method, args) -> {
            if ("list".equals(method)) {
                return List.of(existing);
            }
            if ("getById".equals(method)) {
                return "file-1".equals(args[0]) ? existing : null;
            }
            return null;
        });
        ThrowingStorage storage = new ThrowingStorage();
        SysFileServiceImpl service = service(mapper, null, storage);

        assertThrows(ServiceException.class,
                () -> service.deleteOssByIds(List.of("file-1", "missing")));
        assertEquals(0, storage.deleteCalls.get(), "批量 ID 全部有效前不得触碰物理文件");
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageSigningFailureIsStrictButListByIdsFallsBackToStoredUrl() {
        SysFileVo file = new SysFileVo()
                .setId("file-1")
                .setPlatform("private")
                .setUrl("stored-url")
                .setFilename("stored.txt")
                .setOriginalFilename("original.txt");
        SysFileMapper mapper = fileMapper((method, args) -> {
            if ("paging".equals(method)) {
                Page<SysFileVo> page = (Page<SysFileVo>) args[1];
                page.setRows(List.of(file));
                page.setTotal(1L);
                return page;
            }
            if ("list".equals(method)) {
                return List.of(file);
            }
            return null;
        });
        SysOssConfigMapper configMapper = (SysOssConfigMapper) Proxy.newProxyInstance(
                SysOssConfigMapper.class.getClassLoader(), new Class<?>[]{SysOssConfigMapper.class},
                (proxy, method, args) -> {
                    if ("getEntityType".equals(method.getName())) {
                        return SysOssConfig.class;
                    }
                    if ("list".equals(method.getName())) {
                        return List.of(new SysOssConfig().setConfigKey("private").setAccessPolicy("0"));
                    }
                    return primitiveDefault(method.getReturnType());
                });
        SysFileServiceImpl service = service(mapper, configMapper, new ThrowingStorage());
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNum(1);
        pageQuery.setPageSize(10);

        assertThrows(IllegalStateException.class,
                () -> service.queryOssPageList(new SysFileQuery(), pageQuery));
        List<SysOssVo> files = service.queryOssByIds(List.of("file-1"));
        assertEquals(1, files.size());
        assertEquals("stored-url", files.get(0).getUrl());
    }

    private SysFileServiceImpl service(SysFileMapper mapper, SysOssConfigMapper configMapper,
                                       FileStorageService storage) {
        return new SysFileServiceImpl(mapper, null, configMapper, null) {
            @Override
            FileStorageService fileStorageService() {
                return storage;
            }
        };
    }

    private SysFileMapper fileMapper(MapperCall call) {
        return (SysFileMapper) Proxy.newProxyInstance(
                SysFileMapper.class.getClassLoader(), new Class<?>[]{SysFileMapper.class},
                (proxy, method, args) -> {
                    if ("getEntityType".equals(method.getName())) {
                        return SysFile.class;
                    }
                    Object result = call.invoke(method.getName(), args);
                    return result != null ? result : primitiveDefault(method.getReturnType());
                });
    }

    private static Object primitiveDefault(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        return type == boolean.class ? false : 0;
    }

    @FunctionalInterface
    private interface MapperCall {
        Object invoke(String method, Object[] args);
    }

    private static final class ThrowingStorage extends FileStorageService {
        private final AtomicInteger deleteCalls = new AtomicInteger();

        @Override
        public boolean delete(FileInfo fileInfo) {
            deleteCalls.incrementAndGet();
            return true;
        }

        @Override
        public boolean isSupportPresignedUrl(String platform) {
            return true;
        }

        @Override
        public String generatePresignedUrl(FileInfo fileInfo, java.util.Date expiration) {
            throw new IllegalStateException("signing unavailable");
        }
    }
}

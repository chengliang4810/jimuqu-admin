package com.jimuqu.system.service.impl;

import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.system.domain.query.SysFileQuery;
import com.jimuqu.system.domain.vo.SysFileVo;
import com.jimuqu.system.mapper.SysFileMapper;
import com.jimuqu.system.mapper.SysFilePartMapper;
import com.jimuqu.system.mapper.SysOssConfigMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class SysFileBellContractTest {

    @Test
    void exposesAndFiltersSuffixLikeRuoYiOss() {
        assertEquals(".txt", SysFileServiceImpl.toBellFileSuffix("txt"));
        assertEquals(".txt", SysFileServiceImpl.toBellFileSuffix(".txt"));
        assertEquals("", SysFileServiceImpl.toBellFileSuffix(""));
        assertNull(SysFileServiceImpl.toBellFileSuffix(null));

        SysFileQuery query = new SysFileQuery();
        query.setFileSuffix(".txt");
        query.setCreateBy(1L);
        query.beforeBuildCondition();

        assertEquals("txt", query.getFileSuffix());
        assertEquals(1L, query.getCreateBy());
    }

    @Test
    void missingDownloadUsesUpstreamBusinessError() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        SysFileServiceImpl service = service(fileMapper);
        when(fileMapper.getById("missing")).thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class, () -> service.download("missing"));

        assertEquals("文件数据不存在!", error.getMessage());
    }

    @Test
    void privateFileUsesTemporaryAccessUrl() {
        SysFileServiceImpl service = spy(service(mock(SysFileMapper.class)));
        FileStorageService storageService = mock(FileStorageService.class);
        doReturn(storageService).when(service).fileStorageService();
        when(storageService.isSupportPresignedUrl("private-platform")).thenReturn(true);
        when(storageService.generatePresignedUrl(any(FileInfo.class), any(Date.class)))
                .thenReturn("https://signed.example/avatar.png");
        SysFileVo file = new SysFileVo()
                .setId("42")
                .setPlatform("private-platform")
                .setUrl("https://origin.example/avatar.png")
                .setFilename("avatar.png")
                .setOriginalFilename("avatar.png");

        assertEquals("https://signed.example/avatar.png",
                service.resolveAccessUrl(file, Set.of("private-platform")));
    }

    private static SysFileServiceImpl service(SysFileMapper fileMapper) {
        return new SysFileServiceImpl(fileMapper, mock(SysFilePartMapper.class),
                mock(SysOssConfigMapper.class), mock(SysUserMapper.class));
    }
}

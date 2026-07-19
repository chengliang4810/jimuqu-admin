package com.jimuqu.system.controller;

import com.jimuqu.system.service.SysFileService;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysFileControllerAccessUrlTest {

    @Test
    void uploadResponseUsesUnifiedOssAccessUrl() {
        SysFileService fileService = mock(SysFileService.class);
        SysFileController controller = new SysFileController(fileService, mock(FileStorageService.class));
        FileInfo info = new FileInfo();
        info.setId("42");
        info.setUrl("https://origin.example/file.txt");
        when(fileService.selectUrlByIds("42")).thenReturn("https://signed.example/file.txt");

        assertEquals("https://signed.example/file.txt", controller.resolveUploadUrl(info));
    }
}

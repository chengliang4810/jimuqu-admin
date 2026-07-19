package com.jimuqu.common.web.interceptor;

import org.junit.jupiter.api.Test;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.core.handle.DownloadedFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadHeaderInterceptorTest {

    @Test
    void exposesPercentEncodedDownloadFilename() throws Exception {
        Context context = ContextEmpty.create();
        DownloadedFile file = new DownloadedFile("application/octet-stream",
                "content".getBytes(StandardCharsets.UTF_8), "用户 数据.xlsx");

        Object result = new DownloadHeaderInterceptor().postResult(context, file);

        DownloadedFile renderedFile = (DownloadedFile) result;
        assertNull(renderedFile.getName());
        assertEquals("%E7%94%A8%E6%88%B7%20%E6%95%B0%E6%8D%AE.xlsx",
                context.headerOfResponse("download-filename"));
        assertEquals("attachment; filename=%E7%94%A8%E6%88%B7%20%E6%95%B0%E6%8D%AE.xlsx;"
                        + "filename*=utf-8''%E7%94%A8%E6%88%B7%20%E6%95%B0%E6%8D%AE.xlsx",
                context.headerOfResponse("Content-Disposition"));
        assertTrue(context.headerValuesOfResponse("Access-Control-Expose-Headers")
                .contains("Content-Disposition,download-filename"));
    }
}

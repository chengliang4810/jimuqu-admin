package com.jimuqu.common.excel.utils;

import cn.idev.excel.annotation.ExcelProperty;
import com.jimuqu.common.excel.core.LargeExcelExportRequest;
import com.jimuqu.common.excel.core.LargeExcelExportResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LargeExcelExportUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void exportsZipWhenRowsAreSplitAcrossMultipleFiles() throws Exception {
        List<Integer> requestedPages = new ArrayList<>();
        LargeExcelExportRequest<DemoRow> request = LargeExcelExportRequest.<DemoRow>builder()
                .fileName("demo-report")
                .sheetName("数据")
                .head(DemoRow.class)
                .pageSize(2)
                .rowsPerFile(3)
                .forceZip(true)
                .tempDir(tempDir)
                .fetcher((page, pageSize) -> {
                    requestedPages.add(page);
                    return page(page, pageSize, 5);
                })
                .build();

        LargeExcelExportResult result = LargeExcelExportUtil.exportToTempFile(request);

        assertTrue(result.isZip());
        assertEquals(5, result.getTotalRows());
        assertEquals(2, result.getFileCount());
        assertTrue(Files.exists(result.getPath()));
        assertEquals(List.of(1, 2, 3), requestedPages);
        assertEquals(List.of("demo-report_1.xlsx", "demo-report_2.xlsx"), zipEntryNames(result.getPath()));
    }

    @Test
    void exportsSingleXlsxWhenZipIsNotForcedAndOnlyOneFileIsNeeded() throws Exception {
        LargeExcelExportRequest<DemoRow> request = LargeExcelExportRequest.<DemoRow>builder()
                .fileName("single-report")
                .sheetName("数据")
                .head(DemoRow.class)
                .pageSize(10)
                .rowsPerFile(100)
                .tempDir(tempDir)
                .fetcher((page, pageSize) -> page(page, pageSize, 3))
                .build();

        LargeExcelExportResult result = LargeExcelExportUtil.exportToTempFile(request);

        assertFalse(result.isZip());
        assertEquals("single-report.xlsx", result.getFileName());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", result.getContentType());
        assertEquals(3, result.getTotalRows());
        assertEquals(1, result.getFileCount());
        assertTrue(Files.size(result.getPath()) > 0);
    }

    private static List<DemoRow> page(int page, int pageSize, int total) {
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<DemoRow> rows = new ArrayList<>();
        for (int i = from; i < to; i++) {
            rows.add(new DemoRow(i + 1, "name-" + (i + 1)));
        }
        return rows;
    }

    private static List<String> zipEntryNames(Path zipPath) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    static class DemoRow {
        @ExcelProperty("编号")
        private Integer id;

        @ExcelProperty("名称")
        private String name;

        DemoRow(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}

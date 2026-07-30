package com.jimuqu.common.excel.utils;

import com.jimuqu.common.excel.core.LargeExcelExportRequest;
import com.jimuqu.common.excel.core.LargeExcelExportResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * FastExcel大数据分片导出工具。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LargeExcelExportUtil {

    /**
     * 导出到本地临时文件。
     */
    public static <T> LargeExcelExportResult exportToTempFile(LargeExcelExportRequest<T> request) {
        ExecutorService executorService = request.getExecutorService();
        boolean shutdownExecutor = false;
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(request.getThreadCount());
            shutdownExecutor = true;
        }

        List<Path> cleanupFiles = new ArrayList<>();
        try {
            ExportParts<T> exportParts = exportParts(request, executorService, cleanupFiles);
            LargeExcelExportResult result = buildResult(request, exportParts.parts, exportParts.totalRows, cleanupFiles);
            cleanupFiles.remove(result.getPath());
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("大数据Excel导出失败", e);
        } finally {
            cleanup(cleanupFiles);
            if (shutdownExecutor) {
                executorService.shutdown();
            }
        }
    }

    /**
     * 导出到输出流。内部仍先落临时文件，以便复用分片和zip打包逻辑。
     */
    public static <T> LargeExcelExportResult export(LargeExcelExportRequest<T> request, OutputStream outputStream) {
        LargeExcelExportResult result = exportToTempFile(request);
        try {
            result.writeTo(outputStream);
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("写出Excel导出结果失败", e);
        }
    }

    private static <T> ExportParts<T> exportParts(LargeExcelExportRequest<T> request,
                                                  ExecutorService executorService,
                                                  List<Path> cleanupFiles) throws Exception {
        List<Future<PartFile>> futures = new ArrayList<>();
        List<PartFile> parts = new ArrayList<>();
        List<T> buffer = new ArrayList<>(Math.min(request.getPageSize(), request.getRowsPerFile()));
        int page = 1;
        int partNo = 1;
        int collected = 0;
        long totalRows = 0;

        while (true) {
            List<T> pageRows = request.getFetcher().fetch(page, request.getPageSize());
            if (pageRows == null) {
                pageRows = Collections.emptyList();
            }
            if (pageRows.isEmpty()) {
                break;
            }

            for (T row : pageRows) {
                buffer.add(row);
                totalRows++;
                if (buffer.size() >= request.getRowsPerFile()) {
                    futures.add(submitPart(request, executorService, partNo++, buffer, cleanupFiles));
                    buffer = new ArrayList<>(Math.min(request.getPageSize(), request.getRowsPerFile()));
                    collected = collectCompletedInOrder(futures, parts, collected, request.getThreadCount());
                }
            }

            if (pageRows.size() < request.getPageSize()) {
                break;
            }
            page++;
        }

        if (!buffer.isEmpty() || futures.isEmpty()) {
            futures.add(submitPart(request, executorService, partNo, buffer, cleanupFiles));
        }
        collectAllInOrder(futures, parts, collected);
        return new ExportParts<>(parts, totalRows);
    }

    private static <T> Future<PartFile> submitPart(LargeExcelExportRequest<T> request,
                                                   ExecutorService executorService,
                                                   int partNo,
                                                   List<T> rows,
                                                   List<Path> cleanupFiles) throws IOException {
        List<T> partRows = new ArrayList<>(rows);
        String fileName = sanitizeFileName(request.getFileName()) + "_" + partNo + ".xlsx";
        Path path = createTempFile(request.getTempDir(), fileName, ".xlsx");
        cleanupFiles.add(path);
        Callable<PartFile> task = () -> {
            try (OutputStream os = Files.newOutputStream(path)) {
                ExcelUtil.exportExcel(partRows, request.getSheetName(), request.getHead(), os);
            }
            return new PartFile(path, fileName, partRows.size());
        };
        return executorService.submit(task);
    }

    private static int collectCompletedInOrder(List<Future<PartFile>> futures,
                                               List<PartFile> parts,
                                               int collected,
                                               int threadCount) throws ExecutionException, InterruptedException {
        while (futures.size() - collected >= threadCount) {
            parts.add(futures.get(collected).get());
            collected++;
        }
        return collected;
    }

    private static void collectAllInOrder(List<Future<PartFile>> futures,
                                          List<PartFile> parts,
                                          int collected) throws ExecutionException, InterruptedException {
        for (int i = collected; i < futures.size(); i++) {
            parts.add(futures.get(i).get());
        }
    }

    private static LargeExcelExportResult buildResult(LargeExcelExportRequest<?> request,
                                                      List<PartFile> parts,
                                                      long totalRows,
                                                      List<Path> cleanupFiles) throws IOException {
        String baseName = sanitizeFileName(request.getFileName());
        if (request.isForceZip() || parts.size() > 1) {
            Path zipPath = createTempFile(request.getTempDir(), baseName + ".zip", ".zip");
            cleanupFiles.add(zipPath);
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                for (PartFile part : parts) {
                    zos.putNextEntry(new ZipEntry(part.fileName));
                    Files.copy(part.path, zos);
                    zos.closeEntry();
                }
            }
            return new LargeExcelExportResult(zipPath, baseName + ".zip",
                    LargeExcelExportResult.ZIP_CONTENT_TYPE, true, totalRows, parts.size());
        }

        PartFile part = parts.getFirst();
        return new LargeExcelExportResult(part.path, baseName + ".xlsx",
                LargeExcelExportResult.XLSX_CONTENT_TYPE, false, totalRows, 1);
    }

    private static Path createTempFile(Path tempDir, String fileName, String suffix) throws IOException {
        Path dir = tempDir == null ? Files.createTempDirectory("jimuqu-excel-") : tempDir;
        Files.createDirectories(dir);
        String prefix = sanitizeFileName(fileName);
        if (prefix.length() < 3) {
            prefix = "xls" + prefix;
        }
        return Files.createTempFile(dir, prefix + "-", suffix);
    }

    private static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static void cleanup(List<Path> files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // 临时文件清理失败不应覆盖原始导出结果。
            }
        }
    }

    private record PartFile(Path path, String fileName, int rows) {
    }

    private record ExportParts<T>(List<PartFile> parts, long totalRows) {
    }
}

package com.jimuqu.common.excel.core;

import org.noear.solon.core.handle.DownloadedFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 大数据Excel导出结果。
 */
public class LargeExcelExportResult {

    public static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String ZIP_CONTENT_TYPE = "application/zip";

    private final Path path;
    private final String fileName;
    private final String contentType;
    private final boolean zip;
    private final long totalRows;
    private final int fileCount;

    public LargeExcelExportResult(Path path, String fileName, String contentType, boolean zip, long totalRows, int fileCount) {
        this.path = path;
        this.fileName = fileName;
        this.contentType = contentType;
        this.zip = zip;
        this.totalRows = totalRows;
        this.fileCount = fileCount;
    }

    public Path getPath() {
        return path;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isZip() {
        return zip;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public int getFileCount() {
        return fileCount;
    }

    public InputStream openInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        Files.copy(path, outputStream);
    }

    public DownloadedFile toDownloadedFile() throws IOException {
        return new DownloadedFile(contentType + ";charset=UTF-8", Files.readAllBytes(path), fileName);
    }

    public boolean deleteFile() throws IOException {
        return Files.deleteIfExists(path);
    }
}

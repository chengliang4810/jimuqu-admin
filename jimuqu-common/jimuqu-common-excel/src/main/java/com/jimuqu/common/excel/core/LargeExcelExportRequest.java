package com.jimuqu.common.excel.core;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * 大数据Excel导出请求参数。
 *
 * @param <T> 导出行类型
 */
public class LargeExcelExportRequest<T> {

    /**
     * xlsx除表头外最大数据行数。
     */
    public static final int XLSX_MAX_DATA_ROWS = 1_048_575;

    private static final int DEFAULT_PAGE_SIZE = 5_000;
    private static final int DEFAULT_ROWS_PER_FILE = XLSX_MAX_DATA_ROWS;
    private static final int DEFAULT_THREAD_COUNT = 2;

    private final String fileName;
    private final String sheetName;
    private final Class<T> head;
    private final int pageSize;
    private final int rowsPerFile;
    private final boolean forceZip;
    private final Path tempDir;
    private final int threadCount;
    private final ExecutorService executorService;
    private final ExcelPageFetcher<T> fetcher;

    private LargeExcelExportRequest(Builder<T> builder) {
        this.fileName = requireText(builder.fileName, "fileName");
        this.sheetName = requireText(builder.sheetName, "sheetName");
        this.head = Objects.requireNonNull(builder.head, "head不能为空");
        this.pageSize = positiveOrDefault(builder.pageSize, DEFAULT_PAGE_SIZE, "pageSize");
        this.rowsPerFile = positiveOrDefault(builder.rowsPerFile, DEFAULT_ROWS_PER_FILE, "rowsPerFile");
        if (this.rowsPerFile > XLSX_MAX_DATA_ROWS) {
            throw new IllegalArgumentException("rowsPerFile不能超过" + XLSX_MAX_DATA_ROWS);
        }
        this.forceZip = Boolean.TRUE.equals(builder.forceZip);
        this.tempDir = builder.tempDir;
        this.threadCount = positiveOrDefault(builder.threadCount, DEFAULT_THREAD_COUNT, "threadCount");
        this.executorService = builder.executorService;
        this.fetcher = Objects.requireNonNull(builder.fetcher, "fetcher不能为空");
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public String getFileName() {
        return fileName;
    }

    public String getSheetName() {
        return sheetName;
    }

    public Class<T> getHead() {
        return head;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getRowsPerFile() {
        return rowsPerFile;
    }

    public boolean isForceZip() {
        return forceZip;
    }

    public Path getTempDir() {
        return tempDir;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public ExcelPageFetcher<T> getFetcher() {
        return fetcher;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value.trim();
    }

    private static int positiveOrDefault(Integer value, int defaultValue, String name) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(name + "必须大于0");
        }
        return value;
    }

    public static class Builder<T> {
        private String fileName;
        private String sheetName;
        private Class<T> head;
        private Integer pageSize;
        private Integer rowsPerFile;
        private Boolean forceZip;
        private Path tempDir;
        private Integer threadCount;
        private ExecutorService executorService;
        private ExcelPageFetcher<T> fetcher;

        public Builder<T> fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder<T> sheetName(String sheetName) {
            this.sheetName = sheetName;
            return this;
        }

        public Builder<T> head(Class<T> head) {
            this.head = head;
            return this;
        }

        public Builder<T> pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder<T> rowsPerFile(Integer rowsPerFile) {
            this.rowsPerFile = rowsPerFile;
            return this;
        }

        public Builder<T> forceZip(Boolean forceZip) {
            this.forceZip = forceZip;
            return this;
        }

        public Builder<T> tempDir(Path tempDir) {
            this.tempDir = tempDir;
            return this;
        }

        public Builder<T> threadCount(Integer threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        public Builder<T> executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder<T> fetcher(ExcelPageFetcher<T> fetcher) {
            this.fetcher = fetcher;
            return this;
        }

        public LargeExcelExportRequest<T> build() {
            return new LargeExcelExportRequest<>(this);
        }
    }
}

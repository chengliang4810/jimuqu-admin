package com.jimuqu.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 系统监控信息VO
 *
 * @author chengliang4810
 * @since 2025-09-05
 */
@Data
public class SystemMonitorVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * CPU信息
     */
    private CpuInfo cpu;

    /**
     * 内存信息
     */
    private MemoryInfo memory;

    /**
     * JVM信息
     */
    private JvmInfo jvm;

    /**
     * 磁盘信息
     */
    private List<DiskInfo> disks;

    /**
     * 系统信息
     */
    private SystemInfo system;

    /**
     * CPU信息
     */
    @Data
    public static class CpuInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * CPU核心数
         */
        private int cpuNum;

        /**
         * 系统CPU使用率（整个系统的CPU使用率）
         */
        private double cpuUsed;

        /**
         * 系统CPU使用率（同cpuUsed字段）
         */
        private double cpuSystem;

        /**
         * 当前进程CPU使用率（Java进程的CPU使用率）
         */
        private double cpuUser;
    }

    /**
     * 内存信息
     */
    @Data
    public static class MemoryInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 内存总量
         */
        private long total;

        /**
         * 已用内存
         */
        private long used;

        /**
         * 剩余内存
         */
        private long free;

        /**
         * 内存使用率
         */
        private double usage;
    }

    /**
     * JVM信息
     */
    @Data
    public static class JvmInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * JVM名称
         */
        private String name;

        /**
         * Java版本
         */
        private String version;

        /**
         * JVM供应商
         */
        private String vendor;

        /**
         * JVM总内存
         */
        private long total;

        /**
         * JVM最大内存
         */
        private long max;

        /**
         * JVM已用内存
         */
        private long used;

        /**
         * JVM剩余内存
         */
        private long free;

        /**
         * JVM内存使用率
         */
        private double usage;

        /**
         * JVM运行时间
         */
        private String uptime;

        /**
         * JVM启动时间
         */
        private String startTime;
    }

    /**
     * 磁盘信息
     */
    @Data
    public static class DiskInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 磁盘名称
         */
        private String name;

        /**
         * 磁盘路径
         */
        private String path;

        /**
         * 磁盘类型
         */
        private String type;

        /**
         * 磁盘总容量
         */
        private long total;

        /**
         * 磁盘已用容量
         */
        private long used;

        /**
         * 磁盘剩余容量
         */
        private long free;

        /**
         * 磁盘使用率
         */
        private double usage;
    }

    /**
     * 系统信息
     */
    @Data
    public static class SystemInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 服务器名称
         */
        private String computerName;

        /**
         * 操作系统名称
         */
        private String osName;

        /**
         * 操作系统架构
         */
        private String osArch;

        /**
         * 操作系统版本
         */
        private String osVersion;

        /**
         * 服务器IP
         */
        private String hostIp;

        /**
         * 项目路径
         */
        private String userDir;
    }
}
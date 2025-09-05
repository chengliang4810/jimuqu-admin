package com.jimuqu.system.service.impl;

import com.jimuqu.system.domain.vo.SystemMonitorVo;
import com.jimuqu.system.service.SystemMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 系统监控服务实现
 *
 * @author chengliang4810
 * @since 2025-09-05
 */
@Slf4j
@Component
public class SystemMonitorServiceImpl implements SystemMonitorService {

    
    @Override
    public SystemMonitorVo getSystemMonitorInfo() {
        SystemMonitorVo monitorVo = new SystemMonitorVo();
        
        // 获取CPU信息
        monitorVo.setCpu(getCpuInfo());
        
        // 获取内存信息
        monitorVo.setMemory(getMemoryInfo());
        
        // 获取JVM信息
        monitorVo.setJvm(getJvmInfo());
        
        // 获取磁盘信息
        monitorVo.setDisks(getDiskInfo());
        
        // 获取系统信息
        monitorVo.setSystem(getSystemInfo());
        
        return monitorVo;
    }

    /**
     * 获取CPU信息
     */
    private SystemMonitorVo.CpuInfo getCpuInfo() {
        SystemMonitorVo.CpuInfo cpuInfo = new SystemMonitorVo.CpuInfo();
        
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Runtime runtime = Runtime.getRuntime();
            
            // CPU核心数
            cpuInfo.setCpuNum(runtime.availableProcessors());
            
            // 获取系统CPU使用率
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                
                // 获取系统CPU使用率（整个系统的CPU使用率）
                double systemCpuLoad = sunOsBean.getSystemCpuLoad();
                if (systemCpuLoad >= 0) {
                    cpuInfo.setCpuUsed(systemCpuLoad * 100);
                    cpuInfo.setCpuSystem(systemCpuLoad * 100);
                } else {
                    cpuInfo.setCpuUsed(0.0);
                    cpuInfo.setCpuSystem(0.0);
                }
                
                // 获取当前进程CPU使用率
                double processCpuLoad = sunOsBean.getProcessCpuLoad();
                if (processCpuLoad >= 0) {
                    cpuInfo.setCpuUser(processCpuLoad * 100);
                } else {
                    cpuInfo.setCpuUser(0.0);
                }
            } else {
                cpuInfo.setCpuUsed(0.0);
                cpuInfo.setCpuSystem(0.0);
                cpuInfo.setCpuUser(0.0);
            }
            
        } catch (Exception e) {
            log.error("获取CPU信息失败", e);
            cpuInfo.setCpuNum(0);
            cpuInfo.setCpuUsed(0.0);
            cpuInfo.setCpuSystem(0.0);
            cpuInfo.setCpuUser(0.0);
        }
        
        return cpuInfo;
    }

    /**
     * 获取内存信息
     */
    private SystemMonitorVo.MemoryInfo getMemoryInfo() {
        SystemMonitorVo.MemoryInfo memoryInfo = new SystemMonitorVo.MemoryInfo();
        
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                
                // 获取物理内存信息
                long totalMemory = sunOsBean.getTotalPhysicalMemorySize();
                long freeMemory = sunOsBean.getFreePhysicalMemorySize();
                long usedMemory = totalMemory - freeMemory;
                
                memoryInfo.setTotal(totalMemory);
                memoryInfo.setUsed(usedMemory);
                memoryInfo.setFree(freeMemory);
                memoryInfo.setUsage(totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0.0);
            } else {
                // 如果无法获取物理内存信息，使用JVM内存作为备用
                Runtime runtime = Runtime.getRuntime();
                memoryInfo.setTotal(runtime.totalMemory());
                memoryInfo.setUsed(runtime.totalMemory() - runtime.freeMemory());
                memoryInfo.setFree(runtime.freeMemory());
                memoryInfo.setUsage((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory() * 100);
            }
            
        } catch (Exception e) {
            log.error("获取内存信息失败", e);
            memoryInfo.setTotal(0);
            memoryInfo.setUsed(0);
            memoryInfo.setFree(0);
            memoryInfo.setUsage(0.0);
        }
        
        return memoryInfo;
    }

    /**
     * 获取JVM信息
     */
    private SystemMonitorVo.JvmInfo getJvmInfo() {
        SystemMonitorVo.JvmInfo jvmInfo = new SystemMonitorVo.JvmInfo();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            
            // JVM基本信息
            jvmInfo.setName(runtimeBean.getVmName());
            jvmInfo.setVersion(System.getProperty("java.version"));
            jvmInfo.setVendor(System.getProperty("java.vm.vendor"));
            
            // JVM内存信息
            long totalMemory = runtime.totalMemory();
            long maxMemory = runtime.maxMemory();
            long usedMemory = totalMemory - runtime.freeMemory();
            long freeMemory = runtime.freeMemory();
            
            jvmInfo.setTotal(totalMemory);
            jvmInfo.setMax(maxMemory);
            jvmInfo.setUsed(usedMemory);
            jvmInfo.setFree(freeMemory);
            jvmInfo.setUsage(totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0.0);
            
            // JVM运行时间
            long uptime = runtimeBean.getUptime();
            jvmInfo.setUptime(formatUptime(uptime));
            
            // JVM启动时间
            long startTime = runtimeBean.getStartTime();
            jvmInfo.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(startTime)));
            
        } catch (Exception e) {
            log.error("获取JVM信息失败", e);
            jvmInfo.setName("Unknown");
            jvmInfo.setVersion("Unknown");
            jvmInfo.setVendor("Unknown");
            jvmInfo.setTotal(0);
            jvmInfo.setMax(0);
            jvmInfo.setUsed(0);
            jvmInfo.setFree(0);
            jvmInfo.setUsage(0.0);
            jvmInfo.setUptime("Unknown");
            jvmInfo.setStartTime("Unknown");
        }
        
        return jvmInfo;
    }

    /**
     * 获取磁盘信息
     */
    private List<SystemMonitorVo.DiskInfo> getDiskInfo() {
        List<SystemMonitorVo.DiskInfo> diskInfos = new ArrayList<>();
        
        try {
            // 获取所有根目录
            java.io.File[] roots = java.io.File.listRoots();
            
            for (java.io.File root : roots) {
                SystemMonitorVo.DiskInfo diskInfo = new SystemMonitorVo.DiskInfo();
                
                long totalSpace = root.getTotalSpace();
                long usedSpace = totalSpace - root.getFreeSpace();
                long freeSpace = root.getFreeSpace();
                
                diskInfo.setName(root.getName());
                diskInfo.setPath(root.getAbsolutePath());
                diskInfo.setType("Disk");
                diskInfo.setTotal(totalSpace);
                diskInfo.setUsed(usedSpace);
                diskInfo.setFree(freeSpace);
                diskInfo.setUsage(totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0.0);
                
                diskInfos.add(diskInfo);
            }
            
        } catch (Exception e) {
            log.error("获取磁盘信息失败", e);
        }
        
        return diskInfos;
    }

    /**
     * 获取系统信息
     */
    private SystemMonitorVo.SystemInfo getSystemInfo() {
        SystemMonitorVo.SystemInfo systemInfo = new SystemMonitorVo.SystemInfo();
        
        try {
            systemInfo.setComputerName(System.getProperty("user.name"));
            systemInfo.setOsName(System.getProperty("os.name"));
            systemInfo.setOsArch(System.getProperty("os.arch"));
            systemInfo.setOsVersion(System.getProperty("os.version"));
            systemInfo.setHostIp(getLocalIpAddress());
            systemInfo.setUserDir(System.getProperty("user.dir"));
            
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
            systemInfo.setComputerName("Unknown");
            systemInfo.setOsName("Unknown");
            systemInfo.setOsArch("Unknown");
            systemInfo.setOsVersion("Unknown");
            systemInfo.setHostIp("Unknown");
            systemInfo.setUserDir("Unknown");
        }
        
        return systemInfo;
    }

    /**
     * 获取本地IP地址
     */
    private String getLocalIpAddress() {
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime(long uptime) {
        long days = uptime / (24 * 60 * 60 * 1000);
        long hours = (uptime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (uptime % (60 * 60 * 1000)) / (60 * 1000);
        long seconds = (uptime % (60 * 1000)) / 1000;
        
        return String.format("%d天 %d小时 %d分钟 %d秒", days, hours, minutes, seconds);
    }

  }
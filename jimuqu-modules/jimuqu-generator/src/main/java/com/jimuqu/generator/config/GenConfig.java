package com.jimuqu.generator.config;


import lombok.Data;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * 读取代码生成相关配置
 *
 * @author chengliang
 * @date 2024/08/18
 */
@Data
@Inject("${gen}")
@Configuration
public class GenConfig {

    /**
     * 作者
     */
    private String author;

    /**
     * 生成包路径
     */
    private String packageName;

    /**
     * 自动去除表前缀，默认是false
     */
    private Boolean autoRemovePre;

    /**
     * 表前缀(类名不会包含表前缀)
     */
    private String tablePrefix;

    /**
     * 使用平台
     */
    private String platform;

}

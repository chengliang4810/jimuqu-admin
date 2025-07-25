package com.jimuqu.common.core.utils.file;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件处理工具类
 *
 * @author Lion Li,chengliang4810
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileUtil extends cn.hutool.v7.core.io.file.FileUtil {

    /**
     * 百分号编码工具方法
     *
     * @param s 需要百分号编码的字符串
     * @return 百分号编码后的字符串
     */
    public static String percentEncode(String s) {
        String encode = URLEncoder.encode(s, StandardCharsets.UTF_8);
        return encode.replaceAll("\\+", "%20" );
    }

    /**
     * 文件不存在
     * file 为 null 或 file 不存在则返回 true
     *
     * @param file 文件对象
     * @return true 文件存在，false 文件不存在
     */
    public static boolean notExists(File file) {
        return !exists(file);
    }
}

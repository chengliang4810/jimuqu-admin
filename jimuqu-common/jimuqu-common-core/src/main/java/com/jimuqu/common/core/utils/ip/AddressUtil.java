package com.jimuqu.common.core.utils.ip;

import com.jimuqu.common.core.utils.StringUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.net.NetUtil;


/**
 * 获取地址类
 *
 * @author Lion Li,chengliang4810
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressUtil {

    // 未知地址
    public static final String UNKNOWN = "XX XX";

    public static String getRealAddressByIP(String ip) {
        if (StringUtil.isBlank(ip)) {
            return UNKNOWN;
        }
        // 内网不查询
        ip = StringUtil.contains(ip, "0:0:0:0:0:0:0:1" ) ? "127.0.0.1" : StringUtil.cleanHtmlTag(ip);
        if (NetUtil.isInnerIP(ip)) {
            return "内网IP";
        }
        return RegionUtil.getCityInfo(ip);
    }
}

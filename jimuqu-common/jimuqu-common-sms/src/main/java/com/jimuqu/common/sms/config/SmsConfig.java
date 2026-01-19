package com.jimuqu.common.sms.config;

import org.dromara.sms4j.api.dao.SmsDao;
import org.dromara.sms4j.api.dao.SmsDaoDefaultImpl;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

/**
 * 短信配置
 *
 * @author chengliang
 * @date 2026/01/08
 */
@Configuration
public class SmsConfig {

    @Bean
    public SmsDao  smsDao(){
       return SmsDaoDefaultImpl.getInstance();
    }

}

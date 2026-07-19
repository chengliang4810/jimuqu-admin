package com.jimuqu.common.mail.config;

import cn.hutool.v7.extra.mail.MailAccount;
import com.jimuqu.common.mail.config.properties.MailProperties;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * Mail 配置
 */
@Configuration
@Condition(onBean= MailProperties.class)
public class MailConfig {

    @Bean
    public MailAccount mailAccount(@Inject MailProperties mailProperties) {
        MailAccount account = new MailAccount();
        account.setHost(mailProperties.getHost());
        account.setPort(mailProperties.getPort());
        account.setAuth(mailProperties.getAuth());
        account.setFrom(mailProperties.getFrom());
        account.setUser(mailProperties.getUser());
        if (mailProperties.getPass() != null) {
            account.setPass(mailProperties.getPass().toCharArray());
        }
        if (mailProperties.getPort() != null) {
            account.setSocketFactoryPort(mailProperties.getPort());
        }
        if (mailProperties.getStarttlsEnable() != null) {
            account.setStarttlsEnable(mailProperties.getStarttlsEnable());
        }
        account.setSslEnable(mailProperties.getSslEnable());
        if (mailProperties.getTimeout() != null) {
            account.setTimeout(mailProperties.getTimeout());
        }
        if (mailProperties.getConnectionTimeout() != null) {
            account.setConnectionTimeout(mailProperties.getConnectionTimeout());
        }
        return account;
    }

}

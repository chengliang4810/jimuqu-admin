package com.jimuqu.auth.service;

import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.exception.user.CaptchaException;
import org.junit.jupiter.api.Test;
import org.noear.solon.data.cache.CacheService;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SysRegisterServiceCaptchaTest {

    @Test
    void missingCodeIsCaptchaErrorInsteadOfNullPointer() throws Exception {
        CacheService cache = (CacheService) Proxy.newProxyInstance(
                CacheService.class.getClassLoader(), new Class<?>[]{CacheService.class},
                (proxy, method, args) -> "get".equals(method.getName())
                        && (GlobalConstants.CAPTCHA_CODE_KEY + "uuid").equals(args[0]) ? "1234" : null);
        SysRegisterService service = new SysRegisterService();
        Field field = SysRegisterService.class.getDeclaredField("cacheService");
        field.setAccessible(true);
        field.set(service, cache);

        assertThrows(CaptchaException.class,
                () -> service.validateCaptcha("user", null, "uuid"));
    }
}

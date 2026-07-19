package com.jimuqu.auth.service;

import com.jimuqu.common.core.exception.ServiceException;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerificationCodeServiceTest {

    @Test
    void rejectsFailedSmsBeforeCachingCode() {
        SmsResponse success = new SmsResponse();
        success.setSuccess(true);
        assertDoesNotThrow(() -> VerificationCodeService.ensureSmsSent(success));

        SmsResponse failed = new SmsResponse();
        failed.setData("provider rejected request");
        ServiceException exception = assertThrows(ServiceException.class,
                () -> VerificationCodeService.ensureSmsSent(failed));
        assertEquals("provider rejected request", exception.getMessage());
        assertThrows(ServiceException.class, () -> VerificationCodeService.ensureSmsSent(null));
    }
}

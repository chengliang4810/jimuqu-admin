package com.jimuqu.common.web.filter;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionFilterTest {

    @Test
    void preservesServiceErrorCodeAndMessage() {
        R<Void> response = GlobalExceptionFilter.serviceError(new ServiceException("数据已存在", 409));

        assertEquals(409, response.getCode());
        assertEquals("数据已存在", response.getMsg());
    }
}

package com.jimuqu.common.mybatis.handler;

import com.jimuqu.common.core.domain.R;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.Test;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MybatisExceptionHandlerTest {

    @Test
    void mapsWrappedMysqlDuplicateKeyToConflictEnvelope() {
        Throwable exception = new PersistenceException(
                new SQLIntegrityConstraintViolationException("Duplicate entry 'admin'", "23000", 1062));

        R<Void> response = MybatisExceptionHandler.resolve(exception);

        assertEquals(409, response.getCode());
        assertEquals("数据库中已存在该记录，请联系管理员确认", response.getMsg());
    }

    @Test
    void mapsConnectionFailureWithoutExposingDriverDetails() {
        Throwable exception = new PersistenceException(
                new SQLNonTransientConnectionException("Access denied for password", "08001"));

        R<Void> response = MybatisExceptionHandler.resolve(exception);

        assertEquals(500, response.getCode());
        assertEquals("数据库连接异常，请联系管理员确认", response.getMsg());
    }

    @Test
    void leavesUnrelatedExceptionsForGlobalHandling() {
        assertNull(MybatisExceptionHandler.resolve(new IllegalArgumentException("invalid")));
        assertNull(MybatisExceptionHandler.resolve(new NotLoginException("token expired")));
    }

    @Test
    void usesStableMessageWhenPersistenceExceptionHasNoMessage() {
        R<Void> response = MybatisExceptionHandler.resolve(new PersistenceException((String) null));

        assertEquals(500, response.getCode());
        assertEquals("数据库访问异常，请联系管理员确认", response.getMsg());
        assertNull(response.getData());
    }

    @Test
    void preservesAuthenticationFailureWrappedByPersistenceLayer() {
        R<Void> response = MybatisExceptionHandler.resolve(
                new PersistenceException(new NotLoginException("token expired")));

        assertEquals(401, response.getCode());
        assertEquals("认证失败，无法访问系统资源", response.getMsg());
        assertNull(response.getData());
    }

    private static final class NotLoginException extends RuntimeException {
        private NotLoginException(String message) {
            super(message);
        }
    }
}

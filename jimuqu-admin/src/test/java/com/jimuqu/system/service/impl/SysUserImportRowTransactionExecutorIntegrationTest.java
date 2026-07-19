package com.jimuqu.system.service.impl;

import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.sms.config.SmsConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.noear.solon.annotation.Import;
import org.noear.solon.data.annotation.TransactionAnno;
import org.noear.solon.data.tran.TranPolicy;
import org.noear.solon.data.tran.TranUtils;
import org.noear.solon.test.SolonTest;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(SmsConfig.class)
@SolonTest(scanning = false, enableHttp = false, debug = false, delay = 0)
public class SysUserImportRowTransactionExecutorIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void successfulRowsSurviveAggregateRollbackAndFailedRowsRollbackIndependently() throws Exception {
        try (HikariDataSource dataSource = dataSource()) {
            createTable(dataSource);
            SysUserImportRowTransactionExecutor executor = new SysUserImportRowTransactionExecutor();

            ServiceException aggregate = assertThrows(ServiceException.class, () -> executeOuterTransaction(() -> {
                executor.execute(() -> insert(dataSource, "first-valid"));
                throw new ServiceException("用户导入失败，共 1 条");
            }));
            assertEquals("用户导入失败，共 1 条", aggregate.getMessage());

            assertThrows(IllegalArgumentException.class, () -> executor.execute(() -> {
                insert(dataSource, "failed-row");
                throw new IllegalArgumentException("部门不存在");
            }));
            executor.execute(() -> insert(dataSource, "last-valid"));

            assertEquals(TranPolicy.requires_new,
                    SysUserImportRowTransactionExecutor.transactionPolicy());
            assertEquals(List.of("first-valid", "last-valid"), selectNames(dataSource));
        }
    }

    private HikariDataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("user-import-transaction-test");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + tempDir.resolve("user-import.db").toAbsolutePath());
        config.setMaximumPoolSize(3);
        return new HikariDataSource(config);
    }

    private static void createTable(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("create table imported_user (name varchar(64) primary key)");
        }
    }

    private static void insert(DataSource dataSource, String name) {
        try {
            Connection connection = TranUtils.getConnection(dataSource);
            try (PreparedStatement statement = connection.prepareStatement(
                    "insert into imported_user(name) values (?)")) {
                statement.setString(1, name);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("写入测试用户失败", exception);
        }
    }

    private static List<String> selectNames(DataSource dataSource) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select name from imported_user order by name")) {
            while (resultSet.next()) {
                names.add(resultSet.getString(1));
            }
        }
        return names;
    }

    private static void executeOuterTransaction(Runnable task) {
        try {
            TranUtils.execute(new TransactionAnno().policy(TranPolicy.required), task::run);
        } catch (RuntimeException | Error exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new IllegalStateException("执行外层测试事务失败", throwable);
        }
    }
}

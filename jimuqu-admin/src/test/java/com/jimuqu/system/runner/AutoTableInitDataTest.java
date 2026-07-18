package com.jimuqu.system.runner;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AutoTableInitDataTest {

    @Test
    void mysqlSeedScriptIsPackagedAndParseable() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("sql/MySQL/jimuqu.sql")) {
            assertNotNull(input);
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(102, CCJSqlParserUtil.parseStatements(sql).getStatements().size());
        }
    }
}

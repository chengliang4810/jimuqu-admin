package com.jimuqu.test.coverage;

import com.jimuqu.Application;
import org.junit.jupiter.api.Test;
import org.noear.solon.test.SolonTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 导出 Solon 实际注册的应用路由，作为 HTTP 接口覆盖率分母。
 */
@SolonTest(value = Application.class, env = "test", debug = false)
public class RuntimeRouteInventoryTest {

    @Test
    void printsRegisteredApplicationRoutes() {
        RuntimeRouteCoverage.Report report = RuntimeRouteCoverage
                .snapshotApplicationRoutes(Set.of())
                .report();

        assertTrue(report.registeredCount() > 0, "No application HTTP routes were registered");
        assertEquals(report.registeredCount(), report.missing().size());

        System.out.println("RUNTIME_ROUTE_INVENTORY_BEGIN");
        report.missing().forEach(System.out::println);
        System.out.printf("RUNTIME_ROUTE_INVENTORY_END count=%d%n", report.registeredCount());
    }
}

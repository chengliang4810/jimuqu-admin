package com.jimuqu.test.http;

import com.jimuqu.Application;
import com.jimuqu.test.coverage.RuntimeRouteCoverage;
import org.junit.jupiter.api.Test;
import org.noear.solon.test.SolonTest;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** 确保每条运行时 HTTP 路由恰好归属一个真实契约测试分片。 */
@SolonTest(value = Application.class, env = "test", debug = false)
public class HttpRouteOwnershipTest {

    @Test
    void everyApplicationRouteHasExactlyOneContractOwner() {
        List<Predicate<RuntimeRouteCoverage.RouteKey>> owners = List.of(
                HealthAuthUserHttpContractTest::ownsRoute,
                RbacHttpContractTest::ownsRoute,
                ResourceMonitorHttpContractTest::ownsRoute,
                ConfigurationMessagingHttpContractTest::ownsRoute
        );
        RuntimeRouteCoverage.Report inventory = RuntimeRouteCoverage
                .snapshotApplicationRoutes(Set.of())
                .report();

        assertFalse(inventory.missing().isEmpty(), "运行时未注册任何应用 HTTP 路由");
        for (RuntimeRouteCoverage.RouteKey route : inventory.missing()) {
            long ownerCount = owners.stream().filter(owner -> owner.test(route)).count();
            assertEquals(1L, ownerCount, () -> "路由必须恰好归属一个契约测试分片: " + route);
        }
    }
}

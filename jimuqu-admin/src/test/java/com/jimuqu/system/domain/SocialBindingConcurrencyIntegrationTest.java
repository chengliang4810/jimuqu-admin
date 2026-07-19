package com.jimuqu.system.domain;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.Application;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.system.mapper.SysSocialMapper;
import com.jimuqu.system.service.SysSocialService;
import me.zhyd.oauth.model.AuthUser;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SolonTest(value = Application.class, env = "test", debug = false)
public class SocialBindingConcurrencyIntegrationTest {

    private static final String BOUND_MESSAGE = "此三方账号已经被绑定!";

    @Inject
    private SysSocialService socialService;

    @Inject
    private SysSocialMapper socialMapper;

    @Test
    void concurrentBindingsRemainUniqueAndKeepUpstreamErrorMessage() throws Exception {
        String suffix = Long.toUnsignedString(System.nanoTime(), 36);
        String authSource = "it_auth_" + suffix;
        String sourceSource = "it_source_" + suffix;
        try {
            AuthUser sameAccount = authUser(authSource, "same-account");
            List<Throwable> authIdResults = race(
                    () -> socialService.bind(1L, sameAccount),
                    () -> socialService.bind(2L, sameAccount));
            assertEquals(1L, countBySource(authSource), "同一第三方账号并发绑定只能保留一条记录");
            assertEquals(1L, authIdResults.stream().filter(error -> error == null).count());
            assertUpstreamFailures(authIdResults);

            Throwable duplicate = capture(() -> socialService.bind(1L, sameAccount));
            assertInstanceOf(ServiceException.class, duplicate);
            assertEquals(BOUND_MESSAGE, duplicate.getMessage());

            List<Throwable> sourceResults = race(
                    () -> socialService.bind(1L, authUser(sourceSource, "account-a")),
                    () -> socialService.bind(1L, authUser(sourceSource, "account-b")));
            assertEquals(1L, countBySource(sourceSource), "同一用户同一来源并发绑定只能保留一条记录");
            assertTrue(sourceResults.stream().anyMatch(error -> error == null));
            assertUpstreamFailures(sourceResults);
        } finally {
            socialMapper.delete(where -> where.in(SysSocial::getSource, List.of(authSource, sourceSource)));
        }
    }

    private List<Throwable> race(ThrowingRunnable first, ThrowingRunnable second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> firstResult = pool.submit(() -> awaitAndCapture(ready, start, first));
            Future<Throwable> secondResult = pool.submit(() -> awaitAndCapture(ready, start, second));
            assertTrue(ready.await(5, TimeUnit.SECONDS), "并发绑定任务未就绪");
            start.countDown();
            return Arrays.asList(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "并发绑定线程池未退出");
        }
    }

    private Throwable awaitAndCapture(CountDownLatch ready, CountDownLatch start, ThrowingRunnable action) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new AssertionError("并发绑定开始信号超时");
            }
            action.run();
            return null;
        } catch (Throwable error) {
            return error;
        }
    }

    private void assertUpstreamFailures(List<Throwable> results) {
        results.stream().filter(error -> error != null).forEach(error -> {
            assertInstanceOf(ServiceException.class, error);
            assertEquals(BOUND_MESSAGE, error.getMessage());
        });
    }

    private long countBySource(String source) {
        return QueryChain.of(socialMapper).eq(SysSocial::getSource, source).count();
    }

    private static Throwable capture(ThrowingRunnable action) {
        try {
            action.run();
            return null;
        } catch (Throwable error) {
            return error;
        }
    }

    private static AuthUser authUser(String source, String uuid) {
        return AuthUser.builder()
                .source(source)
                .uuid(uuid)
                .username(uuid)
                .nickname(uuid)
                .build();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

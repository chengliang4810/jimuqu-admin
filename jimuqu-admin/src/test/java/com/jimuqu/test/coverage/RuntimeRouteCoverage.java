package com.jimuqu.test.coverage;

import org.noear.solon.Solon;
import org.noear.solon.core.handle.Action;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.handle.MethodType;
import org.noear.solon.core.route.Routing;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

/** Records application routes reached by successful real HTTP integration cases. */
public final class RuntimeRouteCoverage {

    private static final Set<MethodType> HTTP_METHODS = Set.of(
            MethodType.GET,
            MethodType.POST,
            MethodType.PUT,
            MethodType.DELETE,
            MethodType.PATCH,
            MethodType.HTTP,
            MethodType.ALL
    );

    private final List<Routing<Handler>> routes;
    private final Set<RouteKey> registered;
    private final Set<RouteKey> covered = new ConcurrentSkipListSet<>();
    private final Set<String> unexpected = new ConcurrentSkipListSet<>();

    private RuntimeRouteCoverage(List<Routing<Handler>> routes, Set<RouteKey> exclusions) {
        this.routes = routes;
        this.registered = new TreeSet<>();
        routes.stream()
                .map(RuntimeRouteCoverage::keyOf)
                .filter(key -> !exclusions.contains(key))
                .forEach(registered::add);
    }

    public static RuntimeRouteCoverage snapshotApplicationRoutes(Set<RouteKey> exclusions) {
        List<Routing<Handler>> routes = Solon.app().router().findAll().stream()
                .filter(RuntimeRouteCoverage::isApplicationHttpRoute)
                .sorted(Comparator.comparing((Routing<Handler> route) -> route.method().name())
                        .thenComparing(Routing::path))
                .toList();
        return new RuntimeRouteCoverage(routes, Set.copyOf(exclusions));
    }

    public static boolean supportsHttpMethod(MethodType method) {
        return HTTP_METHODS.contains(method);
    }

    public void record(String method, String requestPath) {
        MethodType methodType;
        try {
            methodType = MethodType.valueOf(method.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            unexpected.add(RouteKey.of(method, requestPath).toString());
            return;
        }

        routes.stream()
                .filter(route -> route.matches(methodType, requestPath))
                .max(Comparator.comparingInt(route -> route.degrees(methodType, requestPath)))
                .map(RuntimeRouteCoverage::keyOf)
                .filter(registered::contains)
                .ifPresentOrElse(covered::add,
                        () -> unexpected.add(RouteKey.of(method, requestPath).toString()));
    }

    public Report report() {
        Set<RouteKey> missing = new TreeSet<>(registered);
        missing.removeAll(covered);
        int coveredCount = registered.size() - missing.size();
        double percentage = registered.isEmpty() ? 100.0 : coveredCount * 100.0 / registered.size();
        return new Report(
                registered.size(),
                coveredCount,
                percentage,
                List.copyOf(missing),
                List.copyOf(unexpected)
        );
    }

    public void assertComplete() {
        Report report = report();
        if (!report.complete()) {
            throw new AssertionError("API route coverage is incomplete: " + report);
        }
    }

    private static boolean isApplicationHttpRoute(Routing<Handler> route) {
        if (!supportsHttpMethod(route.method())) {
            return false;
        }
        if (route.target() instanceof Action action) {
            return action.controller().clz().getName().startsWith("com.jimuqu.");
        }
        return route.target().getClass().getName().startsWith("com.jimuqu.");
    }

    private static RouteKey keyOf(Routing<Handler> route) {
        return RouteKey.of(route.method().name(), route.path());
    }

    public record RouteKey(String method, String path) implements Comparable<RouteKey> {

        public RouteKey {
            method = method.toUpperCase(Locale.ROOT);
            path = normalizePath(path);
        }

        public static RouteKey of(String method, String path) {
            return new RouteKey(method, path);
        }

        private static String normalizePath(String value) {
            String path = value == null || value.isBlank() ? "/" : value.trim().replaceAll("/{2,}", "/");
            if (!path.startsWith("/")) { path = "/" + path; }
            if (path.length() > 1 && path.endsWith("/")) { path = path.substring(0, path.length() - 1); }
            return path;
        }

        @Override
        public int compareTo(RouteKey other) {
            int methodOrder = method.compareTo(other.method);
            return methodOrder == 0 ? path.compareTo(other.path) : methodOrder;
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    public record Report(
            int registeredCount,
            int coveredCount,
            double percentage,
            List<RouteKey> missing,
            List<String> unexpected
    ) {
        public boolean complete() {
            return missing.isEmpty() && unexpected.isEmpty();
        }
    }
}

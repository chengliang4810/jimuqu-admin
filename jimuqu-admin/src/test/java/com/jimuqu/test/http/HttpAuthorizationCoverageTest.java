package com.jimuqu.test.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.jimuqu.Application;
import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import com.jimuqu.common.core.utils.JsonUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.noear.solon.Solon;
import org.noear.solon.core.handle.Action;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.handle.MethodType;
import org.noear.solon.core.route.Routing;
import org.noear.solon.test.SolonTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** 运行时逐路由验证登录与权限拦截，新增受保护接口会自动进入测试分母。 */
@SolonTest(value = Application.class, env = "test", debug = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpAuthorizationCoverageTest {

    private static final Set<String> PUBLIC_ROUTES = Set.of("GET /");
    private static final Set<String> MANUALLY_PROTECTED_IGNORED_ROUTES = Set.of(
            "GET /auth/codes", "POST /auth/social/callback", "DELETE /auth/unlock/{socialId}",
            "GET /resource/message/close"
    );
    private static final String JSON_BODY = JsonUtil.toString(validBindingBody());

    private HttpClient client;
    private URI baseUri;
    private String deniedToken;

    @BeforeAll
    void setUp() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        int port = Solon.cfg().getInt("server.port", Integer.parseInt(System.getenv("JIMU_TEST_SERVER_PORT")));
        baseUri = URI.create("http://127.0.0.1:" + port + "/");
        deniedToken = new HttpApiTestSupport(key -> true)
                .login("no_permission", HttpApiTestSupport.DEFAULT_PASSWORD);
    }

    @Test
    void everyProtectedHttpOperationRejectsAnonymousRequests() throws Exception {
        List<SecuredRoute> routes = securedRoutes();
        assertFalse(routes.isEmpty(), "运行时没有发现受保护 HTTP 操作");
        for (SecuredRoute route : routes) {
            Response response = request(route, null);
            assertEquals(401, response.status(), () -> route.key() + " 未登录 HTTP 状态错误: " + response.body());
            assertEquals(401, response.code(), () -> route.key() + " 未登录响应码错误: " + response.body());
        }
    }

    @Test
    void everyPermissionMarkerRejectsUnprivilegedRequests() throws Exception {
        Map<String, SecuredRoute> representatives = new TreeMap<>();
        for (SecuredRoute route : securedRoutes()) {
            for (String permission : route.permissions()) {
                representatives.merge(permission, route, HttpAuthorizationCoverageTest::preferSimpleBinding);
            }
        }
        assertFalse(representatives.isEmpty(), "运行时没有发现权限标记");
        for (Map.Entry<String, SecuredRoute> entry : representatives.entrySet()) {
            Response response = request(entry.getValue(), deniedToken);
            assertEquals(403, response.status(), () -> entry.getKey() + " 未返回 403，路由 "
                    + entry.getValue().key() + ": " + response.body());
            assertEquals(403, response.code(), () -> entry.getKey() + " 响应码错误: " + response.body());
        }
    }

    private List<SecuredRoute> securedRoutes() {
        List<SecuredRoute> routes = new ArrayList<>();
        for (Routing<Handler> routing : Solon.app().router().findAll()) {
            if (!(routing.target() instanceof Action action)
                    || !action.controller().clz().getName().startsWith("com.jimuqu.")) {
                continue;
            }
            String method = routing.method().name();
            if (!Set.of("GET", "POST", "PUT", "DELETE", "PATCH").contains(method)) {
                continue;
            }
            boolean ignored = action.controller().clz().isAnnotationPresent(SaIgnore.class)
                    || action.method().getAnnotation(SaIgnore.class) != null;
            String key = method + " " + routing.path();
            if (PUBLIC_ROUTES.contains(key)) {
                continue;
            }
            if (ignored && !MANUALLY_PROTECTED_IGNORED_ROUTES.contains(key)) {
                continue;
            }
            SaCheckPermission check = action.method().getAnnotation(SaCheckPermission.class);
            routes.add(new SecuredRoute(method, routing.path(), action.method().getAnnotation(ApiEncrypt.class) != null,
                    check == null ? List.of() : List.of(check.value())));
        }
        return routes.stream().sorted(Comparator.comparing(SecuredRoute::key)).toList();
    }

    private Response request(SecuredRoute route, String token) throws Exception {
        String path = route.path().replaceAll("\\{[^/]+}", "1");
        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.noBody();
        String contentType = null;
        String body = JSON_BODY;
        if (route.method().equals("POST") || route.method().equals("PUT") || route.method().equals("PATCH")) {
            if (route.path().endsWith("/importData") || route.path().endsWith("/upload")) {
                String boundary = "jimu-auth-boundary";
                body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n"
                        + "Content-Type: text/plain\r\n\r\ntest\r\n--" + boundary + "--\r\n";
                contentType = "multipart/form-data; boundary=" + boundary;
            } else if (route.path().endsWith("/export") || route.path().endsWith("/importTemplate")) {
                body = "pageNum=1&pageSize=10";
                contentType = "application/x-www-form-urlencoded";
            } else {
                contentType = "application/json";
            }
            publisher = HttpRequest.BodyPublishers.ofString(body);
        }

        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path.substring(1)))
                .timeout(Duration.ofSeconds(10));
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (route.encrypted()) {
            String aesKey = ApiCryptoUtil.randomAesKey();
            String publicKey = requestPublicKey();
            request.header(Solon.cfg().get("api-decrypt.headerFlag", "encrypt-key"),
                    ApiCryptoUtil.encryptByRsa(Base64.getEncoder().encodeToString(aesKey.getBytes()), publicKey));
            publisher = HttpRequest.BodyPublishers.ofString(ApiCryptoUtil.encryptByAes(body, aesKey));
        }
        request.method(route.method(), publisher);
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        Object code = JsonUtil.toMap(response.body()).get("code");
        return new Response(response.statusCode(), code instanceof Number number ? number.intValue() : -1,
                response.body());
    }

    private String requestPublicKey() throws Exception {
        byte[] encoded = Base64.getDecoder().decode(Solon.cfg().get("api-decrypt.privateKey"));
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(encoded));
        return Base64.getEncoder().encodeToString(KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent())).getEncoded());
    }

    private static SecuredRoute preferSimpleBinding(SecuredRoute left, SecuredRoute right) {
        return methodRank(left.method()) <= methodRank(right.method()) ? left : right;
    }

    private static int methodRank(String method) {
        return switch (method) {
            case "GET" -> 0;
            case "DELETE" -> 1;
            case "POST" -> 2;
            default -> 3;
        };
    }

    private static Map<String, Object> validBindingBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        for (String id : List.of("id", "userId", "roleId", "menuId", "deptId", "postId", "dictId",
                "dictCode", "configId", "noticeId", "ossConfigId")) {
            body.put(id, 1);
        }
        body.putAll(Map.ofEntries(
                Map.entry("userName", "permission_probe"), Map.entry("nickName", "Permission Probe"),
                Map.entry("password", "Admin123!"), Map.entry("roleName", "Permission Probe"),
                Map.entry("roleKey", "permission_probe"), Map.entry("roleSort", 1),
                Map.entry("menuName", "Permission Probe"), Map.entry("menuType", "C"),
                Map.entry("path", "permission-probe"), Map.entry("orderNum", 1),
                Map.entry("parentId", 0), Map.entry("deptName", "Permission Probe"),
                Map.entry("postName", "Permission Probe"), Map.entry("postCode", "permission_probe"),
                Map.entry("postSort", 1), Map.entry("dictName", "Permission Probe"),
                Map.entry("dictType", "permission_probe"), Map.entry("dictKey", "permission_probe"),
                Map.entry("dictLabel", "Permission Probe"), Map.entry("dictValue", "probe"),
                Map.entry("dictTypeKey", "permission_probe"), Map.entry("configName", "Permission Probe"),
                Map.entry("configKey", "permission.probe"), Map.entry("configValue", "probe"),
                Map.entry("clientKey", "permission_probe"), Map.entry("clientSecret", "permission_probe"),
                Map.entry("clientId", "permission_probe"), Map.entry("grantType", "password"),
                Map.entry("noticeTitle", "Permission Probe"), Map.entry("noticeType", "1"),
                Map.entry("noticeContent", "Permission Probe"), Map.entry("status", "0"),
                Map.entry("ids", List.of(1)), Map.entry("roleIds", List.of(1)), Map.entry("menuIds", List.of(1))
        ));
        return body;
    }

    private record SecuredRoute(String method, String path, boolean encrypted, List<String> permissions) {
        String key() { return method + " " + path; }
    }

    private record Response(int status, int code, String body) { }
}

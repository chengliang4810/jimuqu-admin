package com.jimuqu.common.web.sensitive;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.sensitive.annotation.Sensitive;
import com.jimuqu.common.core.sensitive.enums.SensitiveType;
import org.junit.jupiter.api.Test;
import com.jimuqu.common.core.utils.JsonUtil;
import org.apache.fory.json.annotation.JsonProperty;
import org.apache.fory.json.annotation.JsonIgnore;
import org.noear.solon.core.handle.ContextEmpty;
import com.jimuqu.common.web.serialization.ForyJsonStringSerializer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SensitiveJsonRenderTest {

    @Test
    void preservesComputedAndGetterValuesWhileMaskingSensitiveFields() throws Throwable {
        SensitiveJsonRender render = new SensitiveJsonRender(new ForyJsonStringSerializer());

        String json = render.renderAndReturn(new Envelope(new RoleView()), ContextEmpty.create());
        Map<?, ?> response = JsonUtil.toObject(json, Map.class);
        Map<?, ?> data = (Map<?, ?>) response.get("data");

        assertEquals(1, data.get("roleId"));
        assertEquals(true, data.get("superAdmin"));
        assertEquals("enabled", data.get("status"));
        assertEquals("138****5678", data.get("phoneNumber"));
        assertFalse(data.containsKey("password"));
    }

    @Test
    void preservesExactEnvelopeAndPaginationSurface() throws Throwable {
        SensitiveJsonRender render = new SensitiveJsonRender(new ForyJsonStringSerializer());
        PaginationView page = new PaginationView();

        String json = render.renderAndReturn(R.ok(page), ContextEmpty.create());
        Map<?, ?> response = JsonUtil.toObject(json, Map.class);
        Map<?, ?> data = (Map<?, ?>) response.get("data");

        assertEquals(Set.of("code", "msg", "data"), response.keySet());
        assertEquals(Set.of("rows", "total"), data.keySet());
    }

    private static final class PaginationView {
        private final List<String> rows = List.of("row");
        private final long total = 1L;
        private final transient int currentPage = 1;
        private final transient int pageSize = 20;

        public List<String> getRows() {
            return rows;
        }

        public long getTotal() {
            return total;
        }

        public int getOffset() {
            return (currentPage - 1) * pageSize;
        }
    }

    private static final class Envelope {
        private final RoleView data;

        private Envelope(RoleView data) {
            this.data = data;
        }

        public RoleView getData() {
            return data;
        }
    }

    private static final class RoleView {
        @JsonProperty("roleId")
        private final Long id = 1L;

        private final String status = "0";

        @Sensitive(type = SensitiveType.MOBILE)
        @JsonProperty("phoneNumber")
        private final String mobile = "13812345678";

        @JsonIgnore
        private final String password = "secret";

        public Long getId() {
            return id;
        }

        public String getStatus() {
            return "0".equals(status) ? "enabled" : "disabled";
        }

        public String getMobile() {
            return mobile;
        }

        public String getPassword() {
            return password;
        }

        @JsonProperty("superAdmin")
        public boolean isSuperAdmin() {
            return id == 1L;
        }
    }
}

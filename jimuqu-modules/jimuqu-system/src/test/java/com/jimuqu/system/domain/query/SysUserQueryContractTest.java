package com.jimuqu.system.domain.query;

import com.jimuqu.common.core.utils.DateUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SysUserQueryContractTest {

    @Test
    void mapsBellAliasesAndNestedCreationTimeRange() {
        SysUserQuery query = new SysUserQuery();
        query.setPhoneNumber("13800000000");
        query.setParams(Map.of(
                "beginTime", "2026-07-01 00:00:00",
                "endTime", "2026-07-31 23:59:59"));

        query.beforeBuildCondition();

        assertEquals("13800000000", query.getPhonenumber());
        assertEquals("2026-07-01 00:00:00",
                DateUtil.parseDateToStr(DateUtil.YYYY_MM_DD_HH_MM_SS, query.getCreateTime().get(0)));
        assertEquals("2026-07-31 23:59:59",
                DateUtil.parseDateToStr(DateUtil.YYYY_MM_DD_HH_MM_SS, query.getCreateTime().get(1)));
    }
}

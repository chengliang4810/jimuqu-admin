package com.jimuqu.system.domain;

import com.jimuqu.system.domain.bo.SysDictDataBo;
import com.jimuqu.system.domain.query.SysDictDataQuery;
import com.jimuqu.system.domain.vo.SysDictDataVo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DictSortTypeParityTest {

    @Test
    void dictSortUsesUpstreamIntegerTypeAcrossLayers() throws NoSuchFieldException {
        for (Class<?> type : new Class<?>[]{
                SysDictData.class, SysDictDataBo.class, SysDictDataQuery.class, SysDictDataVo.class}) {
            assertEquals(Integer.class, type.getDeclaredField("dictSort").getType(), type.getName());
        }
    }
}

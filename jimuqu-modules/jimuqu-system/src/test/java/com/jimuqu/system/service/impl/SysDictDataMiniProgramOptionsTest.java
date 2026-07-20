package com.jimuqu.system.service.impl;

import com.jimuqu.system.domain.vo.SysDictDataVo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SysDictDataMiniProgramOptionsTest {

    @Test
    void hidesMiniProgramGrantAndDeviceOptionsUntilAnAdapterIsAvailable() {
        List<SysDictDataVo> options = List.of(option("password"), option("xcx"));

        assertEquals(List.of("password"), values(
                SysDictDataServiceImpl.hideUnavailableMiniProgramOptions(
                        "sys_grant_type", options, false)));
        assertEquals(List.of("password"), values(
                SysDictDataServiceImpl.hideUnavailableMiniProgramOptions(
                        "sys_device_type", options, false)));
        assertEquals(List.of("password", "xcx"), values(
                SysDictDataServiceImpl.hideUnavailableMiniProgramOptions(
                        "sys_grant_type", options, true)));
        assertEquals(List.of("password", "xcx"), values(
                SysDictDataServiceImpl.hideUnavailableMiniProgramOptions(
                        "another_dict", options, false)));
    }

    private SysDictDataVo option(String value) {
        SysDictDataVo option = new SysDictDataVo();
        option.setDictValue(value);
        return option;
    }

    private List<String> values(List<SysDictDataVo> options) {
        return options.stream().map(SysDictDataVo::getDictValue).toList();
    }
}

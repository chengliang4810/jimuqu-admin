package com.jimuqu.system.service.impl;

import com.jimuqu.common.core.constant.CacheConstants;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.system.domain.SysDictData;
import com.jimuqu.system.domain.SysDictType;
import com.jimuqu.system.domain.bo.SysDictTypeBo;
import com.jimuqu.system.domain.bo.SysDictTypeBoToSysDictTypeMapperImpl;
import com.jimuqu.system.mapper.SysDictDataMapper;
import com.jimuqu.system.mapper.SysDictTypeMapper;
import com.jimuqu.system.service.SysDictDataService;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.noear.solon.annotation.Import;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.test.SolonTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import(SysDictTypeBoToSysDictTypeMapperImpl.class)
@SolonTest(
        scanning = false,
        enableHttp = false,
        debug = false,
        delay = 0,
        properties = "solon.plugin.exclude[0]=org.dromara.x.file.storage.solon.DromaraXFileStoragePluginImpl"
)
public class SysDictTypeServiceImplParityTest {

    @Test
    void rejectsMissingTypeBeforeUpdatingDictionaryData() {
        SysDictTypeMapper typeMapper = mock(SysDictTypeMapper.class);
        SysDictDataMapper dataMapper = mock(SysDictDataMapper.class);
        CacheService cacheService = mock(CacheService.class);
        SysDictTypeServiceImpl service = service(typeMapper, dataMapper, cacheService);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.updateByBo(updateBo()));

        assertEquals("字典类型不存在", error.getMessage());
        verifyNoInteractions(dataMapper, cacheService);
    }

    @Test
    void failedTypeUpdateRaisesExceptionSoChildUpdateRollsBack() {
        SysDictTypeMapper typeMapper = mock(SysDictTypeMapper.class);
        SysDictDataMapper dataMapper = mock(SysDictDataMapper.class);
        CacheService cacheService = mock(CacheService.class);
        SysDictTypeServiceImpl service = service(typeMapper, dataMapper, cacheService);
        when(typeMapper.getById(7L)).thenReturn(existingType());
        when(typeMapper.update(any(SysDictType.class))).thenReturn(0);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.updateByBo(updateBo()));

        assertEquals("操作失败", error.getMessage());
        Invocation childUpdate = mockingDetails(dataMapper).getInvocations().stream()
                .filter(invocation -> "update".equals(invocation.getMethod().getName()))
                .findFirst()
                .orElseThrow();
        SysDictData updatedData = childUpdate.getArgument(0);
        assertEquals("new-key", updatedData.getDictTypeKey());
        verifyNoInteractions(cacheService);
    }

    @Test
    void successfulTypeUpdateEvictsOldAndNewDictionaryCaches() {
        SysDictTypeMapper typeMapper = mock(SysDictTypeMapper.class);
        SysDictDataMapper dataMapper = mock(SysDictDataMapper.class);
        CacheService cacheService = mock(CacheService.class);
        SysDictTypeServiceImpl service = service(typeMapper, dataMapper, cacheService);
        when(typeMapper.getById(7L)).thenReturn(existingType());
        when(typeMapper.update(any(SysDictType.class))).thenReturn(1);

        assertTrue(service.updateByBo(updateBo()));

        verify(cacheService).remove(CacheConstants.SYS_DICT_KEY + "old-key");
        verify(cacheService).remove(CacheConstants.SYS_DICT_KEY + "new-key");
    }

    private static SysDictTypeServiceImpl service(SysDictTypeMapper typeMapper,
                                                  SysDictDataMapper dataMapper,
                                                  CacheService cacheService) {
        return new SysDictTypeServiceImpl(typeMapper, dataMapper,
                mock(SysDictDataService.class), cacheService);
    }

    private static SysDictType existingType() {
        return new SysDictType().setDictId(7L).setDictKey("old-key");
    }

    private static SysDictTypeBo updateBo() {
        return new SysDictTypeBo().setDictId(7L).setDictKey("new-key")
                .setDictName("测试字典").setDictType("L");
    }
}

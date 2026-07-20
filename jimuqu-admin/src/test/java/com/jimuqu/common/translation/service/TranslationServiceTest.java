package com.jimuqu.common.translation.service;

import com.jimuqu.common.core.domain.PageResult;
import com.jimuqu.common.core.service.DictService;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslatableEnum;
import com.jimuqu.common.translation.core.TranslationInterface;
import com.jimuqu.common.translation.core.impl.DefaultTranslator;
import com.jimuqu.common.translation.core.impl.DictTranslator;
import com.jimuqu.common.translation.core.impl.EnumTranslator;
import com.jimuqu.common.translation.enums.TransType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationServiceTest {

    private TranslationService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new TranslationService();
        Field transMap = TranslationService.class.getDeclaredField("transMap");
        transMap.setAccessible(true);
        transMap.set(service, Map.<String, TranslationInterface>of("defaultTranslator", new DefaultTranslator()));
    }

    @Test
    void translatesRowsInsideUnifiedPageResult() {
        Item item = new Item("1");
        service.translate(new PageResult<>(List.of(item), 1));
        assertEquals("启用", item.statusName);
    }

    @Test
    void translatesNestedCollectionsMapsArraysAndHandlesCycles() {
        Item listItem = new Item("0");
        Item mapItem = new Item("1");
        Item arrayItem = new Item("missing");
        Container container = new Container();
        container.items = List.of(listItem);
        container.index = Map.of("item", mapItem);
        container.array = new Item[]{arrayItem};
        container.self = container;

        service.translate(container);

        assertEquals("停用", listItem.statusName);
        assertEquals("启用", mapItem.statusName);
        assertEquals("未知", arrayItem.statusName);
    }

    @Test
    void translatesMultiValueDictAndEnumFields() throws Exception {
        DictTranslator dictTranslator = new DictTranslator();
        Field dictService = DictTranslator.class.getDeclaredField("dictService");
        dictService.setAccessible(true);
        dictService.set(dictTranslator, new StubDictService());

        Field transMap = TranslationService.class.getDeclaredField("transMap");
        transMap.setAccessible(true);
        transMap.set(service, Map.of(
                "dictTranslator", dictTranslator,
                "enumTranslator", new EnumTranslator()));

        TranslatedItem item = new TranslatedItem();
        service.translate(item);

        assertEquals("停用,启用", item.statusNames);
        assertEquals("高", item.levelName);
    }

    @Test
    void resolvesSystemTranslatorByExplicitTypeName() throws Exception {
        Field transMap = TranslationService.class.getDeclaredField("transMap");
        transMap.setAccessible(true);
        transMap.set(service, Map.<String, TranslationInterface>of(
                "userNameTranslator", (value, trans) -> "user-" + value));
        SystemItem item = new SystemItem(7L);

        service.translate(new PageResult<>(List.of(item), 1));

        assertEquals("user-7", item.userName);
    }

    @Test
    void batchesUniqueKeysAcrossPageRows() throws Exception {
        BatchingTranslator translator = new BatchingTranslator();
        Field transMap = TranslationService.class.getDeclaredField("transMap");
        transMap.setAccessible(true);
        transMap.set(service, Map.<String, TranslationInterface>of("userNameTranslator", translator));
        SystemItem first = new SystemItem(7L);
        AlternateSystemItem second = new AlternateSystemItem(8L);
        SystemItem third = new SystemItem(7L);
        List<Object> items = List.of(first, second, third);

        service.translate(new PageResult<>(items, items.size()));

        assertEquals(1, translator.batchCalls);
        assertEquals(0, translator.singleCalls);
        assertEquals(List.of(7L, 8L), translator.values);
        assertEquals(List.of("user-7", "user-8", "user-7"),
                List.of(first.userName, second.userName, third.userName));
    }

    @Test
    void fallsBackToSingleTranslationAndDefaultValueWhenBatchFails() throws Exception {
        FailingBatchTranslator translator = new FailingBatchTranslator();
        Field transMap = TranslationService.class.getDeclaredField("transMap");
        transMap.setAccessible(true);
        transMap.set(service, Map.<String, TranslationInterface>of("userNameTranslator", translator));
        List<FallbackItem> items = List.of(new FallbackItem(7L), new FallbackItem(8L));

        service.translate(items);

        assertEquals(1, translator.batchCalls);
        assertEquals(2, translator.singleCalls);
        assertEquals("user-7", items.get(0).userName);
        assertEquals("未知", items.get(1).userName);
    }

    private static final class Container {
        private List<Item> items;
        private Map<String, Item> index;
        private Item[] array;
        private Container self;
    }

    private static final class Item {
        private final String status;

        @Trans(field = "status", value = "0=停用,1=启用", defaultValue = "未知")
        private String statusName;

        private Item(String status) {
            this.status = status;
        }
    }

    private static final class TranslatedItem {
        private final String statuses = "0,1";
        private final String level = "1";

        @Trans(type = TransType.DICT, field = "statuses", value = "status")
        private String statusNames;

        @Trans(type = TransType.ENUM, field = "level", enumClass = Level.class)
        private String levelName;
    }

    private static final class SystemItem {
        private final Long userId;

        @Trans(type = TransType.USER_NAME, field = "userId")
        private String userName;

        private SystemItem(Long userId) {
            this.userId = userId;
        }
    }

    private static final class BatchingTranslator implements TranslationInterface {
        private int singleCalls;
        private int batchCalls;
        private List<?> values;

        @Override
        public String translate(Object value, Trans trans) {
            singleCalls++;
            return "single-" + value;
        }

        @Override
        public List<String> translateBatch(List<?> values, Trans trans) {
            batchCalls++;
            this.values = List.copyOf(values);
            return values.stream().map(value -> "user-" + value).toList();
        }
    }

    private static final class AlternateSystemItem {
        private final Long creatorId;

        @Trans(type = TransType.USER_NAME, field = "creatorId")
        private String userName;

        private AlternateSystemItem(Long creatorId) {
            this.creatorId = creatorId;
        }
    }

    private static final class FallbackItem {
        private final Long userId;

        @Trans(type = TransType.USER_NAME, field = "userId", defaultValue = "未知")
        private String userName;

        private FallbackItem(Long userId) {
            this.userId = userId;
        }
    }

    private static final class FailingBatchTranslator implements TranslationInterface {
        private int singleCalls;
        private int batchCalls;

        @Override
        public String translate(Object value, Trans trans) {
            singleCalls++;
            if (Long.valueOf(8L).equals(value)) {
                throw new IllegalStateException("single failure");
            }
            return "user-" + value;
        }

        @Override
        public List<String> translateBatch(List<?> values, Trans trans) {
            batchCalls++;
            throw new IllegalStateException("batch failure");
        }
    }

    private enum Level implements TranslatableEnum<String> {
        LOW("0", "低"), HIGH("1", "高");

        private final String value;
        private final String label;

        Level(String value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    private static final class StubDictService implements DictService {
        @Override
        public String getDictLabel(String dictType, String dictValue, String separator) {
            return "0,1".equals(dictValue) ? "停用,启用" : "";
        }

        @Override
        public String getDictValue(String dictType, String dictLabel, String separator) {
            return "";
        }

        @Override
        public Map<String, String> getAllDictByDictType(String dictType) {
            return Map.of("0", "停用", "1", "启用");
        }
    }
}

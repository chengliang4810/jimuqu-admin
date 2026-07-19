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

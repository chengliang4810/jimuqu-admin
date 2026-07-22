package com.jimuqu.common.translation.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.xbatis.page.IPager;
import cn.xbatis.page.PagerField;
import com.jimuqu.common.core.domain.PageResult;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslationInterface;
import com.jimuqu.common.translation.enums.TransType;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.util.ReflectUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 翻译服务
 */
@Component
@Slf4j
public class TranslationService {

    @Inject
    private Map<String, TranslationInterface> transMap = new HashMap<>();

    /**
     * 翻译
     *
     * @param object
     */
    public void translate(Object object) {
        Map<TranslationGroup, List<TranslationTask>> groups = new LinkedHashMap<>();
        collect(object, Collections.newSetFromMap(new IdentityHashMap<>()), groups);
        groups.forEach(this::translateBatch);
    }

    private void collect(Object object, Set<Object> visited,
                         Map<TranslationGroup, List<TranslationTask>> groups) {
        if (ObjectUtil.isNull(object)) {
            return;
        }

        Class<?> objectType = object.getClass();
        if (!visited.add(object)) {
            return;
        }

        if (object instanceof Collection<?> collection) {
            for (Object item : collection) {
                collect(item, visited, groups);
            }
            return;
        }

        if (object instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                collect(value, visited, groups);
            }
            return;
        }

        if (object instanceof IPager<?> iPager) {
            for (Object value : iPager.get(PagerField.RESULTS)) {
                collect(value, visited, groups);
            }
            return;
        }

        if (object instanceof PageResult<?> pageResult) {
            collect(pageResult.getRows(), visited, groups);
            return;
        }

        if (objectType.isArray()) {
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                collect(Array.get(object, i), visited, groups);
            }
            return;
        }

        if (isSimpleType(objectType)) {
            return;
        }

        Field[] fields = ReflectUtil.getDeclaredFields(objectType);
        for (Field field : fields) {
            if (field.isAnnotationPresent(Trans.class)) {
                try {
                    field.setAccessible(true);
                    Trans trans = field.getAnnotation(Trans.class);
                    String sourceField = ObjectUtil.isEmpty(trans.field()) ? field.getName() : trans.field();
                    Object sourceValue = BeanUtil.getProperty(object, sourceField);
                    if (ObjectUtil.isNotNull(sourceValue)) {
                        TranslationInterface translator = transMap.get(trans.type().getTranslatorName());
                        if (translator == null) {
                            setTranslatedValue(object, field, trans, trans.defaultValue());
                        } else {
                            TranslationGroup group = TranslationGroup.of(translator, trans);
                            groups.computeIfAbsent(group, key -> new java.util.ArrayList<>())
                                    .add(new TranslationTask(object, field, sourceValue, trans));
                        }
                    }
                } catch (Exception e) {
                    log.warn("翻译字段失败, objectType: {}, field: {}", objectType.getName(), field.getName(), e);
                }
            }

            try {
                field.setAccessible(true);
                collect(field.get(object), visited, groups);
            } catch (Exception e) {
                log.warn("遍历待翻译字段失败, objectType: {}, field: {}", objectType.getName(), field.getName(), e);
            }
        }
    }

    private void translateBatch(TranslationGroup group, List<TranslationTask> tasks) {
        Trans trans = tasks.get(0).trans();
        try {
            List<Object> values = tasks.stream()
                    .map(TranslationTask::sourceValue)
                    .distinct()
                    .toList();
            List<String> translatedValues = group.translator().translateBatch(values, trans);
            if (translatedValues.size() != values.size()) {
                throw new IllegalStateException("批量翻译结果数量与输入不一致");
            }
            Map<Object, String> translations = new HashMap<>();
            for (int index = 0; index < values.size(); index++) {
                translations.put(values.get(index), translatedValues.get(index));
            }
            for (TranslationTask task : tasks) {
                setTranslatedValue(task.object(), task.field(), task.trans(),
                        translations.get(task.sourceValue()));
            }
        } catch (Exception e) {
            log.warn("批量翻译失败，回退单值翻译, type: {}", group.type(), e);
            translateOneByOne(group.translator(), tasks);
        }
    }

    private void translateOneByOne(TranslationInterface translator, List<TranslationTask> tasks) {
        for (TranslationTask task : tasks) {
            try {
                setTranslatedValue(task.object(), task.field(), task.trans(),
                        translator.translate(task.sourceValue(), task.trans()));
            } catch (Exception e) {
                log.warn("单值翻译失败，使用默认值, objectType: {}, field: {}",
                        task.object().getClass().getName(), task.field().getName(), e);
                setTranslatedValue(task.object(), task.field(), task.trans(), task.trans().defaultValue());
            }
        }
    }

    private void setTranslatedValue(Object object, Field field, Trans trans, String translatedValue) {
        try {
            if (ObjectUtil.isNotEmpty(translatedValue)) {
                field.set(object, translatedValue);
            } else if (ObjectUtil.isNotEmpty(trans.defaultValue())) {
                field.set(object, trans.defaultValue());
            }
        } catch (Exception e) {
            log.warn("设置翻译字段失败, objectType: {}, field: {}", object.getClass().getName(), field.getName(), e);
        }
    }

    /**
     * 判断字段类型是否需要递归处理
     * 跳过：基本类型、Java核心类、数组、枚举、注解、代理类等
     */
    private boolean isSimpleType(Class<?> type) {
        if (type.isPrimitive() || type.isEnum() || type.isAnnotation()) {
            return true;
        }
        String packageName = type.getPackageName();
        return packageName.startsWith("java.lang")
                || packageName.startsWith("java.time")
                || packageName.startsWith("java.math")
                || packageName.startsWith("java.net")
                || packageName.startsWith("java.nio")
                || packageName.equals("java.util")
                || packageName.startsWith("java.util.concurrent")
                || packageName.startsWith("javax.")
                || packageName.startsWith("jakarta.")
                || java.lang.reflect.Proxy.isProxyClass(type);
    }

    private record TranslationGroup(TranslationInterface translator, TransType type, String value,
                                    Class<?> enumClass, String defaultValue) {

        private static TranslationGroup of(TranslationInterface translator, Trans trans) {
            return new TranslationGroup(translator, trans.type(), trans.value(),
                    trans.enumClass(), trans.defaultValue());
        }
    }

    private record TranslationTask(Object object, Field field, Object sourceValue, Trans trans) {
    }
}

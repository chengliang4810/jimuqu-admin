package com.jimuqu.system.translation;

import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslationInterface;
import com.jimuqu.system.domain.vo.SysOssVo;
import com.jimuqu.system.service.SysFileService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** OSS ID 转可访问地址。 */
@Component(value = "ossUrlTranslator", typed = true)
@RequiredArgsConstructor
public class OssUrlTranslator implements TranslationInterface {

    private final SysFileService fileService;

    @Override
    public String translate(Object value, Trans trans) {
        if (value == null) {
            return trans.defaultValue();
        }
        String url = fileService.selectUrlByIds(String.valueOf(value));
        return url == null || url.isBlank() ? trans.defaultValue() : url;
    }

    @Override
    public List<String> translateBatch(List<?> values, Trans trans) {
        List<String> ids = values.stream()
                .flatMap(value -> splitIds(value).stream())
                .distinct()
                .toList();
        Map<String, String> urls = ids.isEmpty() ? Map.of() : fileService.queryOssByIds(ids).stream()
                .filter(oss -> oss.getOssId() != null && oss.getUrl() != null)
                .collect(Collectors.toMap(SysOssVo::getOssId, SysOssVo::getUrl,
                        (first, ignored) -> first));
        return values.stream().map(value -> {
            String translated = splitIds(value).stream()
                    .map(urls::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(","));
            return translated.isBlank() ? trans.defaultValue() : translated;
        }).toList();
    }

    private List<String> splitIds(Object value) {
        if (value == null) {
            return List.of();
        }
        return Arrays.stream(String.valueOf(value).split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();
    }
}

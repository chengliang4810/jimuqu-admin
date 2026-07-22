package com.jimuqu.system.translation;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslationInterface;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 用户 ID 转昵称。 */
@Component(value = "nicknameTranslator", typed = true)
@RequiredArgsConstructor
public class NicknameTranslator implements TranslationInterface {

    private final SysUserMapper userMapper;

    @Override
    public String translate(Object value, Trans trans) {
        if (value instanceof String text && text.contains(",")) {
            return translateBatch(List.of(value), trans).get(0);
        }
        Long userId = TranslationValueSupport.toLong(value);
        if (userId == null) {
            return trans.defaultValue();
        }
        SysUser user = userMapper.getById(userId);
        return user == null || user.getNickName() == null ? trans.defaultValue() : user.getNickName();
    }

    @Override
    public List<String> translateBatch(List<?> values, Trans trans) {
        List<Long> userIds = TranslationValueSupport.distinctLongs(values);
        Map<Long, String> names = userIds.isEmpty() ? Map.of() : QueryChain.of(userMapper)
                .select(SysUser::getId, SysUser::getNickName)
                .in(SysUser::getId, userIds)
                .list().stream()
                .filter(user -> user.getId() != null && user.getNickName() != null)
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickName,
                        (first, ignored) -> first));
        return TranslationValueSupport.resolveLongValues(values, names, trans.defaultValue());
    }
}

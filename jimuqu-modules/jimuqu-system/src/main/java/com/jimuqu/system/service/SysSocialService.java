package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.system.domain.SysSocial;
import com.jimuqu.system.domain.vo.SysSocialVo;
import com.jimuqu.system.mapper.SysSocialMapper;
import lombok.RequiredArgsConstructor;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.List;
import java.util.Objects;

/**
 * 社会化账号绑定服务。
 */
@Component
@RequiredArgsConstructor
public class SysSocialService {

    private final SysSocialMapper socialMapper;

    public List<SysSocialVo> queryListByUserId(Long userId) {
        return QueryChain.of(socialMapper)
                .eq(SysSocial::getUserId, userId)
                .orderBy(SysSocial::getCreateTime)
                .returnType(SysSocialVo.class)
                .list();
    }

    public List<SysSocialVo> selectByAuthId(String authId) {
        return QueryChain.of(socialMapper)
                .eq(SysSocial::getAuthId, authId)
                .returnType(SysSocialVo.class)
                .list();
    }

    @Transaction
    public void bind(Long userId, AuthUser authUser) {
        String authId = authUser.getSource() + authUser.getUuid();
        SysSocial boundAccount = QueryChain.of(socialMapper)
                .eq(SysSocial::getAuthId, authId)
                .get();
        if (boundAccount != null && !Objects.equals(boundAccount.getUserId(), userId)) {
            throw new ServiceException("此第三方账号已经被其他用户绑定");
        }

        SysSocial sameSource = QueryChain.of(socialMapper)
                .eq(SysSocial::getUserId, userId)
                .eq(SysSocial::getSource, authUser.getSource())
                .get();
        SysSocial social = toEntity(authUser, userId, authId);
        SysSocial current = boundAccount != null ? boundAccount : sameSource;
        if (current == null) {
            if (socialMapper.save(social) <= 0) {
                throw new ServiceException("第三方账号绑定失败");
            }
            return;
        }
        social.setId(current.getId());
        if (socialMapper.update(social) <= 0) {
            throw new ServiceException("第三方账号绑定更新失败");
        }
    }

    public boolean deleteByIdForUser(Long socialId, Long userId) {
        return socialMapper.delete(where -> where
                .eq(SysSocial::getId, socialId)
                .eq(SysSocial::getUserId, userId)) > 0;
    }

    private SysSocial toEntity(AuthUser authUser, Long userId, String authId) {
        SysSocial social = new SysSocial()
                .setUserId(userId)
                .setAuthId(authId)
                .setSource(authUser.getSource())
                .setOpenId(authUser.getUuid())
                .setUserName(authUser.getUsername())
                .setNickName(authUser.getNickname())
                .setEmail(authUser.getEmail())
                .setAvatar(authUser.getAvatar());
        AuthToken token = authUser.getToken();
        if (token != null) {
            social.setAccessToken(token.getAccessToken())
                    .setExpireIn(token.getExpireIn())
                    .setRefreshToken(token.getRefreshToken())
                    .setAccessCode(token.getAccessCode())
                    .setUnionId(token.getUnionId())
                    .setScope(token.getScope())
                    .setTokenType(token.getTokenType())
                    .setIdToken(token.getIdToken())
                    .setMacAlgorithm(token.getMacAlgorithm())
                    .setMacKey(token.getMacKey())
                    .setCode(token.getCode())
                    .setOauthToken(token.getOauthToken())
                    .setOauthTokenSecret(token.getOauthTokenSecret());
        }
        return social;
    }
}

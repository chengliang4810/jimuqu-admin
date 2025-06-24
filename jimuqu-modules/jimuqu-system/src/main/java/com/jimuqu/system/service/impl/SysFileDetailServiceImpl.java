package com.jimuqu.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysFileDetail;
import com.jimuqu.system.domain.SysFilePartDetail;
import com.jimuqu.system.domain.query.SysFileDetailQuery;
import com.jimuqu.system.domain.vo.SysFileDetailVo;
import com.jimuqu.system.mapper.SysFileDetailMapper;
import com.jimuqu.system.mapper.SysFilePartDetailMapper;
import com.jimuqu.system.service.SysFileDetailService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 文件记录Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-06-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysFileDetailServiceImpl implements SysFileDetailService, FileRecorder {

    private final SysFileDetailMapper sysFileDetailMapper;
    private final SysFilePartDetailMapper sysFilePartDetailMapper;

    /**
     * 查询文件记录
     */
    @Override
    public SysFileDetailVo queryById(String id) {
        return sysFileDetailMapper.getVoById(id);
    }

    /**
     * 查询文件记录分页列表
     */
    @Override
    public Page<SysFileDetailVo> queryPageList(SysFileDetailQuery query, PageQuery pageQuery) {
        return buildQueryChain(query)
                .returnType(SysFileDetailVo.class)
                .paging(pageQuery.build());
    }

    /**
     * 查询文件记录列表
     */
    @Override
    public List<SysFileDetailVo> queryList(SysFileDetailQuery query) {
        QueryChain<SysFileDetail> queryChain = buildQueryChain(query);
        return queryChain.returnType(SysFileDetailVo.class).list();
    }

    /**
     * 构建查询条件
     *
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysFileDetail> buildQueryChain(SysFileDetailQuery query) {
        return QueryChain.of(sysFileDetailMapper)
                .forSearch(true)
                .where(query);
    }

    /**
     * 批量删除文件记录
     */
    @Override
    public Integer deleteByIds(Collection<String> ids) {
        return sysFileDetailMapper.deleteByIds(ids);
    }


    /**
     * 保存文件记录
     *
     * @param fileInfo 文件信息
     */
    @Override
    @SneakyThrows
    public boolean save(FileInfo fileInfo) {
        SysFileDetail detail = toSysFileDetail(fileInfo);
        boolean b = sysFileDetailMapper.save(detail) > 0;
        if (b) {
            fileInfo.setId(detail.getId());
        }
        return b;
    }

    /**
     * 更新文件记录，可以根据文件 ID 或 URL 来更新文件记录，
     * 主要用在手动分片上传文件-完成上传，作用是更新文件信息
     *
     * @param fileInfo 文件信息
     */
    @Override
    @SneakyThrows
    public void update(FileInfo fileInfo) {
        SysFileDetail detail = toSysFileDetail(fileInfo);
        sysFileDetailMapper.update(detail, where -> where
                .eq(detail.getUrl() != null, SysFileDetail::getUrl, detail.getUrl())
                .eq(detail.getId() != null, SysFileDetail::getId, detail.getId())
        );
    }

    /**
     * 根据 url 获取文件记录
     *
     * @param url 文件 url
     */
    @Override
    @SneakyThrows
    public FileInfo getByUrl(String url) {
        SysFileDetail sysFileDetail = sysFileDetailMapper.get(where -> where.eq(SysFileDetail::getUrl, url));
        return toFileInfo(sysFileDetail);
    }

    /**
     * 根据 url 删除文件记录
     *
     * @param url 文件 url
     */
    @Override
    public boolean delete(String url) {
        return sysFileDetailMapper.delete(where -> where.eq(SysFileDetail::getUrl, url)) > 0;
    }

    /**
     * 保存文件分片信息
     *
     * @param filePartInfo 文件分片信息
     */
    @Override
    @SneakyThrows
    public void saveFilePart(FilePartInfo filePartInfo) {
        SysFilePartDetail detail = toFilePartDetail(filePartInfo);
        int save = sysFilePartDetailMapper.save(detail);
        if (save > 0){
            filePartInfo.setId(detail.getId());
        }
    }

    /**
     * 删除文件分片信息
     *
     * @param uploadId 上传ID
     */
    @Override
    public void deleteFilePartByUploadId(String uploadId) {
        sysFilePartDetailMapper.delete(where -> where.eq(SysFilePartDetail::getUploadId, uploadId));
    }


    /**
     * 将 FilePartInfo 转成 FilePartDetail
     * @param info 文件分片信息
     */
    public SysFilePartDetail toFilePartDetail(FilePartInfo info) throws JsonProcessingException {
        SysFilePartDetail detail = new SysFilePartDetail();
        detail.setPlatform(info.getPlatform());
        detail.setUploadId(info.getUploadId());
        detail.setETag(info.getETag());
        detail.setPartNumber(info.getPartNumber());
        detail.setPartSize(info.getPartSize());
        detail.setHashInfo(valueToJson(info.getHashInfo()));
        return detail;
    }

    /**
     * 将 FileInfo 转为 SysFileDetail
     */
    public SysFileDetail toSysFileDetail(FileInfo info) throws JsonProcessingException {
        SysFileDetail detail = BeanUtil.copyProperties(
                info, SysFileDetail.class, "metadata", "userMetadata", "thMetadata", "thUserMetadata", "attr", "hashInfo");

        // 这里手动获 元数据 并转成 json 字符串，方便存储在数据库中
        detail.setMetadata(valueToJson(info.getMetadata()));
        detail.setUserMetadata(valueToJson(info.getUserMetadata()));
        detail.setThMetadata(valueToJson(info.getThMetadata()));
        detail.setThUserMetadata(valueToJson(info.getThUserMetadata()));
        // 这里手动获 取附加属性字典 并转成 json 字符串，方便存储在数据库中
        detail.setAttr(valueToJson(info.getAttr()));
        // 这里手动获 哈希信息 并转成 json 字符串，方便存储在数据库中
        detail.setHashInfo(valueToJson(info.getHashInfo()));
        return detail;
    }

    /**
     * 将 SysFileDetail 转为 FileInfo
     */
    public FileInfo toFileInfo(SysFileDetail detail) throws JsonProcessingException {
        FileInfo info = BeanUtil.copyProperties(
                detail, FileInfo.class, "metadata", "userMetadata", "thMetadata", "thUserMetadata", "attr", "hashInfo");

        // 这里手动获取数据库中的 json 字符串 并转成 元数据，方便使用
        info.setMetadata(jsonToMetadata(detail.getMetadata()));
        info.setUserMetadata(jsonToMetadata(detail.getUserMetadata()));
        info.setThMetadata(jsonToMetadata(detail.getThMetadata()));
        info.setThUserMetadata(jsonToMetadata(detail.getThUserMetadata()));
        // 这里手动获取数据库中的 json 字符串 并转成 附加属性字典，方便使用
        info.setAttr(jsonToDict(detail.getAttr()));
        // 这里手动获取数据库中的 json 字符串 并转成 哈希信息，方便使用
        info.setHashInfo(jsonToHashInfo(detail.getHashInfo()));
        return info;
    }

    /**
     * 将指定值转换成 json 字符串
     */
    public String valueToJson(Object value) throws JsonProcessingException {
        if (value == null) {
            return null;
        }
        return JsonUtil.toJsonString( value);
    }

    /**
     * 将 json 字符串转换成元数据对象
     */
    public Map<String, String> jsonToMetadata(String json) throws JsonProcessingException {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JsonUtil.parseObject(json, new TypeReference<Map<String, String>>() {});
    }

    /**
     * 将 json 字符串转换成字典对象
     */
    public Dict jsonToDict(String json) throws JsonProcessingException {
        if (StrUtil.isBlank(json)) {
            return null;
        };
        return JsonUtil.parseObject(json, cn.hutool.core.lang.Dict.class);
    }

    /**
     * 将 json 字符串转换成哈希信息对象
     */
    public HashInfo jsonToHashInfo(String json) throws JsonProcessingException {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JsonUtil.parseObject(json, HashInfo.class);
    }
}

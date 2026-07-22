package com.jimuqu.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.file.FileUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysFile;
import com.jimuqu.system.domain.SysFilePart;
import com.jimuqu.system.domain.SysOssConfig;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.query.SysFileQuery;
import com.jimuqu.system.domain.vo.SysFileVo;
import com.jimuqu.system.domain.vo.SysOssVo;
import com.jimuqu.system.mapper.SysFileMapper;
import com.jimuqu.system.mapper.SysFilePartMapper;
import com.jimuqu.system.mapper.SysOssConfigMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import com.jimuqu.system.service.SysFileService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.noear.solon.annotation.Component;
import org.noear.solon.Solon;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.core.handle.Context;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 文件记录Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-06-24
 */
@Slf4j
@Component(typed = true)
@RequiredArgsConstructor
public class SysFileServiceImpl implements SysFileService, FileRecorder {

    private static final long PRIVATE_URL_VALIDITY_MILLIS = 120_000L;

    private final SysFileMapper sysFileMapper;
    private final SysFilePartMapper sysFilePartMapper;
    private final SysOssConfigMapper sysOssConfigMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 查询文件记录
     */
    @Override
    public SysFileVo queryById(String id) {
        return sysFileMapper.getVoById(id);
    }

    /**
     * 查询文件记录分页列表
     */
    @Override
    public Page<SysFileVo> queryPageList(SysFileQuery query, PageQuery pageQuery) {
        QueryChain<SysFile> chain = buildQueryChain(query);
        return pageQuery.applyOrder(chain).returnType(SysFileVo.class).paging(pageQuery.build());
    }

    /**
     * 查询文件记录列表
     */
    @Override
    public List<SysFileVo> queryList(SysFileQuery query) {
        QueryChain<SysFile> queryChain = buildQueryChain(query);
        return queryChain.returnType(SysFileVo.class).list();
    }

    /**
     * 构建查询条件
     *
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysFile> buildQueryChain(SysFileQuery query) {
        return QueryChain.of(sysFileMapper)
                .forSearch(true)
                .where(query)
                .orderBy(SysFile::getId);
    }

    /**
     * 批量删除文件记录
     */
    @Override
    public Integer deleteByIds(Collection<String> ids) {
        return sysFileMapper.deleteByIds(ids);
    }

    @Override
    public Page<SysOssVo> queryOssPageList(SysFileQuery query, PageQuery pageQuery) {
        Page<SysFileVo> page = queryPageList(query, pageQuery);
        Set<String> privatePlatforms = queryPrivatePlatforms(page.getRows());
        List<SysOssVo> rows = page.getRows().stream()
                .map(file -> toOssVo(file, privatePlatforms, false))
                .toList();
        fillCreatorNames(rows);
        return Page.of(rows, page.getTotal());
    }

    @Override
    public List<SysOssVo> queryOssByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> uniqueIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (uniqueIds.isEmpty()) {
            return List.of();
        }
        List<SysFileVo> files = QueryChain.of(sysFileMapper)
                .in(SysFile::getId, uniqueIds)
                .returnType(SysFileVo.class)
                .list();
        Set<String> privatePlatforms = queryPrivatePlatforms(files);
        Map<String, SysFileVo> filesById = files.stream()
                .collect(Collectors.toMap(SysFileVo::getId, file -> file));
        List<SysOssVo> rows = ids.stream()
                .map(filesById::get)
                .filter(Objects::nonNull)
                .map(file -> toOssVo(file, privatePlatforms, true))
                .toList();
        fillCreatorNames(rows);
        return rows;
    }

    @Override
    public String selectUrlByIds(String ids) {
        if (StrUtil.isBlank(ids)) {
            return "";
        }
        List<String> fileIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();
        return queryOssByIds(fileIds).stream()
                .map(SysOssVo::getUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }

    @Override
    public DownloadedFile download(String id) {
        SysFile file = sysFileMapper.getById(id);
        if (file == null) {
            throw new ServiceException("文件数据不存在!");
        }
        byte[] content = fileStorageService().download(toFileInfo(file)).bytes();
        String contentType = StrUtil.blankToDefault(file.getContentType(), "application/octet-stream");
        String encodedName = FileUtil.percentEncode(file.getOriginalFilename());
        Context.current().headerSet("Access-Control-Expose-Headers", "Content-Disposition,download-filename");
        Context.current().headerSet("download-filename", encodedName);
        return new DownloadedFile(contentType, content, file.getOriginalFilename()).asAttachment(true);
    }

    @Override
    public boolean deleteOssByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new ServiceException("文件ID不能为空");
        }
        List<String> requested = ids.stream().distinct().toList();
        List<SysFile> files = QueryChain.of(sysFileMapper)
                .in(SysFile::getId, requested)
                .list();
        if (files.size() != requested.size()) {
            throw new ServiceException("文件数据不存在!");
        }
        Map<String, SysFile> filesById = files.stream()
                .collect(Collectors.toMap(SysFile::getId, file -> file));
        boolean deleted = false;
        for (String id : requested) {
            deleted |= fileStorageService().delete(toFileInfo(filesById.get(id)));
        }
        return deleted;
    }

    FileStorageService fileStorageService() {
        return Solon.context().getBean(FileStorageService.class);
    }

    private SysOssVo toOssVo(SysFileVo file, Set<String> privatePlatforms, boolean allowUrlFallback) {
        return new SysOssVo()
                .setOssId(file.getId())
                .setFileName(file.getFilename())
                .setOriginalName(file.getOriginalFilename())
                .setFileSuffix(toBellFileSuffix(file.getExt()))
                .setUrl(allowUrlFallback
                        ? resolveAccessUrlLenient(file, privatePlatforms)
                        : resolveAccessUrl(file, privatePlatforms))
                .setExt1(file.getAttr())
                .setCreateTime(file.getCreateTime())
                .setCreateBy(file.getCreateBy())
                .setService(file.getPlatform());
    }

    static String toBellFileSuffix(String extension) {
        if (extension == null || extension.isBlank() || extension.startsWith(".")) {
            return extension;
        }
        return "." + extension;
    }

    private Set<String> queryPrivatePlatforms(List<SysFileVo> files) {
        List<String> platforms = files.stream()
                .map(SysFileVo::getPlatform)
                .filter(Objects::nonNull)
                .filter(platform -> !platform.isBlank())
                .distinct()
                .toList();
        if (platforms.isEmpty()) {
            return Set.of();
        }
        return QueryChain.of(sysOssConfigMapper)
                .select(SysOssConfig::getConfigKey)
                .in(SysOssConfig::getConfigKey, platforms)
                .eq(SysOssConfig::getAccessPolicy, "0")
                .list().stream()
                .map(SysOssConfig::getConfigKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    String resolveAccessUrl(SysFileVo file, Set<String> privatePlatforms) {
        if (!privatePlatforms.contains(file.getPlatform())) {
            return file.getUrl();
        }
        FileStorageService storageService = fileStorageService();
        if (!storageService.isSupportPresignedUrl(file.getPlatform())) {
            return file.getUrl();
        }
        SysFile entity = BeanUtil.copyProperties(file, SysFile.class);
        Date expiration = new Date(System.currentTimeMillis() + PRIVATE_URL_VALIDITY_MILLIS);
        return storageService.generatePresignedUrl(toFileInfo(entity), expiration);
    }

    String resolveAccessUrlLenient(SysFileVo file, Set<String> privatePlatforms) {
        try {
            return resolveAccessUrl(file, privatePlatforms);
        } catch (RuntimeException exception) {
            log.debug("生成 OSS 临时访问地址失败，回退数据库地址: {}", file.getId(), exception);
            return file.getUrl();
        }
    }

    private void fillCreatorNames(List<SysOssVo> files) {
        List<Long> userIds = files.stream().map(SysOssVo::getCreateBy).filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> names = QueryChain.of(sysUserMapper)
                .select(SysUser::getId, SysUser::getUserName)
                .in(SysUser::getId, userIds)
                .list().stream().collect(Collectors.toMap(SysUser::getId, SysUser::getUserName));
        files.forEach(file -> file.setCreateByName(names.get(file.getCreateBy())));
    }


    /**
     * 保存文件记录
     *
     * @param fileInfo 文件信息
     */
    @Override
    @SneakyThrows
    public boolean save(FileInfo fileInfo) {
        SysFile detail = toSysFileDetail(fileInfo);
        boolean b = sysFileMapper.save(detail) > 0;
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
        SysFile detail = toSysFileDetail(fileInfo);
        sysFileMapper.update(detail, where -> where
                .eq(detail.getUrl() != null, SysFile::getUrl, detail.getUrl())
                .eq(detail.getId() != null, SysFile::getId, detail.getId())
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
        SysFile sysFile = sysFileMapper.get(where -> where.eq(SysFile::getUrl, url));
        return toFileInfo(sysFile);
    }

    /**
     * 根据 url 删除文件记录
     *
     * @param url 文件 url
     */
    @Override
    public boolean delete(String url) {
        return sysFileMapper.delete(where -> where.eq(SysFile::getUrl, url)) > 0;
    }

    /**
     * 保存文件分片信息
     *
     * @param filePartInfo 文件分片信息
     */
    @Override
    @SneakyThrows
    public void saveFilePart(FilePartInfo filePartInfo) {
        SysFilePart detail = toFilePartDetail(filePartInfo);
        int save = sysFilePartMapper.save(detail);
        if (save > 0) {
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
        sysFilePartMapper.delete(where -> where.eq(SysFilePart::getUploadId, uploadId));
    }


    /**
     * 将 FilePartInfo 转成 FilePartDetail
     *
     * @param info 文件分片信息
     */
    public SysFilePart toFilePartDetail(FilePartInfo info) {
        SysFilePart detail = new SysFilePart();
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
    public SysFile toSysFileDetail(FileInfo info) {
        SysFile detail = BeanUtil.copyProperties(
                info, SysFile.class, "metadata", "userMetadata", "thMetadata", "thUserMetadata", "attr", "hashInfo");

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
    public FileInfo toFileInfo(SysFile detail) {
        if (detail == null) {
            return null;
        }
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
    public String valueToJson(Object value) {
        if (value == null) {
            return null;
        }
        return JsonUtil.toString(value);
    }

    /**
     * 将 json 字符串转换成元数据对象
     */
    public Map<String, String> jsonToMetadata(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JsonUtil.toObject(json, new TypeReference<Map<String, String>>() {
        });
    }

    /**
     * 将 json 字符串转换成字典对象
     */
    public Dict jsonToDict(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        ;
        return JsonUtil.toObject(json, cn.hutool.core.lang.Dict.class);
    }

    /**
     * 将 json 字符串转换成哈希信息对象
     */
    public HashInfo jsonToHashInfo(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JsonUtil.toObject(json, HashInfo.class);
    }
}

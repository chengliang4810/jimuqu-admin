package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.bo.SysPluginBo;
import com.jimuqu.system.domain.query.SysPluginQuery;
import com.jimuqu.system.domain.vo.SysPluginVo;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.core.handle.UploadedFile;

import java.util.Collection;
import java.util.List;

/**
 * 在线插件Service接口。
 *
 * @author jimuqu-admin
 * @since 2026-06-13
 */
public interface SysPluginService {

    /**
     * 根据主键查询插件。
     */
    SysPluginVo queryById(Long id);

    /**
     * 查询插件分页列表。
     */
    Page<SysPluginVo> queryPageList(SysPluginQuery query, PageQuery pageQuery);

    /**
     * 查询插件列表。
     */
    List<SysPluginVo> queryList(SysPluginQuery query);

    /**
     * 新增插件。
     */
    Boolean insertByBo(SysPluginBo bo);

    /**
     * 更新插件。
     */
    Boolean updateByBo(SysPluginBo bo);

    /**
     * 批量删除插件。
     */
    Integer deleteByIds(Collection<Long> ids);

    /**
     * 修改启停状态。
     */
    Boolean updateStatus(Long id, Integer status);

    /**
     * 扫描本地插件目录。
     */
    Integer scan();

    /**
     * 上传插件包。
     */
    Long upload(UploadedFile file);

    /**
     * 下载开发模板。
     */
    DownloadedFile downloadTemplate();
}

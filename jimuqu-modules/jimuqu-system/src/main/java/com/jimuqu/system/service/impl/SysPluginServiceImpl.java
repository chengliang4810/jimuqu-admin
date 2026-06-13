package com.jimuqu.system.service.impl;

import cn.hutool.v7.core.map.Dict;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysPlugin;
import com.jimuqu.system.domain.bo.SysPluginBo;
import com.jimuqu.system.domain.query.SysPluginQuery;
import com.jimuqu.system.domain.vo.SysPluginVo;
import com.jimuqu.system.mapper.SysPluginMapper;
import com.jimuqu.system.service.SysPluginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.core.handle.UploadedFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 在线插件Service业务层处理。
 *
 * @author jimuqu-admin
 * @since 2026-06-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysPluginServiceImpl implements SysPluginService {

    private static final int STATUS_ENABLED = 0;
    private static final int STATUS_DISABLED = 1;
    private static final Path PLUGIN_DIR = Paths.get("runtime", "plugins");
    private static final Path UPLOAD_DIR = PLUGIN_DIR.resolve("packages");

    private final SysPluginMapper sysPluginMapper;

    @Override
    public SysPluginVo queryById(Long id) {
        return sysPluginMapper.getVoById(id);
    }

    @Override
    public Page<SysPluginVo> queryPageList(SysPluginQuery query, PageQuery pageQuery) {
        return buildQueryChain(query)
                .returnType(SysPluginVo.class)
                .paging(pageQuery.build());
    }

    @Override
    public List<SysPluginVo> queryList(SysPluginQuery query) {
        return buildQueryChain(query)
                .returnType(SysPluginVo.class)
                .list();
    }

    @Override
    public Boolean insertByBo(SysPluginBo bo) {
        SysPlugin sysPlugin = MapstructUtil.convert(bo, SysPlugin.class);
        fillDefaultValue(sysPlugin);
        boolean flag = sysPluginMapper.save(sysPlugin) > 0;
        bo.setId(sysPlugin.getId());
        return flag;
    }

    @Override
    public Boolean updateByBo(SysPluginBo bo) {
        SysPlugin sysPlugin = MapstructUtil.convert(bo, SysPlugin.class);
        fillDefaultValue(sysPlugin);
        return sysPluginMapper.update(sysPlugin) > 0;
    }

    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        return sysPluginMapper.deleteByIds(ids);
    }

    @Override
    public Boolean updateStatus(Long id, Integer status) {
        Assert.isTrue(STATUS_ENABLED == status || STATUS_DISABLED == status, "插件状态不合法");
        SysPlugin sysPlugin = sysPluginMapper.getById(id);
        Assert.notNull(sysPlugin, "插件不存在");
        return sysPluginMapper.update(new SysPlugin().setId(id).setStatus(status)) > 0;
    }

    @Override
    public Integer scan() {
        try {
            Files.createDirectories(PLUGIN_DIR);
            int count = 0;
            try (Stream<Path> paths = Files.walk(PLUGIN_DIR, 3)) {
                List<Path> descriptors = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> "plugin.json".equalsIgnoreCase(path.getFileName().toString()))
                        .toList();
                for (Path descriptor : descriptors) {
                    upsertByDescriptor(descriptor);
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            throw new IllegalStateException("扫描插件目录失败", e);
        }
    }

    @Override
    public Long upload(UploadedFile file) {
        try {
            Assert.notNull(file, "插件包不能为空");
            Assert.isFalse(file.isEmpty(), "插件包不能为空");
            Files.createDirectories(UPLOAD_DIR);
            String fileName = sanitizeFileName(file.getName());
            Path target = UPLOAD_DIR.resolve(System.currentTimeMillis() + "-" + fileName);
            file.transferTo(target.toFile());
            SysPlugin sysPlugin = buildPackagePlugin(target);
            sysPluginMapper.save(sysPlugin);
            return sysPlugin.getId();
        } catch (IOException e) {
            throw new IllegalStateException("上传插件包失败", e);
        }
    }

    @Override
    public DownloadedFile downloadTemplate() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            writeEntry(zip, "plugin.json", templateManifest());
            writeEntry(zip, "README.md", templateReadme());
            writeEntry(zip, "src/main/java/com/example/plugin/DemoPlugin.java", templateJava());
            zip.finish();
            return new DownloadedFile("application/zip", outputStream.toByteArray(), "jimuqu-plugin-template.zip");
        } catch (IOException e) {
            throw new IllegalStateException("生成插件开发模板失败", e);
        }
    }

    private QueryChain<SysPlugin> buildQueryChain(SysPluginQuery query) {
        return QueryChain.of(sysPluginMapper)
                .forSearch(true)
                .where(query);
    }

    private void upsertByDescriptor(Path descriptor) throws IOException {
        String manifestJson = Files.readString(descriptor, StandardCharsets.UTF_8);
        Dict manifest = JsonUtil.toMap(manifestJson);
        String pluginKey = getManifestValue(manifest, "pluginKey", "key", "id", "name");
        Assert.notBlank(pluginKey, "插件描述文件缺少 pluginKey: {}", descriptor);
        SysPlugin sysPlugin = sysPluginMapper.get(where -> where.eq(SysPlugin::getPluginKey, pluginKey));
        if (sysPlugin == null) {
            sysPlugin = new SysPlugin().setPluginKey(pluginKey).setStatus(STATUS_DISABLED);
        }
        sysPlugin.setPluginName(StringUtil.defaultIfBlank(getManifestValue(manifest, "pluginName", "title", "name"), pluginKey))
                .setVersion(StringUtil.defaultIfBlank(getManifestValue(manifest, "version"), "0.0.1"))
                .setAuthor(getManifestValue(manifest, "author"))
                .setPluginType(StringUtil.defaultIfBlank(getManifestValue(manifest, "pluginType", "type"), "local"))
                .setEntryClass(getManifestValue(manifest, "entryClass", "mainClass", "main"))
                .setPackagePath(resolvePackagePath(descriptor))
                .setDescriptorPath(descriptor.toAbsolutePath().normalize().toString())
                .setDescription(getManifestValue(manifest, "description"))
                .setManifestJson(manifestJson);
        fillDefaultValue(sysPlugin);
        if (sysPlugin.getId() == null) {
            sysPluginMapper.save(sysPlugin);
        } else {
            sysPluginMapper.update(sysPlugin);
        }
    }

    private SysPlugin buildPackagePlugin(Path target) {
        String baseName = target.getFileName().toString();
        int dotIndex = baseName.lastIndexOf('.');
        String pluginKey = dotIndex > 0 ? baseName.substring(0, dotIndex) : baseName;
        return new SysPlugin()
                .setPluginKey(pluginKey)
                .setPluginName(pluginKey)
                .setVersion("0.0.1")
                .setPluginType("package")
                .setStatus(STATUS_DISABLED)
                .setPackagePath(target.toAbsolutePath().normalize().toString())
                .setDescription("通过在线插件管理上传的插件包");
    }

    private String resolvePackagePath(Path descriptor) throws IOException {
        Path parent = descriptor.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            return null;
        }
        try (Stream<Path> paths = Files.list(parent)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".jar") || name.endsWith(".zip");
                    })
                    .findFirst()
                    .map(path -> path.toAbsolutePath().normalize().toString())
                    .orElse(null);
        }
    }

    private String getManifestValue(Dict manifest, String... keys) {
        if (manifest == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = manifest.get(key);
            if (value != null && StringUtil.isNotBlank(value.toString())) {
                return value.toString();
            }
        }
        return null;
    }

    private void fillDefaultValue(SysPlugin sysPlugin) {
        if (sysPlugin.getStatus() == null) {
            sysPlugin.setStatus(STATUS_DISABLED);
        }
        if (StringUtil.isBlank(sysPlugin.getVersion())) {
            sysPlugin.setVersion("0.0.1");
        }
        if (StringUtil.isBlank(sysPlugin.getPluginType())) {
            sysPlugin.setPluginType("local");
        }
    }

    private String sanitizeFileName(String fileName) {
        return StringUtil.defaultIfBlank(fileName, "plugin-package").replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void writeEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String templateManifest() {
        return """
                {
                  "pluginKey": "demo-plugin",
                  "pluginName": "演示插件",
                  "version": "0.0.1",
                  "author": "jimuqu",
                  "pluginType": "local",
                  "entryClass": "com.example.plugin.DemoPlugin",
                  "description": "积木区 Admin 插件开发模板"
                }
                """;
    }

    private String templateReadme() {
        return """
                # Jimuqu Admin 插件模板

                1. 修改 `plugin.json` 中的插件编码、名称和入口类。
                2. 将插件业务代码放在 `src/main/java`。
                3. 打包后把插件包和 `plugin.json` 放到后端 `runtime/plugins/{pluginKey}` 目录。
                4. 在后台“在线插件”页面点击扫描，即可纳入管理。
                """;
    }

    private String templateJava() {
        return """
                package com.example.plugin;

                /**
                 * 演示插件入口。
                 */
                public class DemoPlugin {

                    public void start() {
                        // 在这里初始化插件能力。
                    }
                }
                """;
    }
}

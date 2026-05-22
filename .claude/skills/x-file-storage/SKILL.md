---
name: x-file-storage
description: Use when working with x-file-storage, FileStorageService, FileInfo, UploadPretreatment, FileRecorder, storage platform selection, file upload/delete flows, thumbnail generation, Solon file upload integration, or debugging path/platform/file-record issues in Java projects.
---

# x-file-storage 开发技能

你在辅助 x-file-storage（`org.dromara.x-file-storage`）相关开发、上传/删除、文件记录、平台配置、预处理和问题排查。本技能基于本地 Maven sources jar 与真实项目用法整理；若目标项目版本不同，先以目标项目版本为准。

本地源码来源：`~/.m2/repository/org/dromara/x-file-storage/`

## 首要原则

1. **先判定任务类型**：单文件上传、批量上传、删除、文件信息查询、预处理链、回调、记录器、平台切换、缩略图、Web 响应流、Solon 集成或安全排查。
2. **优先查证源码**：不确定的 `FileStorageService`、`UploadPretreatment`、`FileInfo`、回调和平台类行为，必须查本地 sources jar。
3. **不要把上传流程和业务记录混为一谈**：先理解存储库对象模型，再决定是否接入记录器或额外业务表。
4. **外部输入要谨慎**：不要信任用户传入的文件路径、平台名、原始文件名或 URL；上传前限制大小、类型和路径。
5. **资源与流**：Web 上传流、输入流、响应流要明确谁负责关闭；`UploadedFile` 这类外部流通常由框架或调用方管理，不要在示例里随手关闭。
6. **安全边界**：模板/路径/平台配置必须来自可信来源；图片处理和回调参数要防止注入与越权。

## 资料入口

按任务读取对应 reference，必要时再读源码：

- 源码结构与核心 API：`references/source-map.md`
- 上传、删除、文件信息与平台配置：`references/upload-delete-platform.md`
- 预处理、记录器、回调与扩展点：`references/pretreatment-recorder-callback.md`
- Solon 集成与常见安全排查：`references/solon-security.md`

## 工作流

### 上传

1. 确认输入来源：`UploadedFile`、`File`、`byte[]`、`InputStream`、`URL`、`URI`、`String`。
2. 通过 `fileStorageService.of(...)` 创建预处理对象，按需设置原始文件名、路径、平台、对象信息和属性。
3. 需要图片处理时再链式调用图片/缩略图相关方法。
4. 最后调用 `upload()`，拿到 `FileInfo`。

### 删除与查询

1. 确认删除依据：`FileInfo`、url、path、平台文件名或业务关联信息。
2. 使用源码确认对应删除/查询方法，不要臆造链式 API 名称。
3. 如果项目有记录器，注意删除时是否同步清理记录。

### 平台与扩展

1. 先确认使用哪个存储平台实现（本地、MinIO、OSS、S3 等）。
2. 需要自定义平台行为时优先查 `StoragePlatform`、`FileRecorder`、切面和配置类。
3. 只在需要时接入 Solon 插件和自动记录，不要额外封装一层重复抽象。

## 常用查证命令

```bash
# 解压 sources jar
rm -rf /tmp/x-file-storage-2.3.0-core-src /tmp/x-file-storage-2.3.0-solon-src
mkdir -p /tmp/x-file-storage-2.3.0-core-src /tmp/x-file-storage-2.3.0-solon-src
unzip -q ~/.m2/repository/org/dromara/x-file-storage/x-file-storage-core/2.3.0/x-file-storage-core-2.3.0-sources.jar -d /tmp/x-file-storage-2.3.0-core-src
unzip -q ~/.m2/repository/org/dromara/x-file-storage/x-file-storage-solon/2.3.0/x-file-storage-solon-2.3.0-sources.jar -d /tmp/x-file-storage-2.3.0-solon-src

# 核心 API
grep -R "class FileStorageService\|class FileInfo\|class UploadPretreatment\|interface FileRecorder\|class FileStorageProperties" /tmp/x-file-storage-2.3.0-core-src /tmp/x-file-storage-2.3.0-solon-src
```

## 输出规范

- 全程中文；Java 类名、方法名和注解保持原样。
- 涉及源码机制时引用 `path:line`。
- 不确定时说“源码中未确认”，不要套用别的文件上传库或业务项目写法。
- 代码示例要尽量短，突出上传、删除、查询与安全边界。

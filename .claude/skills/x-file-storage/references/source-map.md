# x-file-storage 源码地图

## 关键包

- `org.dromara.x.file.storage.core`
  - `FileStorageService`
  - `FileInfo`
  - `FileStorageProperties`
  - `UploadPretreatment`
  - `FileRecorder`
- `org.dromara.x.file.storage.core.platform`
  - 各平台存储实现
- `org.dromara.x.file.storage.spring` / `solon`
  - 框架集成

## 关键对象关系

- `FileStorageService` 负责入口与平台路由。
- `of(...)` 返回预处理对象，之后再补充路径、文件名、平台、属性。
- `upload()` 返回 `FileInfo`。
- `FileRecorder` 负责上传记录扩展。

## 排查重点

- 输入来源是否可信。
- 平台名是否配置正确。
- 原始文件名是否需要手动设置。
- 图片处理、缩略图、回调是否影响最终文件名和路径。
- 外部流是否被错误关闭。

## 参考源码

建议优先看：
- `FileStorageService.java`
- `UploadPretreatment.java`
- `FileInfo.java`
- `FileStorageProperties.java`
- `FileRecorder.java`

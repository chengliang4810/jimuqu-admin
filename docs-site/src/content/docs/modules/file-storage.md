---
title: 文件存储
description: 本地与 MinIO 文件上传能力
---

项目通过 x-file-storage 统一文件上传、下载与删除，依赖中包含本地、MinIO 与 Amazon S3 V2 运行时支持。业务代码应通过存储抽象操作文件，不要直接依赖本地路径。

## 管理接口

| 接口 | 权限 | 说明 |
| --- | --- | --- |
| `GET /resource/oss/list` | `system:oss:list` | 分页查询文件 |
| `GET /resource/oss/listByIds/{ids}` | `system:oss:query` | 按 ID 查询 |
| `POST /resource/oss/upload` | `system:oss:upload` | 上传文件 |
| `GET /resource/oss/download/{id}` | `system:oss:download` | 下载文件 |
| `DELETE /resource/oss/{ids}` | `system:oss:remove` | 删除文件及记录 |

存储平台通过 `/resource/oss/config` 管理，支持列表、新增、编辑、启停和删除。

## 默认本地存储

```yaml
dromara:
  x-file-storage:
    default-platform: default
    local-plus:
      - platform: default
        enable-storage: true
        enable-access: true
        domain: "${JIMU_OSS_DOMAIN:/file/}"
        storage-path: "${JIMU_OSS_PATH:./data/oss/}"
```

生产环境建议由 Nginx 等静态服务器提供文件访问，应用负责鉴权、元数据和上传流程。

## MinIO 与 S3

启用对象存储时配置独立平台标识、端点、Bucket、访问密钥和基础路径。密钥必须通过外部配置注入，并对 Bucket 使用最小权限策略。

生产环境需要限制文件大小、扩展名与 MIME 类型，并使用随机对象名。公开下载地址不应绕过业务鉴权；敏感文件建议使用授权下载或短期签名 URL。

完整代码示例见[文件上传示例](/reference/file-upload-examples/)。

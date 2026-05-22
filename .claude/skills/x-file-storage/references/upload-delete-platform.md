# x-file-storage 上传与平台

## 输入来源

常见输入：
- `UploadedFile`
- `File`
- `byte[]`
- `InputStream`
- `URL`
- `URI`
- `String`

## 典型流程

```java
FileInfo info = fileStorageService.of(file)
    .setOriginalFilename("a.jpg")
    .setPath("upload/")
    .upload();
```

## 常用设置

- `setOriginalFilename(...)`
- `setPath(...)`
- `setPlatform(...)`
- `setObjectId(...)`
- `setObjectType(...)`
- `putAttr(...)`

## 平台选择

- 平台名必须来自已配置的存储平台。
- 需要指定平台时，在预处理对象上设置平台后再上传。
- 多平台共存时，先确认默认平台和目标平台配置。

## 图片处理

- 图片处理和缩略图通常通过链式方法完成。
- 需要指定缩略图后缀或保存文件名时，优先在上传前设置。

## 删除与查询

- 删除/查询通常基于 `FileInfo` 或文件标识字段。
- 先查源码确认对应方法签名，不要把别的文件库 API 套进来。

---
name: hutool
description: 辅助开发、选择模块、调试和解释 Hutool Java 工具库。只要用户提到 Hutool、cn.hutool、hutool-all、hutool-core、StrUtil、ObjectUtil、Convert、BeanUtil、DateUtil、FileUtil、IoUtil、CollUtil、MapUtil、ReflectUtil、ReUtil、IdUtil、NumberUtil、JSONUtil、HttpRequest/HttpUtil、SecureUtil、JWTUtil、ExcelUtil、Setting、CacheUtil、CronUtil、CaptchaUtil，或需要根据 /Users/chengliang/code-repositories/hutool 源码确认 API、模块依赖、空值行为、编码/日期/Bean/JSON/HTTP/加密/Excel 用法，都必须使用此技能。此技能只记录通用 Hutool 能力，不写入某个业务项目的专属规范。
---

# Hutool 开发技能

你在辅助 Hutool Java 工具库相关开发、API 选择、源码机制解释和问题排查。Hutool 工具类数量很多，且部分 API 有空值、编码、类型转换和版本差异细节，回答和实现必须优先以本地源码为准。

默认源码目录：`/Users/chengliang/code-repositories/hutool`

## 首要原则

1. **先判定任务类型**：核心工具、字符串/集合/Map、类型转换、Bean 拷贝、日期时间、文件/IO、JSON、HTTP、加密/摘要/JWT、Excel/POI、Setting、缓存、定时任务、验证码、数据库、AOP、脚本或问题排查。
2. **优先查证本地源码**：不确定的方法名、参数顺序、空值行为、异常行为、模块归属和依赖，必须读源码或测试示例。
3. **按需引入模块**：能单独引入模块时不要默认要求 `hutool-all`；但快速原型或用户明确要全量时可用 `hutool-all`。
4. **不要臆造 API**：Hutool API 很多但命名相近，写代码前确认真实类和方法，尤其是 `BeanUtil`、`JSONUtil`、`HttpRequest`、`ExcelUtil`。
5. **注意安全边界**：涉及 HTTP、文件路径、加密、JWT、反射、脚本执行、Excel 导入时，提醒 SSRF、路径穿越、弱摘要、秘钥泄露、公式注入、反射越权等边界。
6. **保持通用性**：本技能只记录 Hutool 通用能力。业务项目专属工具封装、返回体、目录结构、权限规范放到对应项目技能中。

## 资料入口

按任务读取对应 reference，必要时再读源码文件：

- 仓库结构和模块选择：`references/source-map.md`
- core 常用工具：`references/core-utils.md`
- Convert、Bean、Map/集合：`references/convert-bean-collection.md`
- 日期、文件、IO：`references/date-file-io.md`
- JSON、HTTP：`references/json-http.md`
- Crypto、JWT、Excel、Setting 等扩展：`references/extension-modules.md`
- 排查与安全注意：`references/debugging-security.md`

如果任务很具体，直接 grep 源码优先于泛读 reference。

## 工作流

### 业务代码中使用 Hutool

1. 查当前项目已引入的 Hutool 版本和模块。
2. 若只需要某一能力，优先给单模块依赖；若项目已有 `hutool-all`，可直接使用。
3. 查对应工具类源码或测试，确认 API 与行为。
4. 给最小代码片段，并说明空值、异常、字符集、时区、资源关闭等关键点。
5. 涉及外部输入时补安全边界：路径、URL、JSON、Excel、加密秘钥等。

### 源码机制解释

1. 定位工具类和真实实现类，例如 `StrUtil` 继承 `CharSequenceUtil`，`Convert` 走 `ConverterRegistry`，`BeanUtil.copyProperties` 走 `BeanCopier/CopyOptions`。
2. 阅读方法定义和测试示例，不只看 README。
3. 输出：结论 → 源码路径 → 行为细节 → 使用建议/坑点。

### 问题排查

1. 收集 Hutool 版本、模块依赖、JDK 版本、输入样例、异常堆栈。
2. 对照源码确认：空值返回、默认字符集、日期解析格式、Bean 属性名/大小写、JSONConfig、HTTP 超时/编码、加密算法/Provider。
3. 给最小修复和验证代码。

## 常用查证命令

```bash
# 常用 core 工具
grep -R "class StrUtil\|class BeanUtil\|class Convert\|class DateUtil\|class FileUtil" /Users/chengliang/code-repositories/hutool/hutool-core/src/main/java

# JSON/HTTP/Crypto/POI/JWT
grep -R "class JSONUtil\|class HttpRequest\|class SecureUtil\|class ExcelUtil\|class JWTUtil" /Users/chengliang/code-repositories/hutool/*/src/main/java

# 测试示例
find /Users/chengliang/code-repositories/hutool -path '*/src/test/java/*' -type f | grep -E 'StrUtil|BeanUtil|DateUtil|JSONUtil|HttpRequest|SecureUtil|ExcelUtil|JWTUtil'
```

## 输出规范

- 全程中文；Java 类名、方法名、配置键保持原样。
- 涉及源码机制时引用 `path:line`。
- 不确定时说“源码中未确认”，不要凭 Hutool 旧版本或网上印象补方法。
- 代码示例尽量短，优先使用当前源码中存在的 API。
- 安全相关默认提醒：不要用 MD5/SHA1 存密码，不要拼接不可信路径或 URL，不要把秘钥硬编码到仓库。

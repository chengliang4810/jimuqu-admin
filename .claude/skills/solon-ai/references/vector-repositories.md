# 向量 Repository 选择

源码目录：`/Users/chengliang/code-repositories/solon-ai/solon-ai-rag-repositorys`

## 模块清单

- `solon-ai-repo-chroma` — Chroma。
- `solon-ai-repo-dashvector` — DashVector。
- `solon-ai-repo-dashvector-official` — 官方 DashVector SDK 适配。
- `solon-ai-repo-elasticsearch` — Elasticsearch。
- `solon-ai-repo-mariadb` — MariaDB 向量能力。
- `solon-ai-repo-milvus` — Milvus。
- `solon-ai-repo-mysql` — MySQL 向量能力。
- `solon-ai-repo-opensearch` — OpenSearch。
- `solon-ai-repo-pgvector` — PostgreSQL pgvector。
- `solon-ai-repo-qdrant` — Qdrant。
- `solon-ai-repo-redis` — Redis 向量检索。
- `solon-ai-repo-tcvectordb` — 腾讯云 VectorDB。
- `solon-ai-repo-vectorex` — VectoRex。
- `solon-ai-repo-weaviate` — Weaviate。

## Core Repository

- `InMemoryRepository`：位于 `solon-ai-core`，适合测试或小规模内存场景。
- `WebSearchRepository`：把 Web search 结果包装为 Repository。

## 具体示例：Qdrant

源码：`solon-ai-rag-repositorys/solon-ai-repo-qdrant/src/main/java/org/noear/solon/ai/rag/repository/QdrantRepository.java`

测试：`solon-ai-rag-repositorys/solon-ai-repo-qdrant/src/test/java/features/ai/repo/qdrant/QdrantRepositoryTest.java`

最小构造模式：

```java
QdrantClient client = new QdrantClient(
    QdrantGrpcClient.newBuilder("localhost", 6334, false).build());

QdrantRepository repository = QdrantRepository
    .builder(embeddingModel, client)
    .collectionName("solon_ai")
    .build();
```

源码确认：

- 默认 collection：`solon_ai`。
- `build()` 时会初始化 collection。
- collection 不存在时，会调用 `embeddingModel.dimensions()` 推断向量维度。
- `save(List<Document>)` 会按 `embeddingModel.batchSize()` 分批，并调用 `embeddingModel.embed(batch)` 写入 embedding。
- 支持 `QueryCondition.filterExpression(...)` 过滤表达式。

## 选择建议

- 本地开发/单测：`InMemoryRepository`。
- 已有数据库：MySQL、MariaDB、PgVector 可减少新基础设施。
- 专用向量库：Milvus、Qdrant、Weaviate、Chroma。
- 搜索生态：Elasticsearch、OpenSearch。
- 云服务：DashVector、TcVectorDB。
- Redis 已有基础设施：`solon-ai-repo-redis`。

## 使用前查证

每个 repository 的构造参数、建表/建集合、索引参数、维度配置不同。写具体代码前必须读对应模块 README、测试或源码，不要臆造统一构造器。

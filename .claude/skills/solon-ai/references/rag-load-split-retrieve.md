# RAG：加载、切分、向量化、检索

## 核心链路

核心源码：`solon-ai-core/src/main/java/org/noear/solon/ai/rag`

主要接口/类：

- `Document`
- `DocumentLoader`
- `DocumentSplitter`
- `Repository`
- `RepositoryLifecycle`
- `RepositoryStorable`
- `RepositoryTool`
- `InMemoryRepository`
- `WebSearchRepository`

README 典型流程：

```java
EmbeddingModel embeddingModel = EmbeddingModel.of(apiUrl)
    .apiKey(apiKey)
    .provider(provider)
    .model(model)
    .batchSize(10)
    .build();

InMemoryRepository repository = new InMemoryRepository(embeddingModel);
repository.insert(new PdfLoader(pdfUri).load());

List<Document> docs = repository.search(query);
ChatMessage message = ChatMessage.ofUserAugment(query, docs);
chatModel.prompt(message).call();
```

## Loaders

源码目录：`solon-ai-rag-loaders`

- `solon-ai-load-ddl`
- `solon-ai-load-excel`
- `solon-ai-load-html`
- `solon-ai-load-markdown`
- `solon-ai-load-pdf`
- `solon-ai-load-ppt`
- `solon-ai-load-word`

需要加载 PDF/Word/Excel 等格式时，必须引入对应 loader 模块，不要只依赖 core。

## Splitters

核心 splitter 在：`solon-ai-core/src/main/java/org/noear/solon/ai/rag/splitter`

- `TextSplitter`
- `TokenSizeTextSplitter`
- `RegexTextSplitter`
- `JsonSplitter`
- `SemanticSplitter`
- `SplitterPipeline`

## Reranking

`RerankingModel` 可在 repository 检索后对 `Document` 重排。需要 provider 支持对应 rerank API。

关键源码：`solon-ai-core/src/main/java/org/noear/solon/ai/reranking/RerankingModel.java`

- 构造时同样要求 `apiUrl` 与 `model`，但选择的是 `RerankingDialectManager`，不要与 ChatModel 方言混用。
- `rerank(query, documents)` 会把返回结果的 `relevance_score` 写回 `Document.score`，再按分数降序返回。
- 方言实现目前可在 `solon-ai-llm-dialects/*/*RerankingDialect.java` 中查证。

## QueryCondition 与过滤

关键源码：`solon-ai-core/src/main/java/org/noear/solon/ai/rag/util/QueryCondition.java`

- 默认 `limit=4`、`similarityThreshold=0.4`、`searchType=VECTOR`。
- `filterExpression(String)` 使用 Solon `SnEL.parse(...)`，表达式会用于查询结果二次过滤或 repository 能力适配。
- 可配置 `freshness(...)`、`limit(...)`、`similarityThreshold(...)`、`disableRefilter(...)`、`searchType(SearchType.HYBRID)`、`hybridSearchParams(...)`。
- 过滤表达式依赖 Document metadata 字段；写示例前先确认插入文档时 metadata key 是否存在。

## 坑点

- `EmbeddingModel.embed(List<Document>)` 会把 embedding 写回 Document；返回数量不一致会抛 `EmbeddingException`。
- 文档 loader、splitter、repository 是独立层，不要混在一个类里写死。
- 生产场景不要默认用 `InMemoryRepository`；它适合测试或小规模内存知识库。

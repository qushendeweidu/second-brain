# 智能剪藏 · 第二大脑 — 技术方案（jishu.md）

> 本文只讲"实现需要的技术手段"，不含需求分析。供评审后再迭代。
> 技术基线遵循团队踩坑规范：JDK 21 + Spring Boot 3.5.10，全量 `jakarta.*`。

---

## 0. 一句话与核心闭环

贴一个 URL 或随手记一句 → 后端异步抓正文 → AI 生成摘要/标签 → 切块向量化入库 → 以后用大白话语义检索 + RAG 综合回答。

核心闭环（也是全链路技术骨架）：

```
录入(URL/文本)
   → [抓取] 拉正文 + 提取标题/首图 + 编码归一
   → [AI 加工] 摘要 + 自动打标签 + 分类
   → [向量化] 分块 → Embedding → 存 ES 向量库
   → [检索] 语义(向量) + 关键词(BM25) 混合召回
   → [RAG 问答] top-k 拼 prompt → LLM 生成 + 标注来源
```

---

## 1. 技术栈总览（遵循踩坑规范）

| 层 | 选型 | 备注 |
|----|------|------|
| 语言/框架 | JDK 21 + Spring Boot 3.5.10 | 用虚拟线程、record、switch 模式匹配；全量 `jakarta.*` |
| 认证 | Sa-Token | 你熟；单用户也保留登录，便于以后多端 |
| ORM | MyBatis-Plus（`mybatis-plus-spring-boot3-starter` 3.5.16） | 分页必须用 `PaginationInnerInterceptor`，禁假分页 |
| 关系库 | MySQL 8（`com.mysql:mysql-connector-j`） | 剪藏原始记录、分类、用户 |
| 缓存/队列 | Redis | 抓取去重、异步任务状态、热点缓存、限流 |
| 向量/检索 | Elasticsearch 8 | dense_vector + kNN（语义）；BM25（关键词）；二期混合检索 |
| 时序库 | InfluxDB 2.7 OSS | 剪藏行为/标签趋势/阅读时长；**三期增强，非核心** |
| AI | Spring AI 1.0.x | Embedding、Chat、Tool(Function Calling)、VectorStore 抽象 |
| 抓取 | RestClient + readability4j + Jsoup | 详见 §5.1 |
| API 文档 | Knife4j jakarta 4.5.0 | 禁 Springfox |
| 前端 | Vue3 + ElementPlus + ECharts（H5 响应式） | AI 生成为主，你负责联调 |

---

## 2. 分层架构

```
┌──────────────── Vue3 H5（剪藏列表 / 语义搜索 / 详情 / 统计图表）
│  REST + Sa-Token
├──────────────── Spring Boot 单体
│  ├── clip     模块：剪藏 CRUD、分类、任务状态查询
│  ├── capture  模块：URL 抓取 + 正文提取（§5.1）
│  ├── ai       模块：摘要、打标签、Embedding、RAG 问答、Tool（§5.2/5.3）
│  ├── search   模块：向量检索 + 关键词检索（§5.3）
│  └── stat     模块：InfluxDB 时序统计 + AI 周报（§5.5，三期）
├──────────────── 异步层：虚拟线程执行抓取+AI 加工（§5.4）
└──────────────── 存储：MySQL / Redis / Elasticsearch / InfluxDB
```

---

## 3. 数据存储职责划分

- **MySQL** — 事实数据（唯一真相源）
  - `clip`：id、user_id、title、source_url、raw_text（正文）、summary、status（PENDING/DONE/FAILED）、created_at
  - `tag` / `clip_tag`：标签及关联
  - `category`：分类树
  - `sys_user`：用户
- **Redis**
  - URL 去重：`SET clip:url:{md5(url)}`（存在即重复，不重复抓）
  - 异步任务状态：`HASH task:{taskId}` → {status, clipId, error}
  - 热点缓存 / 抓取频率限流（令牌桶或简单计数）
- **Elasticsearch** — 检索层（不是真相源，可从 MySQL 重建）
  - 每个 chunk 一条 doc：clipId、chunkText、embedding(dense_vector)、tags、createdAt
- **InfluxDB** — 时序度量（三期）
  - measurement `clip_event`：tag=category/source，field=count；measurement `reading`：field=duration_sec

> 设计原则：MySQL 存"是什么"，ES 存"怎么找到"，InfluxDB 存"随时间怎么变"。三者可从 MySQL 全量重建，避免多写不一致的深坑。

---

## 5. 核心技术专题

### 5.1 网页正文抓取（你的新领域 · 服务端主动抓）

这一步的难点不是"下载 HTML"，而是**从一堆导航/广告/评论里抽出正文**。

**技术组合（推荐）：`RestClient` 拉 HTML → `readability4j` 抽正文 → `Jsoup` 补标题/首图/清洗**

- **HTTP 客户端**：Spring Boot 3.2+ 内置 `RestClient`（同步、简单）。要点：设 `User-Agent`（伪装成浏览器，否则很多站直接 403）、连接/读取超时、失败重试。
- **正文提取**：不要自己写"猜正文"的规则，质量差且脆。用 **readability4j**（Mozilla Readability 的 Java 移植，坐标 `net.dankito.readability4j:readability4j`），它按可读性算法自动抽正文段落。
- **辅助清洗**：`Jsoup` 解析 DOM 取 `og:title`、首图 `og:image`、去脚本/样式残留。
- **编码归一**：按 `Content-Type` → `<meta charset>` → 探测 三级判定编码，统一转 UTF-8，**防中文乱码**（踩坑规范红线）。

```java
// 简化示例：拉 HTML → 抽正文
String html = restClient.get().uri(url)
        .header("User-Agent", "Mozilla/5.0 ...")
        .retrieve().body(String.class);

Readability4J r4j = new Readability4J(url, html);
Article article = r4j.parse();
String title = article.getTitle();
String content = article.getTextContent();   // 纯正文文本
```

**必须提前认清的范围红线：**

- **JS 动态渲染页面（掘金/知乎/公众号等）静态抓不到正文**——它们的正文是 JS 跑出来的，`RestClient` 只拿到空壳。
  - **MVP 决策：不引无头浏览器**。抓不到就把 `status=FAILED`，降级为"存链接 + 让你手输一句摘要"。这条决策能省掉一半复杂度，先别碰。
  - 二期若确实高频需要，再引 **Playwright for Java**（比 Selenium 现代）跑真实渲染。
- **反爬礼仪**：尊重 `robots.txt`、加请求间隔、限流，别把自己 IP 干封。
- **抓取要异步**（见 §5.4），别让用户贴完 URL 干等十几秒。

---

### 5.2 给智能体写抓取 Tool（Function Calling · 你的新领域）

注意区分两件事，你把它们说成一件了：

- **§5.1 是"后端主动抓"**：用户贴 URL，我的代码直接去抓。这是固定流程，不需要 LLM 参与。
- **本节是"让 LLM 自己决定去抓"**：在对话里，模型判断需要网页内容时，**主动调用你注册的一个函数**。这就是 Function Calling / Tool，也是 Agent 的核心机制。

**Spring AI 里怎么写一个 Tool：** 把一个普通方法标注成工具，交给 ChatClient，模型会在需要时生成一次"调用请求"，Spring AI 帮你执行并把结果回填给模型。

```java
@Component
public class WebTools {

    private final CaptureService capture;

    @Tool(description = "抓取给定网页链接的正文文本，用于总结或存档")
    public String fetchWebContent(
            @ToolParam(description = "要抓取的完整 http/https 链接") String url) {
        String content = capture.fetchArticleText(url);   // 复用 §5.1
        // 关键：正文可能几千字，超模型上下文。先截断/预摘要再返回，防 token 爆炸
        return content.length() > 4000 ? content.substring(0, 4000) : content;
    }
}
```

```java
// 把 Tool 挂到对话上；模型可自主决定何时调用
String answer = chatClient.prompt()
        .user("帮我看看 https://xxx 这篇讲了啥，顺便存下来")
        .tools(webTools, clipTools)   // clipTools 里有 saveClip、searchClips
        .call().content();
```

**要点/坑：**

- Tool 的 `description` 和参数 `description` 就是模型的"说明书"，写清楚模型才知道何时调、传什么。
- **返回内容要控长**：网页正文动辄几千字，直接回给模型会撑爆上下文，先截断或先跑一次小摘要再喂。
- 多个 Tool 组合就是 Agent：`fetchWebContent`（抓）+ `saveClip`（存）+ `searchClips`（查）+ `tagClip`（打标签），模型能自己编排"抓→总结→存到某分类"。
- 版本注意：`@Tool` 注解是 Spring AI 较新版本的写法（早期是 `FunctionCallback`）。落地时锁定 Spring AI 具体版本，按对应 API 写。

---

### 5.3 RAG 链路（你的强项，简要）

- **Embedding 模型**：优先本地 **Ollama + bge-m3**（中文友好、免费、你本机有 Ollama）；备选 OpenAI `text-embedding-3-small`。走 Spring AI `EmbeddingModel` 抽象，切换只改配置。
- **分块**：长正文先切块再向量化。用 Spring AI `TokenTextSplitter`，chunk ≈ 500 token、overlap ≈ 100，避免语义被切断。
- **向量库**：用 Spring AI `ElasticsearchVectorStore` 抽象，省去手写 kNN query；底层 ES `dense_vector` + kNN。
- **检索**：
  - MVP：纯向量语义检索 top-k。
  - 二期：向量 + BM25 关键词 **混合检索**（RRF 融合），召回更稳。
- **RAG 问答**：检索 top-k chunk → 拼进 prompt（带"仅依据下列资料回答，并标注来源剪藏"）→ LLM 生成 → 返回答案 + 命中的 clipId 列表，前端可点回原剪藏。

---

### 5.4 异步 + 虚拟线程（工程亮点）

抓取 + AI 加工是 IO 密集、耗时几秒到十几秒，必须异步：

1. 用户提交 URL → 生成 `taskId`，Redis 记 `PENDING` → **立即返回 taskId**。
2. 虚拟线程池执行：抓取 → 摘要 → 打标签 → 分块 → Embedding → 存 ES → MySQL `status=DONE`，Redis 更新状态。
3. 前端轮询 `/task/{id}` 或用 SSE 推送完成。

```java
// JDK 21 虚拟线程：每任务一线程，IO 阻塞也不吃资源
var executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(() -> pipeline.process(taskId, url));
```

这块正好把你想练的 JDK 21 虚拟线程用上，也是简历里"高吞吐异步管线"的谈资。

---

### 5.5 InfluxDB 时序统计（半年没碰 · 复习向）

**先说定位（重要）**：时序库对本项目是**锦上添花的第三期增强**，不是核心。核心是抓取 + RAG。放这里是满足你"练回 InfluxDB"的诉求，但别让它拖累 MVP——前两期完全不碰它。

**概念快速找回（InfluxDB 2.x）：**

| 概念 | 类比关系库 | 说明 |
|------|-----------|------|
| Bucket | 数据库 | 数据容器，带保留策略 |
| Measurement | 表 | 如 `clip_event` |
| **Tag** | 索引列 | 被索引，用于**分组/过滤**（如 category、source） |
| **Field** | 普通列 | 存实际数值（如 count、duration），**不被索引** |
| Timestamp | — | 每条数据的时间戳，主排序键 |

> 最容易混：**能用来 filter/group by 的放 Tag，纯数值度量放 Field**。放反了查询会很别扭且性能差。

**写入（官方 `influxdb-client-java`）：**

```java
// 每次剪藏成功打一个点
Point p = Point.measurement("clip_event")
        .addTag("category", category)     // 维度：可分组过滤
        .addTag("source", host)           // 来源域名
        .addField("count", 1)             // 度量值
        .time(Instant.now(), WritePrecision.NS);
writeApi.writePoint(bucket, org, p);
```

**查询用 Flux（2.x 主力语言，管道式）：**

```flux
// 最近 30 天每天的剪藏数量
from(bucket: "second_brain")
  |> range(start: -30d)
  |> filter(fn: (r) => r._measurement == "clip_event" and r._field == "count")
  |> aggregateWindow(every: 1d, fn: sum)
```

**本项目用法：** 剪藏成功打点 → 前端 ECharts 画"知识增长曲线/标签趋势/来源分布" → AI 读聚合结果生成"本周你在 xx 主题上剪藏最多"的周报。

**环境：** Docker 一把起：

```yaml
influxdb:
  image: influxdb:2.7
  ports: ["8086:8086"]
  environment:
    DOCKER_INFLUXDB_INIT_MODE: setup
    DOCKER_INFLUXDB_INIT_USERNAME: admin
    DOCKER_INFLUXDB_INIT_PASSWORD: admin12345
    DOCKER_INFLUXDB_INIT_ORG: my-org
    DOCKER_INFLUXDB_INIT_BUCKET: second_brain
```

---

## 6. 关键接口清单（简要）

| 模块 | 接口 | 说明 |
|------|------|------|
| clip | `POST /api/clip/url` | 提交 URL，返回 taskId（异步） |
| clip | `POST /api/clip/text` | 提交纯文本剪藏 |
| clip | `GET /api/task/{id}` | 查异步任务状态 |
| clip | `GET /api/clip/page` | 剪藏分页列表（MyBatis-Plus 分页） |
| clip | `GET /api/clip/{id}` | 剪藏详情 |
| search | `GET /api/search?q=` | 语义检索 |
| ai | `POST /api/ai/ask` | RAG 问答（返回答案 + 来源） |
| ai | `POST /api/ai/chat` | 带 Tool 的对话式 Agent（可自主抓/存/查） |
| stat | `GET /api/stat/trend` | 时序统计（三期） |

---

## 7. 分阶段落地（呼应"备考优先·前松后紧"）

- **MVP（第一冲刺，约 7/24–8/2）**：URL/文本录入 → 抓取(静态) → AI 摘要+标签 → 向量化 → 纯语义检索 → 极简列表页。**能日用即止。**
- **二期（约 8/3–8/15）**：RAG 问答 + 来源标注、混合检索、对话式 Tool/Agent、H5 前端成形。
- **三期（考完再长）**：InfluxDB 时序统计 + ECharts 图表 + AI 周报、JS 渲染抓取(Playwright)、自动归类 Agent。

> 8/16–9/1 项目冻结，全力备考。

---

## 8. 风险与范围控制

- **JS 渲染页面抓不到** → MVP 不碰无头浏览器，失败降级手输摘要。
- **AI 调用成本** → Embedding 用本地 Ollama；Chat 走可配置模型，别锁死。
- **抓取被封/违规** → 尊重 robots、限流、只抓自己要存的、别批量爬。
- **多存储一致性** → MySQL 为唯一真相源，ES/InfluxDB 可重建，避免多写。
- **时序库别喧宾夺主** → 严格压到三期。

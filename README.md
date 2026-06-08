# AI 广告流

AI 广告流是一个 Android 原生广告信息流示例项目，使用 Java、AndroidX、Material Components、Navigation、ViewBinding、SQLite、Retrofit、OkHttp、Glide 和 Media3 实现。

应用围绕广告内容推荐与浏览体验展开，支持多频道广告流、标签筛选、下拉刷新、分页加载、广告详情、点赞、收藏、分享、曝光/点击埋点、图片与视频广告展示，以及基于大语言模型的广告摘要、标签补全和自然语言搜索。没有配置 LLM API Key 时，项目会自动使用本地降级逻辑，仍可正常运行和体验主要功能。

## 如何运行

### 环境要求

- Android Studio：建议使用支持 Android Gradle Plugin 9.2.1 的版本
- JDK：11 或以上
- Android SDK：
  - compileSdk：36.1
  - minSdk：24
  - targetSdk：36

### 打开项目

1. 使用 Android Studio 打开项目根目录。
2. 等待 Gradle Sync 完成。
3. 选择模拟器或真机。
4. 点击 Run 运行 `app`。

### 命令行构建

```bash
./gradlew :app:assembleDebug
```

构建成功后，Debug APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 可选：配置 AI 能力

项目从根目录的 `local.properties` 读取 LLM 配置。可以按需添加：

```properties
LLM_BASE_URL=https://api.openai.com/
LLM_API_KEY=你的 API Key
LLM_MODEL=gpt-4o-mini
```

说明：

- `LLM_BASE_URL` 默认为 `https://api.openai.com/`
- `LLM_MODEL` 默认为 `gpt-4o-mini`
- `LLM_API_KEY` 为空时不会请求远程模型，会使用本地 fallback 生成广告摘要和搜索结果
- `local.properties` 不应提交到版本库，避免泄露密钥

## 模块划分

当前项目只有一个 Gradle 模块：

```text
:app
```

主要代码位于 `app/src/main/java/com/example/myapplication`，内部按职责划分如下：

```text
ai/          LLM 接入、广告 AI 摘要生成、自然语言搜索和降级逻辑
data/        数据仓库，统一协调 UI、SQLite、本地种子数据、AI 元数据和埋点
data/local/  SQLiteOpenHelper、DAO、广告实体和埋点实体
data/mock/   本地广告种子数据
media/       视频播放器池和 Media3 播放相关逻辑
model/       广告、频道、埋点等业务模型
ui/common/   通用 UI 工具，例如图片加载
ui/feed/     信息流列表、频道/标签筛选、分页、曝光、点赞、收藏
ui/detail/   广告详情页
ui/search/   搜索弹窗和搜索结果列表
```

资源文件位于 `app/src/main/res`：

```text
layout/      Activity、Fragment、广告卡片和搜索弹窗布局
navigation/  Navigation 图
menu/        顶部菜单
drawable/    占位图和启动图资源
values/      字符串、颜色、主题等基础资源
```

## 开发规范

### 架构与职责

- UI 层只负责展示、用户交互和观察 `LiveData`，不要直接访问数据库或网络。
- `ViewModel` 负责保存页面状态，并将用户操作转发给 `Repository`。
- `AdRepository` 是广告数据的统一入口，负责刷新、分页、筛选、详情、埋点、AI 元数据补全和搜索。
- 数据库操作集中在 `data/local`，避免在 UI 层拼 SQL 或直接操作 `SQLiteDatabase`。
- AI 接入集中在 `ai` 包，新增模型、提示词或解析逻辑时优先在该包内扩展。

### 代码风格

- 项目当前主要使用 Java，新增业务代码优先保持 Java 风格一致。
- 命名应表达业务含义，类名使用 PascalCase，方法和变量使用 camelCase。
- 注释用于解释业务原因或复杂流程，避免重复描述代码本身。
- 新增资源名称使用清晰前缀，例如 `fragment_`、`item_`、`dialog_`、`ic_`。
- 新增字符串应放入 `res/values/strings.xml`，避免在布局或代码中硬编码可见文案。

### UI 与交互

- 优先复用现有 AppCompat、Material、Navigation 和 ViewBinding 方案。
- 广告列表相关 UI 放在 `ui/feed`，详情相关 UI 放在 `ui/detail`，搜索相关 UI 放在 `ui/search`。
- 图片加载优先通过 `AdImageLoader` 统一处理。
- 视频广告播放逻辑优先复用 `PlayerPool`，避免每个列表项重复创建播放器导致资源浪费。

### 数据与安全

- API Key 等敏感配置只放在 `local.properties` 或本地环境中，不写入源码。
- 远程 LLM 返回内容必须经过解析和 fallback 保护，不能假设模型一定返回合法 JSON。
- 新增埋点事件时，应同步维护 `TelemetryEvent`、`TelemetryEntity` 和相关写入逻辑。
- 修改数据库表结构时，需要更新 `AppDatabase.DATABASE_VERSION` 和升级逻辑。

### 提交前检查

建议在提交前至少执行：

```bash
./gradlew :app:assembleDebug
```

如涉及单元测试或 Android 测试，可继续执行：

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## AI 声明

本项目包含 AI 辅助功能，用于生成广告摘要、推荐理由、标签补全和自然语言搜索结果。AI 能力由 `ai/AiRepository.java` 统一封装，通过兼容 Chat Completions 风格的接口请求模型服务。

AI 功能遵循以下原则：

- 用户未配置 `LLM_API_KEY` 时，应用使用本地 fallback 逻辑，保证基础功能可用。
- 模型输出只作为广告内容展示和搜索排序的辅助信息，不应被视为事实性结论或商业承诺。
- 远程模型调用可能涉及广告标题、品牌、描述、标签和用户搜索文本，请在接入真实服务前确认隐私政策、数据处理方式和合规要求。
- 不应将 API Key、访问令牌或其他敏感信息提交到仓库。
- AI 生成内容需要保留人工审核和异常兜底机制，尤其是在面向真实用户、真实品牌或投放场景时。

如果后续接入生产环境模型服务，建议补充请求超时、重试策略、内容安全审核、日志脱敏、用户授权提示和服务端代理层，避免在客户端直接暴露密钥。

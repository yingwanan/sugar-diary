# AGENTS.md — Sugar Diary Developer Guide

## Project Overview

Sugar Diary（砂糖日记）是一个原生 Android 应用，使用 Kotlin、Jetpack Compose、Room、DataStore、OkHttp 和 kotlinx.serialization 构建。应用本地优先保存日记正文、图片和分析结果，并通过用户配置的 OpenAI 兼容接口提供格式转换、文风润色、多 Agent 心理分析、周期报告和心理对话。

## Repository Layout

- `app/src/main/java/com/localdiary/app/`
  - `data/`: repository、Room DAO/entity、文件存储、导入导出、LLM provider、设置持久化、图片嵌入服务。
  - `domain/`: 纯业务逻辑，例如浏览筛选、预览生成、心理 Agent、情绪中心投影、周期报告生成。
  - `model/`: 领域模型、编辑器块模型、Markdown/HTML 序列化、图片嵌入语法、AI 配置模型。
  - `ui/`: Compose UI、导航、design system、页面、ViewModel、全局 snackbar 消息。
- `app/src/test/java/`: JUnit4 单元测试，覆盖模型解析、筛选、报告、LLM stream parser、心理 Agent 逻辑和部分 UI token 规则。
- `app/src/main/icon/`: launcher foreground 的 base64 源文件；构建时生成 PNG。
- `app/src/main/res/`: Android 资源。
- `docs/`: 项目记录、计划或审查材料。

## Required Commands

从仓库根目录运行：

```bash
# 单元测试
./gradlew testDebugUnitTest

# 指定测试类
./gradlew testDebugUnitTest --tests "com.localdiary.app.model.EditorDocumentParserTest"

# Android Lint
./gradlew lintDebug

# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

在受限环境中，如果 Gradle daemon 因本地 socket 或资源限制不稳定，优先使用：

```bash
env GRADLE_USER_HOME=/root/strj/sugar-diary/.gradle GRADLE_OPTS=-Xmx1024m ./gradlew --no-daemon --max-workers=1 testDebugUnitTest assembleDebug
```

## Release Process

1. 更新 `app/build.gradle.kts` 中的 `versionCode` 和 `versionName`。
2. 确保根目录存在 `keystore.properties`，并指向 release keystore。
3. 运行测试：`./gradlew testDebugUnitTest`。
4. 打包：`./gradlew assembleRelease`。
5. 用 `apksigner verify --print-certs` 校验 APK 签名。
6. 上传到 GitHub Releases，并使用 `Sugar-Diary-v<version>-release.apk` 命名。
7. 合并发布分支到 `main` 后推送。

当前已知上一版 `v1.0.2` release APK 证书 SHA-256 为：

```text
d18346d58c2a48427259f7c6b93730180d0d55ee0fb6690166adadc2e6c3c897
```

发布 2.0.0 或后续版本时必须使用同一签名证书，否则 Android 无法覆盖安装旧版本。不要用 debug keystore 代替正式签名。

## Architecture Notes

### Dependency Injection

项目使用手写 DI：`DiaryApplication` 创建 `AppContainer`，`MainActivity` 将容器传给 `DiaryAppRoot`。ViewModel 通过小型 `ViewModelFactory` 在 composable 中创建。不要引入 Hilt/Koin，除非有明确迁移计划。

### Storage Model

应用使用双存储：

- Room 保存结构化元数据：文章元信息、情绪分析、周期报告、文风、版本快照、心理对话、Agent 运行过程、用户画像。
- `LocalEntryFileStore` 保存正文内容文件。`EntryEntity` 只保存正文文件路径，不保存大段正文。

新增持久化数据时先判断：可查询/可关联的数据进 Room；大文本或二进制内容优先放文件系统。

### Editor Model

编辑器不是直接操作 raw Markdown 字符串，而是操作 block model：

- `EditorDocumentBlock.Text`
- `EditorDocumentBlock.Image`
- `EditorDocumentParser`
- `EmbeddedImageParser`

任何影响图片插入、光标位置、格式转换或序列化的修改，必须同步考虑 model/parser、`EditorViewModel` 和 `EditorScreen`。

### AI and Psychology

- `OpenAiCompatibleLlmProvider` 负责 OpenAI 兼容接口和 SSE streaming。
- `AiSettingsRepository` 通过 DataStore 保存模型配置，API Key 加密保存。
- `PsychologyAgentOrchestrator` 调度多 Agent 心理分析。
- `MoodReportGenerator` 基于情绪历史生成周报/月报。
- 图片进入 LLM 前应先被摘要或替换成引用，不要把整段 base64 直接发送给主模型。

### UI Guidelines

- 复用 `ui/designsystem` 中的 token、atom、molecule、organism、template。
- Compose 列表应为动态数据提供稳定 `key`；长列表或多类型列表同时提供 `contentType`。
- 可点击自定义组件应设置合适语义，例如 `Role.Button` 或明确 `onClickLabel`。
- 不要把装饰性图标暴露给 TalkBack；操作性图标必须有 `contentDescription`。
- 耗时操作按钮必须根据 ViewModel 的 busy/working/sending 状态禁用，避免重复请求。
- 明暗色 token 修改后应考虑 WCAG 对比度；已有 `DiaryColorsContrastTest` 覆盖关键配色。

## Testing Expectations

- 新增 parser、domain、repository-adjacent 逻辑时优先写单元测试。
- UI 纯布局目前以人工检查为主，但可测试的 token/formatter/filter 逻辑应写 JUnit 测试。
- 修改 release、签名、版本号后至少运行：

```bash
./gradlew testDebugUnitTest assembleDebug
```

若环境具备 release keystore，还必须运行：

```bash
./gradlew assembleRelease
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

## Coding Style

- Kotlin 代码遵循现有 Compose 风格：小 composable、显式参数、状态上提到 ViewModel。
- 不要在 Composable 内创建昂贵对象，除非使用 `remember`。
- 不要在 RSC/Web/React 方式上套用本项目；这是 Android Compose 项目。
- 保持变更最小，避免顺手重构无关模块。
- 用户文案目前以中文为主，保持一致。

## Git and Review

- 默认工作分支为功能分支；不要在未确认的情况下强推或重置。
- 合并到 `main` 前必须测试通过。
- commit message 使用简短英文 Conventional Commits，例如：
  - `docs: refresh user and developer guides`
  - `fix: improve compose accessibility states`
  - `chore: release version 2.0.0`
- 仓库包含被 `.gitignore` 排除的签名材料、构建产物和本地配置；不要提交这些文件。

# Sugar Diary

## 事先声明：完全AI编程，有任何问题都去骂codex

Sugar Diary（砂糖日记）是一款原生 Android 本地日记应用，面向希望长期保留私人写作记录、同时又需要 AI 辅助整理与分析的用户。应用支持 Markdown 与 HTML 写作、插入设备本地图片、使用 OpenAI 兼容接口做审查与润色，并基于文章与图片生成情绪分析和周期报告。

- Repository: `https://github.com/yingwanan/sugar-diary`
- Release: `https://github.com/yingwanan/sugar-diary/releases/tag/v1.0.0`

## Core Features

- 本地优先：文章默认保存在设备本地，不依赖云端同步。
- 双格式写作：支持 Markdown 与 HTML 编辑、预览与导出。
- 图片内嵌：可按光标位置插入本地图片，并在编辑区与查看页展示缩略预览。
- AI 审查与润色：支持 OpenAI 兼容模型，可按指定格式输出候选稿。
- 情绪分析：结合正文与图片，生成情绪标签、强度、总结与健康建议。
- 周期报告：按日、周、月汇总情绪变化趋势。
- 文风系统：内置常用文风提示词，并支持自定义文风。
- 数据迁移：支持整库导入导出，也支持原始文章文件夹迁移。

## Privacy

- 日记正文与图片默认仅存储在本地。
- API Key 使用加密存储。
- 应用备份已关闭，避免系统备份直接带走本地隐私数据。
- 若接入第三方 LLM，只有你主动触发的内容才会发送到已配置接口。

## Project Structure

- `app/src/main/java/com/localdiary/app/data`: 数据存储、导入导出、LLM 接口与图片处理。
- `app/src/main/java/com/localdiary/app/model`: 领域模型与编辑器解析逻辑。
- `app/src/main/java/com/localdiary/app/ui`: Compose 界面、页面与 ViewModel。
- `app/src/test`: 解析、格式、筛选与报告生成相关单元测试。
- `app/src/main/icon`: 文本化图标源，构建时自动还原为启动图标。

## Build

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

发布签名依赖本地 `keystore.properties`，该文件与签名材料默认不进入版本控制。

## Release Notes

- 应用显示名：`砂糖日记`
- 英文名：`Sugar Diary`
- 发布包类型：Android APK
- 已支持自定义图标、发布签名与本地正式构建
- 当前公开发行版本：`v1.0.0`

## License

本项目采用 MIT License 开源。

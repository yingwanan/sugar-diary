# Sugar Diary

Sugar Diary（砂糖日记）是一个原生 Android 本地日记应用。文章支持 Markdown 与 HTML，本地保存，不依赖云端；可接入 OpenAI 兼容 LLM，用于审查、润色、情绪分析、周期报告与图片理解辅助分析。

## Features

- Local-first writing with Markdown and HTML
- Inline local image embedding and preview
- OpenAI-compatible text and vision model integration
- Mood analysis, trend reports, and style presets
- Import and export for device-to-device migration

## Build

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

Release signing uses a local `keystore.properties` file and is intentionally excluded from version control.

## License

This project is open-sourced under the MIT License.

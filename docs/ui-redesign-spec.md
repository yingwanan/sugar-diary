# Sugar Diary UI 重构设计规格

## 1. 设计原则

- **书写优先**：编辑器是第一公民，视觉噪音最小化，让用户沉浸在写作中。
- **材质层次**：通过颜色深浅建立空间层次，而非卡片嵌套。
- **情绪克制**：整体中性偏暖，情感色彩留给内容（日记正文与情绪分析），UI 不抢戏。
- **无障碍平等**：所有功能对屏幕阅读器用户完全可用，触摸目标不小于 48dp。

## 2. 颜色系统

使用固定色板（不启用 Dynamic Color），基于 Material3 令牌体系。

### 2.1 核心颜色

| Token | Light 值 | Dark 值 | 用途 |
|-------|----------|---------|------|
| `Primary` | `#5D5FEF` | `#A5A6F6` | 主按钮、选中态、FAB、强调链接 |
| `OnPrimary` | `#FFFFFF` | `#1B1B1F` | Primary 上的文字/图标 |
| `Secondary` | `#8B8DBF` | `#C5C6DC` | 次级按钮、标签、次要强调 |
| `Tertiary` | `#A37B68` | `#D7B8A4` | 情绪/心理相关模块的点缀色 |
| `Background` | `#FDFDFD` | `#1B1B1F` | 页面最底层背景 |
| `Surface` | `#F4F4F9` | `#2C2C33` | 卡片、列表项、输入框底色 |
| `SurfaceContainer` | `#EBEBF0` | `#373740` | 底部导航栏、顶部 AppBar |
| `SurfaceContainerHigh` | `#E0E0E8` | `#42424D` | 选中态背景、悬停态 |
| `OnSurface` | `#1B1B1F` | `#E4E2E6` | 主要文字 |
| `OnSurfaceVariant` | `#5E5E66` | `#C5C5D0` | 次要文字、时间戳、占位符 |
| `Error` | `#BA1A1A` | `#FFB4AB` | 错误、删除、高风险提示 |
| `Outline` | `#767680` | `#8E8E99` | 分隔线、边框 |
| `OutlineVariant` | `#C7C7D1` | `#46464F` | 弱分隔线 |

### 2.2 语义颜色

| Token | Light 值 | 用途 |
|-------|----------|------|
| `EmotionJoy` | `#E8A87C` | 积极情绪标签、情绪分析正面维度 |
| `EmotionCalm` | `#7BABBC` | 平静/中性情绪标签 |
| `EmotionLow` | `#9B8AA5` | 低落/负面但不危险的情绪标签 |
| `EmotionRisk` | `#C27B7B` | 高风险提示、安全警告 |
| `Success` | `#5B8A72` | 保存成功、操作完成 |

## 3. Typography

中文字体使用系统默认（`FontFamily.Default`），但通过字重和行高优化阅读节奏。

| Token | Size | Weight | Line Height | Letter Spacing | 用途 |
|-------|------|--------|-------------|----------------|------|
| `DisplaySmall` | 36sp | Medium | 44sp | -0.5sp | 空状态大标题 |
| `HeadlineLarge` | 32sp | SemiBold | 40sp | 0sp | 页面大标题 |
| `HeadlineMedium` | 28sp | SemiBold | 36sp | 0sp | 模块标题 |
| `HeadlineSmall` | 24sp | SemiBold | 32sp | 0sp | 卡片标题、文章标题 |
| `TitleLarge` | 22sp | Medium | 28sp | 0sp | 文章列表标题 |
| `TitleMedium` | 18sp | Medium | 24sp | 0.15sp | 设置项标题、Tab |
| `TitleSmall` | 16sp | Medium | 22sp | 0.1sp | 小卡片标题 |
| `BodyLarge` | 16sp | Normal | 24sp | 0.5sp | 正文、编辑器 |
| `BodyMedium` | 14sp | Normal | 20sp | 0.25sp | 摘要、描述 |
| `BodySmall` | 12sp | Normal | 16sp | 0.4sp | 时间戳、标签 |
| `LabelLarge` | 14sp | Medium | 20sp | 0.1sp | 按钮文字 |
| `LabelMedium` | 12sp | Medium | 16sp | 0.5sp | 标签、Chip |
| `LabelSmall` | 11sp | Medium | 16sp | 0.5sp | 角标、辅助 |

## 4. 间距系统

| Token | Value | 用途 |
|-------|-------|------|
| `space0` | 0.dp | 无间距 |
| `space1` | 4.dp | 图标与文字紧凑间距 |
| `space2` | 8.dp | Chip 间距、紧凑内边距 |
| `space3` | 12.dp | 卡片内部紧凑间距 |
| `space4` | 16.dp | 标准内边距、列表项间距 |
| `space5` | 20.dp | 模块之间间距 |
| `space6` | 24.dp | 页面水平内边距 |
| `space7` | 32.dp | 大模块间距、空状态 |
| `space8` | 48.dp | 超大间距、Hero 区域 |

## 5. Shape 系统

| Token | Value | 用途 |
|-------|-------|------|
| `ShapeNone` | 0.dp | 全宽分割线、无圆角需求 |
| `ShapeExtraSmall` | 4.dp | 小型 Button、Tag |
| `ShapeSmall` | 8.dp | 输入框、小型 Card |
| `ShapeMedium` | 12.dp | 中型 Card、菜单、Dialog |
| `ShapeLarge` | 16.dp | 大型 Card、Sheet |
| `ShapeExtraLarge` | 28.dp | BottomSheet、Modal |
| `ShapeFull` | 50% | FAB、圆形头像 |

## 6. 共享组件目录

所有共享组件放在 `ui/designsystem/` 下，按原子设计分层。Screen 层不直接引用 Material3 组件，只引用 Design System 组件。

### 6.1 Atoms（原子）

```kotlin
// 无状态、最小单元

@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?, // 必须提供，null 表示 decorative
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
)

@Composable
fun AppText(
    text: String,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    modifier: Modifier = Modifier,
)

@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    color: Color = DiaryTheme.colors.outlineVariant,
)
```

### 6.2 Molecules（分子）

```kotlin
// 由原子组合的简单组件

@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)

@Composable
fun AppLoadingState(
    message: String = "加载中...",
    modifier: Modifier = Modifier,
)

@Composable
fun AppErrorState(
    title: String = "出错了",
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)

@Composable
fun AppStatusBanner(
    title: String,
    message: String,
    type: StatusType, // Info | Success | Warning | Error
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)

@Composable
fun AppChip(
    label: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)

@Composable
fun AppListItem(
    headline: String,
    supporting: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

### 6.3 Organisms（有机体）

```kotlin
// 较复杂的可复用组件

@Composable
fun AppTopBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
)

@Composable
fun AppBottomBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    items: List<BottomBarItem>,
)

data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

@Composable
fun AppDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null,
)

@Composable
fun AppFAB(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
)

@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
)
```

### 6.4 Templates（模板）

```kotlin
// 页面级骨架，用于统一各 Screen 的基础布局

@Composable
fun AppScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (padding: PaddingValues) -> Unit,
)

@Composable
fun AppContentLayout(
    state: ContentState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
)

sealed class ContentState<out T> {
    data object Loading : ContentState<Nothing>()
    data class Error(val message: String) : ContentState<Nothing>()
    data class Empty(val message: String) : ContentState<Nothing>()
    data class Success<T>(val data: T) : ContentState<T>()
}
```

## 7. 各 Screen 改造简述

### 7.1 TimelineScreen（时间轴）

**当前问题**：新建文章 Card 太抢眼，文章列表用 Card 包 Card。

**改造方向**：
- 去掉全屏渐变，背景改为纯 `Background`。
- 去掉顶部"新建文章"Card，改为 **页面标题 + FAB** 布局。
  - 页面顶部：`HeadlineLarge("时间轴")` + `BodyMedium("共 N 篇日记")`。
  - 右下角 FAB：`Icons.Filled.Edit`，`contentDescription = "新建日记"`。
- 文章列表从 Card 改为 `AppListItem`：
  - 左侧：文章首字圆形头像（如"春"字）。
  - 中间：标题（`TitleLarge`）+ 日期（`BodySmall`）+ 标签 Chips。
  - 右侧：格式小标签。
- 空状态：`AppEmptyState(icon = Icons.Filled.Book, title = "还没有日记", description = "点击下方按钮开始记录", actionLabel = "写日记")`。

### 7.2 BrowserScreen（浏览）

**当前问题**：OverviewHeroCard 信息冗余，FilterRow 横向滚动不优雅，删除 Dialog 在 Screen 内部。

**改造方向**：
- 顶部改为 `AppSearchBar` + 分类 Tabs（全部 / 标签 / 时间 / 心情）。
- 搜索展开时，搜索栏占满顶部；收起时只显示搜索图标。
- 标签/时间/心情筛选器改为 **FlowRow**（`androidx.compose.foundation.layout.FlowRow`）自动换行，不再横向滚动。
- 文章列表用 `LazyColumn` + `AppListItem`，而非 Card。
- 删除确认改为 `AppDialog`，状态由 ViewModel 管理（`pendingDeleteItemId: String?`），不再由 Screen 持有 `var pendingDelete`。

### 7.3 EditorScreen（编辑器）

**当前问题**：676 行、4 个 Tab 混在一个文件、编辑器像表单、焦点管理灾难、正文空间被挤压。

**改造方向（核心变更）**：

采用 **沉浸式写作模式**，正文是绝对主角，元信息最大程度弱化。

**默认写作视图**：

```
┌─────────────────────────────┐
│ [◄]  当前标题（可点击编辑） [保存]│  ← 极简 TopBar
├─────────────────────────────┤
│                             │
│  正文写作区域...              │  ← 占满全屏，BodyLarge，1.75行高
│                             │
│  [图片预览]                   │  ← 内联在正文中
│                             │
│  继续写作...                  │
│                             │
│                             │
│                             │
│                             │
├─────────────────────────────┤
│ [+] [格式] [AI] [心理] [⋯]   │  ← 底部悬浮工具栏
└─────────────────────────────┘
```

具体规则：
- **标题**：默认显示在 TopBar 中间（`HeadlineSmall`），点击后从顶部滑出轻量编辑条（非全屏，只覆盖顶部 80dp）。编辑完成后自动收起。
- **标签**：不在默认视图显示。点击底部工具栏的"信息"图标，从底部弹出 `ModalBottomSheet`，内含标签输入和格式选择。填写完成后 Sheet 收起，标签只在正文末尾以弱色 Chips 展示。
- **正文区域**：`Column` + `Modifier.verticalScroll`，**不使用 LazyColumn**。占满 TopBar 和底部工具栏之间的全部可用空间。
  - Text Block：无边框 `BasicTextField`，全宽，`BodyLarge` 字号，行高 1.75，上下留白 `space4`。
  - Image Block：圆角图片预览（`ShapeMedium`），点击展开查看大图，长按或点击删除图标移除。
- **底部悬浮工具栏**：固定在屏幕底部，背景 `SurfaceContainer` + 模糊效果（`Modifier.blur` 或半透明蒙版）。包含：
  - 插入图片
  - 格式切换（Markdown / HTML，小弹窗）
  - AI 工具入口（点击后全屏进入 AI 处理页面，不在编辑器内做 Tab）
  - 心理分析入口（同上，全屏跳转）
  - 更多信息（标签、标题编辑、版本历史）
- **AI 处理、心理分析、文章预览**：**不再是 EditorScreen 内的 Tab**，而是独立的导航页面。从底部工具栏点击后，通过导航跳转到独立的全屏页面。这样编辑器永远只有"写作"一件事。
- **保存行为**：点击 TopBar 保存按钮，或按系统返回键时自动保存（后台静默保存，不弹 Snackbar 打扰）。
- **Dialog 统一**：放弃修改、删除确认、AI 确认都用 `AppDialog`，状态全部由 `EditorViewModel` 管理。

### 7.4 ViewerScreen（查看文章）

**当前问题**：正文包了两层 Card，阅读压抑。

**改造方向**：
- 顶部 `AppTopBar`，操作按钮：编辑、删除（放入 `DropdownMenu`）。
- 正文区域：**零 Card**，直接全屏显示，顶部显示文章元信息（标题、日期、标签），下方正文留白阅读。
- 情绪摘要用 `AppStatusBanner` 以弱背景色展示在正文上方。
- 底部可浮动"心理洞察"按钮。

### 7.5 EmotionCenterScreen（心理洞察）

**当前问题**：OverviewHeroCard 冗余，Card 套 Card。

**改造方向**：
- 顶部：页面标题"心理洞察" + 搜索图标 + "周期报告"文字链接。
- 单篇分析列表用 `LazyColumn` + `AppListItem`：
  - 左侧：情绪色块圆点（根据 labels 决定颜色）。
  - 中间：文章标题 + 最新情绪标签 Chips + 摘要一行。
  - 右侧：分析时间。
- 空状态："还没有心理分析记录，去写几篇日记吧"。

### 7.6 EmotionDetailScreen（情绪详情）

**当前问题**：Agent 过程事件滚动耦合到硬编码索引 `3 + eventIndex`。

**改造方向**：
- 顶部 `AppTopBar`。
- 重新分析区域："重新分析范围"选择器 + 开始分析按钮。
- Agent 过程：用 `LazyColumn` 展示，但每个事件用 `AppListItem` 样式，Agent 名称作为 headline，标题作为 supporting，内容折叠在下方（可展开）。
- 最新分析：全宽展示，不使用 Card，使用 `Surface` 底色区分。
- 历史分析：可折叠列表，默认折叠。

### 7.7 EmotionReportsScreen（周期报告）

**当前问题**：生成报告 Card 太抢眼，报告列表 Card 套 Card。

**改造方向**：
- 顶部：页面标题 + "生成周报"和"生成月报"两个 `OutlinedButton`。
- 空状态："还没有周期报告"。
- 报告列表：`LazyColumn` + `AppListItem`，报告标题为 headline，摘要两行，右侧为生成时间。
- 删除操作放入 `DropdownMenu`，不在列表项上直接显示删除按钮。

### 7.8 SettingsScreen（设置）

**当前问题**：Card 包 Card，结构混乱。

**改造方向**：
- 使用 **Settings 列表范式**：每个设置项占一行，左侧图标 + 标题 + 描述，右侧 Switch/箭头/输入框。
- 大模块之间用 `AppDivider` 分隔，不用 Card。
- 自定义文风：底部 Sheet 弹窗输入，不在页面内展开。
- 导入导出：两个 `OutlinedButton` 并排，放在页面底部。

### 7.9 PsychologyChatScreen（心理对话）

**当前问题**：输入框和消息区域间距不舒适，消息气泡不够清晰。

**改造方向**：
- 顶部 `AppTopBar`。
- 消息区域：`LazyColumn`，用户消息右对齐、Agent 消息左对齐。
  - 用户气泡：`Surface` + `PrimaryContainer` 背景。
  - Agent 气泡：`Surface` + `SurfaceContainer` 背景。
- 输入框固定在底部：无边框 `TextField` + 圆形发送按钮。
- 免责声明：`AppStatusBanner(type = Warning)`，可手动关闭。

### 7.10 PsychologyProfileScreen（用户画像）

**当前问题**：每个字段一张 Card，压抑。

**改造方向**：
- 使用 Settings 列表范式：每个画像字段一行，点击后底部 Sheet 编辑。
- 去掉"每行一条"的输入框，改为列表 + 添加按钮。

## 8. 导航结构

```
Bottom Navigation（4个顶级页面）:
├── 时间轴 (TimelineRoute)
├── 浏览 (BrowserRoute)
├── 心理 (EmotionRoute)  —— 含子页面：
│   ├── 情绪详情 (EmotionDetailRoute)
│   ├── 周期报告 (EmotionReportsRoute)
│   ├── 心理对话 (PsychologyChatRoute)
│   └── 用户画像 (PsychologyProfileRoute)
└── 设置 (SettingsRoute)

独立页面（从底部导航可进入）:
├── 编辑器 (EditorRoute) —— 从时间轴、浏览、查看页进入
└── 查看器 (ViewerRoute) —— 从浏览、心理进入
```

底部导航图标使用 Material Icons Extended：
- 时间轴：`Icons.AutoMirrored.Filled.MenuBook` / `Icons.AutoMirrored.Outlined.MenuBook`
- 浏览：`Icons.Filled.Search` / `Icons.Outlined.Search`
- 心理：`Icons.Filled.Psychology` / `Icons.Outlined.Psychology`
- 设置：`Icons.Filled.Settings` / `Icons.Outlined.Settings`

## 9. 无障碍规范

### 9.1 强制规则

1. **所有非装饰性图标必须有 `contentDescription`**。装饰性图标传 `null`。
2. **所有可点击元素最小触摸目标 48dp × 48dp**。`AppIconButton` 内部自动确保。
3. **复杂组件必须合并语义**：
   - 文章列表项：`Modifier.semantics(mergeDescendants = true)`，朗读格式：`"日记标题，更新于 2026-04-29，标签：生活、工作，点击查看"`。
   - 情绪分析卡片：合并为`"分析于某日，心情：开心、平静，点击查看详情"`。
4. **状态变化自动播报**：
   - 保存成功：`Modifier.semantics { liveRegion = LiveRegionKind.Polite }`。
   - 删除成功、分析完成、报告生成同理。
5. **空/加载/错误状态必须暴露 `stateDescription`**：
   - `AppLoadingState` 内部设置 `stateDescription = "正在加载"`。
   - `AppEmptyState` 设置 `stateDescription = "没有内容，点击添加"`。
6. **编辑器焦点管理**：
   - 编辑器正文区域不使用 `LazyColumn`，焦点不会在滚动时乱跳。
   - 图片块设置 `contentDescription = "图片，长按或双击删除"`。
   - 文本块设置 `semantics { editable = true; textSelectionRange = ... }`。
7. **颜色对比度**：
   - 主文字 `OnSurface` on `Background`/`Surface` ≥ 7:1。
   - 次要文字 `OnSurfaceVariant` on `Surface` ≥ 4.5:1。
   - 错误文字 `OnErrorContainer` on `ErrorContainer` ≥ 4.5:1。
   - 所有颜色组合必须通过 WCAG AA 检验。

### 9.2 TalkBack 测试清单

重构完成后，以下操作必须在 TalkBack 下完整可用：
- [ ] 新建一篇日记并保存
- [ ] 在编辑器中插入图片并删除
- [ ] 触发 AI 润色并应用结果
- [ ] 删除一篇日记
- [ ] 生成周期报告
- [ ] 在心理对话中发送和接收消息
- [ ] 修改用户画像并保存

## 10. 新增依赖

```kotlin
// app/build.gradle.kts

// Material Icons Extended（底部导航图标）
implementation("androidx.compose.material:material-icons-extended")

// FlowRow / FlowColumn（筛选器自动换行）
// 已在 Compose BOM 2024.09.01 中包含 androidx.compose.foundation:foundation
// 无需额外依赖

// 可选：如果要做图像加载优化（预览大图）
// implementation("io.coil-kt:coil-compose:2.7.0")
```

## 11. 实施阶段建议

为避免一次改动过大导致无法编译或难以 review，建议分 6 个阶段推进：

| 阶段 | 内容 | 预计改动文件数 |
|------|------|---------------|
| Phase 1 | **Design System 基础**：新建 `ui/designsystem/` 目录，实现 Theme、Token、Atoms、Molecules。同时新建 `ui/theme/` 颜色替换。 | 15-20 |
| Phase 2 | **导航骨架**：替换底部导航为图标 + `AppBottomBar`，替换所有 `TopAppBar` 为 `AppTopBar`，统一页面背景。 | 12-15 |
| Phase 3 | **编辑器重写**：`EditorScreen` 拆分为写作主界面 + 独立 AI 页面 + 独立心理页面 + 独立预览页面。重写编辑布局（去掉 LazyColumn，改为 Column+Scroll），底部工具栏，元信息 BottomSheet。 | 10-12 |
| Phase 4 | **各 Screen 逐个翻新**：Timeline → Browser → Viewer → EmotionCenter → EmotionDetail → EmotionReports → Settings → Chat → Profile。每个 Screen 独立 PR。 | 每个 3-5 |
| Phase 5 | **无障碍补完**：全局检查 `contentDescription`、触摸目标、`liveRegion`、语义合并。 | 20-30 |
| Phase 6 | **架构收尾**：可选。引入 Hilt + 类型安全导航，删除 `AppContainer` 和 `ViewModelFactory`。 | 15-20 |

每个 Phase 完成后必须：
1. `./gradlew testDebugUnitTest` 通过
2. `./gradlew assembleDebug` 通过
3. 在真机或模拟器上验证核心流程无崩溃

---

## 附录：编辑器布局对比

### 改造前（当前）

```
┌─────────────────────────────┐
│ [返回] 编辑文章        [保存] │  ← TopAppBar
├─────────────────────────────┤
│ [ 编辑 │ 预览 │ AI │ 心理 ]  │  ← ScrollableTabRow
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │ 标题输入框               │ │  ← Card
│ │ 标签输入框               │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ 正文                     │ │  ← Card + LazyColumn
│ │ [TextField]              │ │
│ │ [图片]                   │ │
│ │ [TextField]              │ │
│ └─────────────────────────┘ │
└─────────────────────────────┘
```

### 改造后（沉浸式）

```
┌─────────────────────────────┐
│ [◄]  当前标题（可点击编辑） [保存]│  ← 极简 TopBar
├─────────────────────────────┤
│                             │
│  正文写作区域...              │  ← 占满全屏，充足留白
│                             │
│  [图片预览]                   │
│                             │
│  继续写作...                  │
│                             │
│                             │
│                             │
│                             │
│                             │
├─────────────────────────────┤
│ [+] [格式] [AI] [心理] [⋯]   │  ← 底部悬浮工具栏
└─────────────────────────────┘
```

元信息编辑（点击 TopBar 标题或底部工具栏"信息"时弹出）：

```
┌─────────────────────────────┐
│         文章信息              │  ← BottomSheet 顶部把手
├─────────────────────────────┤
│  标题                        │
│  [________________________] │
│                             │
│  标签                        │
│  [________________________] │
│  生活, 工作, 旅行            │
│                             │
│  格式                        │
│  [Markdown ▼]               │
│                             │
│         [完成]               │
└─────────────────────────────┘
```

# 影笺代码优化修复 SPEC

**日期：** 2026-08-11  
**适用工程：** YingJian Android App  
**范围：** 基于当前代码审查发现的 10 项问题，修复可靠性、数据安全、性能、存储生命周期和维护性风险。  

---

## 1. 背景与目标

当前工程已能通过单元测试和 lint，但代码审查发现若干不会被编译器直接拦截的运行时风险：

- PDF 分享路径缺少 `FileProvider` 配置，存在点击分享崩溃风险。
- 画册保存流程不是数据库事务，存在半保存导致页面丢失的风险。
- Room 仍启用 destructive migration 兜底，后续迁移遗漏时可能清空用户数据。
- 部分图片和数据库操作在主线程路径执行，存在卡顿风险。
- 归档图片文件没有生命周期清理，长期使用会造成私有存储膨胀。
- PDF 导出和配置维护存在静默失败、N+1 查询、权限/Exif API 使用不规范等问题。

本次优化目标：

1. 修复会直接崩溃、丢数据或造成明显卡顿的问题。
2. 保持现有功能行为和 UI 入口不做大范围重构。
3. 所有高风险改动必须有可运行验证。
4. 不引入新的远程存储、登录、云同步等超出当前范围的功能。

---

## 2. 总体实施原则

- **先修硬问题：** PDF 分享、事务保存、数据库迁移兜底、主线程阻塞优先。
- **小步提交：** 每个阶段完成后运行测试，避免一次性改动过大。
- **保持现有架构：** 继续使用当前 `core/feature` 包结构、Room Repository、Compose 页面结构。
- **用户数据优先：** 任何删除、迁移、文件清理逻辑都必须避免误删仍被引用的数据。
- **失败可见：** 关键导出/分享/保存失败必须给用户反馈，不能静默吞掉。

---

## 3. 优先级与阶段划分

| 阶段 | 项目 | 优先级 | 目标 |
|---|---|---:|---|
| Phase 1 | PDF 分享、事务保存、迁移兜底 | P0 | 修复崩溃和数据安全问题 |
| Phase 2 | 主线程阻塞、图片元数据 IO 化 | P1 | 降低选图和编辑卡顿 |
| Phase 3 | 归档清理、PDF 错误处理 | P1 | 提升存储和导出可靠性 |
| Phase 4 | 权限/Exif、N+1 查询、lint 整理 | P2 | 降低维护成本和性能隐患 |

---

## 4. Phase 1：可靠性与数据安全

### 4.1 修复 PDF 分享 FileProvider 缺失

**问题说明：**  
预览页分享 PDF 使用 `FileProvider.getUriForFile()`，但 Manifest 未注册 provider，也没有 paths XML。点击分享时可能抛出 `IllegalArgumentException`。

**涉及文件：**

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/file_paths.xml`（新增）
- `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`

**实施方案：**

1. 在 `AndroidManifest.xml` 的 `<application>` 内注册 provider：
   - `android:name="androidx.core.content.FileProvider"`
   - `android:authorities="${applicationId}.fileprovider"`
   - `android:exported="false"`
   - `android:grantUriPermissions="true"`
   - `meta-data android:name="android.support.FILE_PROVIDER_PATHS"`
2. 新增 `res/xml/file_paths.xml`：
   - 允许分享 `cacheDir` 中生成的 PDF。
   - 范围尽量收敛，例如只开放 `cache-path name="pdf_cache" path="."`。
3. 预览分享失败时显示 Snackbar 或 Toast。
4. 不改变当前分享文件生成位置：继续使用 `context.cacheDir/photobook_{id}.pdf`。

**验收标准：**

- 点击预览页分享按钮，系统分享面板正常弹出。
- provider authority 与 `getUriForFile()` 使用的 authority 一致。
- 分享失败时用户能看到明确提示。

**测试建议：**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

手工验证：

1. 创建或打开一本画册。
2. 进入预览页。
3. 点击分享按钮。
4. 确认无崩溃，分享面板出现，目标 app 能收到 PDF。

---

### 4.2 画册保存改为数据库事务

**问题说明：**  
当前保存流程先删除全部页面布局，再逐页插入，最后更新画册信息。中途失败会造成页面被删但新页面未完全写入。

**涉及文件：**

- `app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt`
- `app/src/main/java/com/yingjian/core/data/repository/PhotobookRepository.kt`
- `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`
- `app/src/test/java/...`（新增事务相关单测，如适用）

**实施方案：**

1. 在 Repository 层增加一个原子保存方法，例如：
   - `suspend fun replacePhotobookLayoutsAndUpdateBook(photobook, pageLayouts)`
2. 使用 Room 事务能力：
   - 推荐在 DAO 或 Database 上使用 `@Transaction`。
   - 或在 Repository 内通过 `database.withTransaction { ... }` 实现。
3. 将当前 `onSave` 中的：
   - `deletePageLayouts`
   - 多次 `savePageLayout`
   - `updatePhotobook`
   合并到同一个事务方法。
4. 保留 UI 层的保存按钮和“已保存”提示，但只有事务成功后再提示成功。
5. 事务失败时显示“保存失败，请重试”。

**验收标准：**

- 保存操作成功后，画册页面、封面布局、封底布局一起更新。
- 保存失败时不会出现页面全部被删除的半保存状态。
- UI 成功提示只在数据库写入成功后出现。

**测试建议：**

- 单测覆盖 Repository 事务方法的正常路径。
- 如测试条件允许，模拟插入中途异常，确认旧数据未被破坏。

---

### 4.3 移除 `fallbackToDestructiveMigration()`

**问题说明：**  
数据库已声明 1-6 自动迁移，但仍启用 destructive fallback。后续版本如忘记迁移，会直接删库重建，照片日记数据存在不可接受的数据丢失风险。

**涉及文件：**

- `app/src/main/java/com/yingjian/AppDependencies.kt`
- `app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt`
- `app/schemas/com.yingjian.core.data.database.YingJianDatabase/*.json`

**实施方案：**

1. 从 Room builder 中移除 `.fallbackToDestructiveMigration()`。
2. 保留现有 `autoMigrations` 和 `exportSchema = true`。
3. 后续每次数据库版本升级必须补充 migration 或 auto migration。
4. 如当前缺少迁移测试，新增基础 migration 验证计划：
   - 至少验证当前最新 schema 能打开。
   - 后续版本升级时必须验证从上一版本迁移到最新版本。

**验收标准：**

- Room builder 不再包含 destructive fallback。
- 当前单元测试通过。
- 后续缺迁移时应在开发/测试阶段失败，而不是运行时删库。

---

## 5. Phase 2：主线程与选图性能

### 5.1 移除编辑器 `runBlocking`

**问题说明：**  
编辑器重置图片时在 Compose 回调中使用 `runBlocking` 查询数据库，调用线程会被阻塞。

**涉及文件：**

- `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`

**实施方案：**

1. 删除 `runBlocking` import 和用法。
2. 在 `onResetSelectedSlotImage` 内使用已有 `coroutineScope.launch`。
3. 数据库读取放入 `withContext(Dispatchers.IO)`。
4. 读取到 memory 后再计算 reset page 并调用 `setEditorBookState()`。
5. 如果 memory 不存在，直接返回或显示轻量提示。

**验收标准：**

- 代码中不再出现 `runBlocking`。
- 点击“重置图片”时功能行为不变。
- 数据库读取仍在 IO dispatcher。

**测试建议：**

- 运行现有画册相关单测。
- 手工验证编辑器中重置选中图片，页面模板、图片位置恢复正常。

---

### 5.2 图片元数据读取迁移到 IO 线程

**问题说明：**  
选图后读取图片尺寸和 Exif 时间会打开图片流。当前部分调用发生在 UI 回调路径，大图或多图场景可能卡顿。

**涉及文件：**

- `app/src/main/java/com/yingjian/feature/memories/MemoriesScreen.kt`
- `app/src/main/java/com/yingjian/feature/memories/NewPostScreen.kt`
- `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`
- `app/src/main/java/com/yingjian/feature/photobook/PhotoPickerScreen.kt`

**实施方案：**

1. 将 `getImageMetadata()` 改造成 suspend 版本，或新增：
   - `suspend fun getImageMetadataAsync(context, uri)`
   - 内部统一 `withContext(Dispatchers.IO)`。
2. 多图场景按列表批量读取元数据，避免 UI 回调中同步 map。
3. `MemoriesScreen` 和 `NewPostScreen` 的 picker 回调只启动协程，不直接读取图片流。
4. 读取失败时使用当前时间和 `0x0` 尺寸兜底，保持现有容错行为。
5. 避免重复读取同一张图片的 metadata；必要时在一次选择流程内缓存结果。

**验收标准：**

- UI 回调中不直接执行 `BitmapFactory.decodeStream` 或 `ExifInterface(stream)`。
- 多选 9 张图片时界面无明显冻结。
- 新建影记的日期仍优先使用 Exif 日期，失败时才 fallback。

**测试建议：**

- 给 `getImageMetadata` 的日期解析逻辑补充单测（可抽出纯函数解析 Exif 日期字符串）。
- 手工选择多张大图，确认进入发布页流畅。

---

## 6. Phase 3：存储生命周期与 PDF 导出

### 6.1 增加归档图片清理机制

**问题说明：**  
图片导入后会复制到 `filesDir/image_archive`。删除影记或删除影记中的某张照片时，只更新数据库，不清理私有归档文件。长期使用会造成存储膨胀。

**涉及文件：**

- `app/src/main/java/com/yingjian/core/media/ImageArchiveStore.kt`
- `app/src/main/java/com/yingjian/core/media/ImageArchiveRepository.kt`
- `app/src/main/java/com/yingjian/core/data/repository/MemoryRepository.kt`
- `app/src/main/java/com/yingjian/core/data/repository/PhotobookRepository.kt`
- `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`
- `app/src/main/java/com/yingjian/feature/memories/MemoryDetailScreen.kt`

**实施方案：**

1. 在 `ImageArchiveStore` 增加安全删除方法：
   - 只允许删除 `image_archive` 目录内文件。
   - 使用 canonical path 校验，防止误删目录外文件。
2. 增加引用检查方法：
   - 检查所有 `memory_record.imageUri` 和 `imageUrisJson`。
   - 检查所有画册 `page_layout.elementsJson` 中的 image URI。
   - 检查 `photobook.coverImageUri`。
3. 删除单张照片时：
   - 先更新数据库。
   - 再检查被移除 URI 是否仍被引用。
   - 未被引用才删除归档文件。
4. 删除整条影记时：
   - 收集该影记所有图片 URI。
   - 删除影记数据库记录。
   - 对每个 URI 做引用检查和安全删除。
5. 暂不删除用户系统相册原文件，只处理 app 私有归档文件。

**验收标准：**

- 删除影记后，不再被引用的归档文件会被清理。
- 被其他影记或画册引用的图片不会被误删。
- 非 `image_archive` 内的 file URI 不会被删除。

**测试建议：**

- 单测 `ImageArchiveStore.deleteIfArchived()`：
  - archive 内文件可删。
  - archive 外文件不可删。
- 单测引用检查：
  - memory 引用存在时不删。
  - page layout 引用存在时不删。
  - cover 引用存在时不删。

---

### 6.2 改进 PDF 导出错误处理和流关闭

**问题说明：**  
当前 `renderImageSlot()` 用 `runCatching` 静默吞掉图片解码失败。图片打不开时 PDF 可能缺图，但用户不知道原因。部分流关闭也应统一使用 `use`。

**涉及文件：**

- `app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt`
- `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`

**实施方案：**

1. 将图片读取全部改为 `openInputStream(uri)?.use { ... }`。
2. 引入轻量导出结果模型，例如：
   - `PdfExportResult.Success`
   - `PdfExportResult.SuccessWithWarnings(missingImages)`
   - `PdfExportResult.Failure(error)`
3. 如保持 `writePdf()` API 不变，可至少在调用层收集失败计数或抛出明确异常。
4. 图片解码失败时：
   - 不让整个 app 崩溃。
   - 但导出完成后提示“部分图片无法读取，PDF 可能缺图”。
5. 对不可打开输出流、写入失败、权限失败分别给出可见提示。

**验收标准：**

- 图片读取异常不会造成资源泄漏。
- PDF 缺图场景有用户可见提示。
- 完全导出失败时不会显示“导出成功”。

**测试建议：**

- 单测 `PdfImageRenderMath` 保持通过。
- 构造不可读 URI 手工验证提示行为。

---

## 7. Phase 4：维护性与性能优化

### 7.1 处理图片权限和 ExifInterface

**问题说明：**  
当前 Manifest 声明 `READ_MEDIA_IMAGES`，但主要使用 Android Photo Picker。lint 提示 Android 14+ Selected Photos Access 未处理。同时代码使用 `android.media.ExifInterface`，项目已依赖 AndroidX ExifInterface。

**涉及文件：**

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/yingjian/feature/memories/MemoriesScreen.kt`
- `app/src/main/java/com/yingjian/feature/photobook/PhotoPickerScreen.kt`
- `gradle/libs.versions.toml`

**实施方案：**

1. 评估当前是否仍需要直接读取系统媒体库：
   - 如果全流程都通过 Photo Picker 获得 URI，移除 `READ_MEDIA_IMAGES` 和旧版 `READ_EXTERNAL_STORAGE`。
   - 如果仍需要媒体库全量扫描，再补 Android 14 partial access 处理。
2. 将 import 从：
   - `android.media.ExifInterface`
   替换为：
   - `androidx.exifinterface.media.ExifInterface`
3. 保留现有 `androidx.exifinterface:exifinterface` 依赖。

**验收标准：**

- lint 不再提示 `SelectedPhotoAccess` 和平台 `ExifInterface`。
- Android 13/14 选图流程正常。
- 无多余运行时相册权限弹窗。

---

### 7.2 优化画册列表 N+1 查询

**问题说明：**  
加载画册列表时先取所有画册，再逐本查询页数。画册数量增加后会产生 N+1 查询。

**涉及文件：**

- `app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt`
- `app/src/main/java/com/yingjian/core/data/repository/PhotobookRepository.kt`
- `app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt`

**实施方案：**

1. 新增查询结果 DTO，例如：
   - `data class PageCountByPhotobook(val photobookId: Long, val pageCount: Int)`
2. 在 `PageLayoutDao` 增加：
   - `SELECT photobookId, COUNT(*) AS pageCount FROM page_layout GROUP BY photobookId`
3. Repository 增加：
   - `suspend fun getPageCountsByPhotobook(): Map<Long, Int>`
4. `PhotobookViewModel.loadPhotobooks()` 改为：
   - 一次查画册列表。
   - 一次查页数 map。
   - 更新 UI state。

**验收标准：**

- 画册列表页数显示与优化前一致。
- 加载画册列表时不再按画册数量循环查询页数。

**测试建议：**

- Repository/DAO 单测验证多个 photobook 的 page count map。
- 手工创建多本画册，确认列表页数正确。

---

### 7.3 整理低风险 lint 和依赖维护项

**问题说明：**  
当前 lint 通过但存在多项 warning，包括依赖版本过期、硬编码依赖未放版本目录、未用字符串资源、Compose primitive state 装箱等。

**涉及文件：**

- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`
- 其他 lint 指出的 Compose 文件

**实施方案：**

1. 将硬编码依赖移动到 `libs.versions.toml`：
   - `androidx.compose.ui:ui-tooling`
   - `junit:junit`
2. 将 `mutableStateOf(0)` 改为 `mutableIntStateOf(0)`。
3. 未用字符串资源处理策略：
   - 仍计划国际化或资源化 UI 文案：改代码引用资源。
   - 不计划使用：删除未用资源。
4. 依赖升级单独评估：
   - AGP、Compose BOM、Room、Navigation、ExifInterface 可分批升级。
   - 每次升级后运行测试和 lint。
5. `mipmap-anydpi-v26` 因 `minSdk = 26` 可后续合并到非 v26 目录。

**验收标准：**

- `lintDebug` warning 数量下降。
- 依赖声明统一走版本目录。
- 不因批量升级引入编译或运行时兼容问题。

---

## 8. 建议实施顺序

1. **先做 P0：**
   - FileProvider 分享修复。
   - 画册保存事务。
   - 移除 destructive migration。
2. **再做 P1：**
   - 移除 `runBlocking`。
   - 图片元数据 IO 化。
   - PDF 错误处理。
   - 归档图片清理。
3. **最后做 P2：**
   - 权限/Exif 整理。
   - N+1 查询优化。
   - lint 和依赖维护。

推荐分支或提交拆分：

| 提交 | 内容 |
|---|---|
| commit 1 | Fix PDF sharing FileProvider |
| commit 2 | Make photobook save transactional |
| commit 3 | Remove destructive migration fallback |
| commit 4 | Move blocking editor and metadata IO off main thread |
| commit 5 | Add archive cleanup and PDF export warnings |
| commit 6 | Clean up permissions, Exif, page-count query, lint items |

---

## 9. 总体验收清单

### 功能验收

- [ ] PDF 预览分享可以弹出系统分享面板。
- [ ] PDF 导出失败时显示失败提示。
- [ ] 画册编辑保存成功后，重新进入编辑器数据保持一致。
- [ ] 保存失败不会导致画册页面被清空。
- [ ] 重置图片不会阻塞 UI。
- [ ] 多图选择和发布流程流畅。
- [ ] 删除影记或照片后，未被引用的私有归档文件会被清理。
- [ ] 被画册或其他影记引用的图片不会被误删。
- [ ] Android 13/14 选图流程不出现不必要权限问题。
- [ ] 画册列表页数显示正确。

### 自动化验证

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

### 手工验证

1. 新建影记，选择 1 张和多张图片。
2. 编辑影记，删除单张照片和整条影记。
3. 新建画册，添加图片，调整裁切，保存后重新进入。
4. 编辑封面/封底文字，保存后进入预览。
5. 导出 PDF 到本地文件。
6. 预览页分享 PDF 到任意支持接收文件的应用。
7. 在 Android 13/14 设备或模拟器上验证选图权限体验。

---

## 10. 不在本次范围内

- 新增 S3/SMB/FTP 的真实上传下载能力。
- 重做整体 UI 设计或导航架构。
- 引入完整依赖注入框架。
- 改造为分页数据源或 Flow 全量响应式架构。
- 重新设计画册模板算法。
- 清理用户系统相册中的原始图片。

---

## 11. 风险与回滚策略

| 风险 | 影响 | 控制方式 |
|---|---|---|
| 事务保存改动影响现有保存流程 | 页面无法保存或保存不完整 | 保留原序列化逻辑，只调整事务边界；补充单测 |
| 移除 destructive fallback 后旧版本数据库打不开 | 老用户升级失败 | 确认 1-6 schema 自动迁移完整；后续版本补 migration 测试 |
| 图片清理误删仍被引用文件 | 画册或影记缺图 | 删除前做全库引用检查，只删除 `image_archive` 内文件 |
| 权限删除后部分设备无法读取图片 | 选图后图片不可访问 | 确认 Photo Picker URI 已复制归档；不依赖长期外部权限 |
| 依赖升级引入兼容问题 | 编译或运行失败 | 依赖升级单独提交，逐项验证 |

---

## 12. 完成定义

本 spec 完成的标准：

1. 10 项优化均完成或明确标记为延期并说明原因。
2. `:app:testDebugUnitTest` 通过。
3. `:app:lintDebug` 通过，且关键 warning 已处理或有记录。
4. PDF 导出、PDF 分享、影记删除、画册保存四条主流程完成手工验证。
5. 没有引入新的 destructive migration、主线程 `runBlocking` 或静默关键失败。

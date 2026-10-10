# Android GitHub Release 发布流程

本文是影笺 Android 正式版发布的操作清单，记录版本更新、签名、GitHub Actions 检查和常见故障处理。

## 发布链路

推送 `v*` 标签会启动 [.github/workflows/release.yml](../../.github/workflows/release.yml)，依次执行单元测试、Release APK 构建、正式证书签名校验和 GitHub Release 上传。Release 只有在最后一步成功后才算发布完成。

### 发布前检查

- 更新 `app/build.gradle.kts` 中的 `versionName` 和递增的 `versionCode`，并在 `CHANGELOG.md` 记录本次对用户可见的变化。
- 确认工作区中只有本次准备发布的改动，且本地正式签名包可构建、可验签。
- 检查工作流仍使用受支持的 Android SDK setup Action。当前使用 `android-actions/setup-android@v4.0.4`；不要退回 `v3`。旧版会请求 Google 已下架的 `tools` SDK 包，导致 `Set up Android SDK` 失败并阻断发布。升级 Action 时先查看[官方版本记录](https://github.com/android-actions/setup-android/releases)和[当前使用说明](https://github.com/android-actions/setup-android)。
- 确认 GitHub 仓库已配置 `SIGNING_KEY_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` 四个 Actions secrets。只检查名称，不打印或提交 secret 值。

## 本地构建和签名

后续命令中的 `${VERSION}` 都指本次 `versionName`，例如：

```bash
export VERSION="1.0.6" # 下次发布时改成新版本号
```

构建、测试 Release APK：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="/Users/haos/Library/Android/sdk"
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleRelease
```

使用仓库外的签名配置签名并校验。不要将 keystore、`credentials.env`、签名 APK 或 `.idsig` 文件提交到 Git。

```bash
source ~/.yingjian-release-signing/credentials.env
unsigned_apk="app/build/outputs/apk/release/app-release-unsigned.apk"
aligned_apk="/tmp/YingJian-${VERSION}-aligned.apk"
signed_apk="$PWD/release-artifacts/YingJian-${VERSION}-release.apk"

mkdir -p release-artifacts
"$ANDROID_HOME/build-tools/35.0.0/zipalign" -p -f 4 "$unsigned_apk" "$aligned_apk"
"$ANDROID_HOME/build-tools/35.0.0/apksigner" sign \
  --ks "$KEYSTORE_PATH" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass env:KEYSTORE_PASSWORD \
  --key-pass env:KEY_PASSWORD \
  --out "$signed_apk" \
  "$aligned_apk"
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs "$signed_apk"
```

签名证书 SHA-256 指纹应为：

```text
38036d6cb242da8a0aa690a62dece6da6d6d3f72fdf09a47f6bd5d63a2f8d397
```

若指纹不同，先停止发布并检查 keystore，不能用新密钥替代已发布应用的签名证书。

## 提交、推送和打标签

先确认远端状态，避免覆盖或遗漏其他人的提交：

```bash
git status -sb
git fetch origin
git log --graph --oneline --decorate --left-right origin/main...main
```

提交版本号、更新日志和本次功能改动后，创建与 `versionName` 一致的注释标签，再推送分支和标签：

```bash
git tag -a "v${VERSION}" -m "Release v${VERSION}"
git push origin main
git push origin "v${VERSION}"
```

如果 `main` 推送提示远端有新提交，先 `fetch` 并检查差异，再通过 rebase 或 merge 保留远端工作；不要强推 `main`。若本地与远端有内容相同但提交哈希不同的提交，比较树和文件差异后再整理历史。

## 检查 Actions 和 GitHub Release

GitHub CLI 认证及工作流检查：

```bash
gh auth status
gh run list --repo 5umrd4y/YingJian --workflow "Android Release" --limit 5
gh run watch <run-id> --repo 5umrd4y/YingJian --exit-status
gh release view "v${VERSION}" --repo 5umrd4y/YingJian --json url,assets,publishedAt
```

只有满足以下条件才确认发布完成：

1. 对应标签的 `Android Release` 工作流结论为 `success`。
2. Release 页面已创建且不是草稿。
3. 资产包含 `YingJian-v${VERSION}-release.apk`，状态为上传完成。
4. 签名步骤显示 `apksigner verify` 成功，证书指纹与上方记录一致。

保留 Release 链接、工作流链接、APK 文件名和 GitHub 提供的 SHA-256 digest 作为发布记录。

## 故障处理

### Android SDK 初始化提示找不到 `tools`

典型日志为：

```text
Warning: Failed to find package 'tools'
sdkmanager ... failed with exit code 1
```

原因是旧版 `android-actions/setup-android@v3` 请求了已下架的 `tools` 包。将 `.github/workflows/release.yml` 更新到官方当前支持版本（本项目目前为 `@v4.0.4`），提交并推送修复后再触发发布。不要只重跑旧标签的同一份失败工作流，因为它仍会读取标签上的旧工作流文件。

如果失败的标签尚未创建 Release，可在确认标签目标和远端状态后，将该标签更新到修复提交并重新推送。**若 Release 已正式发布，不要移动或强推已发布标签**；应按版本策略发布新的修正版版本。

仅在确认 Release 不存在且失败运行尚未发布资产时，才执行：

```bash
git tag -fa "v${VERSION}" -m "Release v${VERSION}"
git push --force origin "refs/tags/v${VERSION}"
```

### `gh auth status` 显示 token 无效

重新登录并再次核验：

```bash
gh auth login -h github.com
gh auth status
```

Git 推送凭据和 GitHub CLI 凭据可能来自不同的凭据存储；分别验证 `git push` 与 `gh auth status`，不要因为 Git 推送成功就假定 `gh` 已登录。

### GitHub 连接失败或代理端口拒绝连接

先查看 Git 实际使用的代理配置和端口，不要照抄旧端口：

```bash
git config --show-origin --get http.proxy
```

若 Git 配置了已失效的本机代理，可以在单次命令中覆盖。直连可用时：

```bash
git -c http.proxy= push origin main
```

如果本机代理 `127.0.0.1:7890` 正在运行，可使用有低速超时的单次配置：

```bash
git -c http.proxy=http://127.0.0.1:7890 \
  -c http.lowSpeedLimit=100 \
  -c http.lowSpeedTime=10 \
  push origin main
```

本机代理端口可能变化；先确认代理正在监听，再选择直连或当前可用端口。网络失败后检查远端状态，避免盲目重复推送。

### 标签已推送，但 Release 没有创建

先检查 Actions 对应运行的失败步骤和完整日志，再判断 Release 是否已创建。修复环境或工作流后，只有确认该版本还没有已发布 Release 时才考虑更新同名标签；工作流读取标签所指提交中的文件，因此修复提交必须成为该标签的目标。

```bash
gh run view <run-id> --repo 5umrd4y/YingJian --log-failed
gh release view "v${VERSION}" --repo 5umrd4y/YingJian
```

Release 已存在时不要覆盖其标签或资产；应按正式版本修复流程发布新版本。

### Secrets、签名或资产上传失败

用 `gh secret list --repo 5umrd4y/YingJian` 确认四个 secret 名称存在，不要读取 secret 值。检查 Actions 中 `Sign release APK` 与 `Publish GitHub Release` 步骤的日志；确认工作流拥有 `contents: write`。密钥错误应修复 GitHub Secrets，不要重新生成密钥或更换已发布应用的证书。

## 密钥位置

本机签名材料保存在仓库外：

```text
~/.yingjian-release-signing/yingjian-release.jks
~/.yingjian-release-signing/credentials.env
```

Android 应用更换签名密钥会破坏已有安装的升级兼容性。除非明确进行身份迁移，不要替换此密钥。

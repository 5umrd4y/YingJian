# Android GitHub Release 发布流程

本文记录影笺 Android App 的签名和 GitHub Release 自动发布流程。

## 当前发布方式

项目使用 GitHub Actions 在推送 `v*` tag 时自动完成：

1. 检出代码。
2. 安装 JDK 17 和 Android SDK。
3. 执行单元测试和 `assembleRelease`。
4. 从 GitHub Secrets 还原 release keystore。
5. 使用 Android SDK `zipalign` 和 `apksigner` 对 release APK 签名。
6. 上传已签名 APK 到 GitHub Releases。

工作流文件：

```text
.github/workflows/release.yml
```

当前 release 资产命名：

```text
YingJian-v1.0.4-release.apk
```

## 签名密钥

本机 release keystore 存放在仓库外：

```text
~/.yingjian-release-signing/yingjian-release.jks
~/.yingjian-release-signing/credentials.env
```

这些文件不能提交到 Git。`.gitignore` 已忽略常见签名文件：

```text
*.jks
*.keystore
*.p12
*.pem
*.key
```

GitHub Actions 使用以下 repository secrets：

```text
SIGNING_KEY_BASE64
KEYSTORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

不要在仓库、issue、release notes、聊天记录中暴露 secret 的实际值。

## 首次配置或重建 Secrets

如果需要重新生成 keystore，先确认这是一次有意的身份更换。Android 应用发布后更换 keystore 会影响升级安装兼容性。

生成 keystore：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
mkdir -p ~/.yingjian-release-signing
chmod 700 ~/.yingjian-release-signing

"$JAVA_HOME/bin/keytool" -genkeypair -v \
  -keystore ~/.yingjian-release-signing/yingjian-release.jks \
  -storetype PKCS12 \
  -storepass "<KEYSTORE_PASSWORD>" \
  -keypass "<KEYSTORE_PASSWORD>" \
  -alias "yingjian-release" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=YingJian, OU=YingJian, O=YingJian, L=Unknown, ST=Unknown, C=CN"
```

写入 GitHub Secrets：

```bash
base64 -i ~/.yingjian-release-signing/yingjian-release.jks \
  | gh secret set SIGNING_KEY_BASE64 --repo 5umrd4y/YingJian

printf '%s' "<KEYSTORE_PASSWORD>" \
  | gh secret set KEYSTORE_PASSWORD --repo 5umrd4y/YingJian

printf '%s' "yingjian-release" \
  | gh secret set KEY_ALIAS --repo 5umrd4y/YingJian

printf '%s' "<KEYSTORE_PASSWORD>" \
  | gh secret set KEY_PASSWORD --repo 5umrd4y/YingJian
```

检查 secret 名称是否存在：

```bash
gh secret list --repo 5umrd4y/YingJian
```

## 发布新版本

1. 修改版本号：

```kotlin
// app/build.gradle.kts
versionCode = 6
versionName = "1.0.5"
```

2. 本地验证：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleRelease
```

3. 本地签名验证，确认 keystore 和签名链路可用：

```bash
export ANDROID_HOME="/Users/haos/Library/Android/sdk"
source ~/.yingjian-release-signing/credentials.env

unsigned_apk="$(find app/build/outputs/apk/release -name '*-release-unsigned.apk' -print -quit)"
aligned_apk="/tmp/YingJian-v1.0.5-aligned.apk"
signed_apk="$PWD/release-artifacts/YingJian-v1.0.5-release.apk"

mkdir -p release-artifacts
"$ANDROID_HOME/build-tools/35.0.0/zipalign" -p -f 4 "$unsigned_apk" "$aligned_apk"

KEYSTORE_PASSWORD="$KEYSTORE_PASSWORD" KEY_PASSWORD="$KEY_PASSWORD" \
  "$ANDROID_HOME/build-tools/35.0.0/apksigner" sign \
  --ks "$KEYSTORE_PATH" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass env:KEYSTORE_PASSWORD \
  --key-pass env:KEY_PASSWORD \
  --out "$signed_apk" \
  "$aligned_apk"

"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs "$signed_apk"
```

4. 提交、打 tag、推送：

```bash
git status -sb
git add app/build.gradle.kts
git commit -m "chore: bump app version to 1.0.5"

git tag -a v1.0.5 -m "Release v1.0.5"
git push origin main
git push origin v1.0.5
```

5. 监控 Actions：

```bash
gh run list --repo 5umrd4y/YingJian --workflow "Android Release" --limit 5
gh run watch <run-id> --repo 5umrd4y/YingJian --exit-status
```

6. 查看 Release：

```bash
gh release view v1.0.5 --repo 5umrd4y/YingJian --json url,assets,publishedAt
```

## 代理环境

如果 `git push`、`gh api` 或 `curl https://api.github.com` 出现 connection reset，而系统代理可用，显式指定代理：

```bash
HTTPS_PROXY=http://127.0.0.1:7897 \
HTTP_PROXY=http://127.0.0.1:7897 \
ALL_PROXY=socks5://127.0.0.1:7897 \
gh run list --repo 5umrd4y/YingJian --workflow "Android Release" --limit 5
```

同理可用于 `git push`：

```bash
HTTPS_PROXY=http://127.0.0.1:7897 \
HTTP_PROXY=http://127.0.0.1:7897 \
ALL_PROXY=socks5://127.0.0.1:7897 \
git push origin main
```

## CI 依赖源策略

`settings.gradle.kts` 使用条件分支：

- GitHub Actions 中 `CI=true`：只使用官方 `gradlePluginPortal()`、`google()`、`mavenCentral()`。
- 本机：保留 Aliyun 镜像作为依赖下载入口，并保留官方源兜底。

原因：

- GitHub Runner 访问官方源稳定，Aliyun 镜像偶发 `502` 会导致 Gradle 禁用仓库。
- 本机访问官方 Google/Maven Central 可能 TLS/网络不稳定，Aliyun 镜像更可靠。

## 常见问题

### OAuth App cannot create or update workflow

错误：

```text
refusing to allow an OAuth App to create or update workflow without workflow scope
```

处理：

```bash
gh auth refresh -h github.com -s workflow
```

### Gradle wrapper 指向本机路径

错误：

```text
gradle-9.4.1-bin.zip (No such file or directory)
```

处理：`gradle/wrapper/gradle-wrapper.properties` 必须使用公开 distribution：

```text
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
```

### KSP 插件无法解析

优先检查 `settings.gradle.kts` 的 `pluginManagement.repositories`。CI 应使用官方源优先，否则镜像可能缺失插件元数据。

### Aliyun 镜像 502

如果 CI 日志中出现：

```text
maven.aliyun.com ... 502 Bad Gateway
```

确认 `CI=true` 时没有使用 Aliyun 依赖源。

### Release 已存在或 tag 需要重发

正常发布不要强推 tag。只有在首次配置流水线、release 未被正式使用时才考虑：

```bash
git tag -f -a v1.0.5 -m "Release v1.0.5"
git push --force origin v1.0.5
```

如果远端 main 被临时 API 提交污染，应先确认远端 sha，再使用 `--force-with-lease`，避免覆盖别人提交。

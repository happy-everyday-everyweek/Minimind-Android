# MiniMind Android 移动端 - 内嵌 Linux 后端开箱即用架构 Spec

## Why
MiniMind 目前仅有 Streamlit WebUI 和 OpenAI 兼容 API 服务端，无法在移动端直接使用。需要构建一个前后端分离的 Android 应用：前端使用 Android 原生（Kotlin + Jetpack Compose），后端使用内嵌在 APK 中的 Linux 运行环境（proot），将 Python 解释器、PyTorch、模型文件及所有依赖全部封装在 APK 内，用户安装 APK 后即可开箱即用，无需额外安装 Termux 或手动配置任何环境。

## What Changes
- 后端：基于现有 `serve_openai_api.py` 扩展，增加健康检查、模型列表等端点，适配 proot 环境运行
- 内嵌 Linux 运行环境：将 proot 二进制、精简 Linux rootfs（含 Python + PyTorch + transformers）、模型文件打包到 APK assets 中
- 前端：新建 Android 原生项目（Kotlin + Jetpack Compose），实现聊天界面、流式响应、思考过程展示、工具调用展示、参数配置、多语言（中/英）、会话历史管理
- 本地通信：Android 前端通过 localhost HTTP/SSE 与内嵌 Linux 环境中运行的后端 API 交互
- 服务生命周期管理：Android 应用负责内嵌 Linux 后端服务的启动、停止和状态监控

## Impact
- Affected specs: `serve_openai_api.py` 需扩展端点
- Affected code: `scripts/serve_openai_api.py`（后端增强）、新增 Android 项目目录 `android/`、新增 Linux rootfs 构建脚本
- 现有训练、模型代码不受影响

---

## ADDED Requirements

### Requirement: 内嵌 Linux 运行环境

APK SHALL 内嵌一个完整的 Linux 运行环境，无需用户额外安装任何软件：

#### Scenario: 首次启动初始化
- **WHEN** 用户首次安装并打开 APK
- **THEN** 应用从 APK assets 中提取 proot 二进制和 Linux rootfs 到应用内部存储
- **WHEN** 提取完成后
- **THEN** 应用自动在 proot 环境中启动后端 API 服务

#### Scenario: 后续启动
- **WHEN** 用户再次打开应用且 rootfs 已解压
- **THEN** 跳过解压步骤，直接启动后端服务

#### Scenario: proot 运行环境
- **WHEN** 后端服务在 proot 环境中运行
- **THEN** Python 解释器、PyTorch、transformers 等依赖均可用
- **WHEN** proot 环境中无 CUDA
- **THEN** 后端自动使用 CPU 推理，打印警告而非报错

#### Scenario: 模型文件管理
- **WHEN** 模型文件打包在 APK assets 中
- **THEN** 首次启动时解压到内部存储，后续直接加载
- **WHEN** 模型文件较大导致 APK 体积过大
- **THEN** 支持首次启动时从网络下载模型到内部存储（可选方案）

---

### Requirement: 后端 API 增强

后端 SHALL 在现有 `serve_openai_api.py` 基础上扩展以下能力：

#### Scenario: 健康检查
- **WHEN** 客户端发送 `GET /v1/health`
- **THEN** 返回 `{"status": "ok", "model_loaded": true}`

#### Scenario: 模型信息查询
- **WHEN** 客户端发送 `GET /v1/models`
- **THEN** 返回当前加载模型的名称、参数量等信息，格式兼容 OpenAI `/v1/models`

#### Scenario: 流式聊天响应
- **WHEN** 客户端发送 `POST /v1/chat/completions` 且 `stream: true`
- **THEN** 后端以 SSE 格式返回流式响应，包含 `reasoning_content`（思考内容）和 `content`（正文内容）以及 `tool_calls`

#### Scenario: 非流式聊天响应
- **WHEN** 客户端发送 `POST /v1/chat/completions` 且 `stream: false`
- **THEN** 后端返回完整 JSON 响应

#### Scenario: 服务关闭端点
- **WHEN** 客户端发送 `POST /v1/shutdown`
- **THEN** 后端优雅关闭，释放资源

---

### Requirement: Android 原生聊天应用

Android 应用 SHALL 使用 Kotlin + Jetpack Compose 构建，提供以下功能：

#### Scenario: 开箱即用体验
- **WHEN** 用户安装 APK 后首次打开
- **THEN** 应用自动初始化内嵌 Linux 环境并启动后端服务，显示初始化进度
- **WHEN** 后端服务就绪后
- **THEN** 自动进入聊天界面，无需用户手动配置

#### Scenario: 后端服务生命周期管理
- **WHEN** 应用启动时后端服务未运行
- **THEN** 应用自动启动后端服务
- **WHEN** 应用退出时
- **THEN** 应用通知后端服务优雅关闭
- **WHEN** 后端服务异常崩溃
- **THEN** 应用检测到并自动重启服务

#### Scenario: 聊天对话
- **WHEN** 用户输入消息并发送
- **THEN** 应用调用后端 `/v1/chat/completions` 接口，以流式方式接收并实时显示回复
- **WHEN** 回复包含 `reasoning_content`
- **THEN** 思考内容以可折叠区域显示，默认折叠
- **WHEN** 回复包含 `tool_calls`
- **THEN** 工具调用以卡片形式展示工具名称和参数

#### Scenario: 模型参数配置
- **WHEN** 用户进入设置页面
- **THEN** 可调整 Temperature（0.6-1.2）、Max Tokens（256-8192）、历史对话轮次（0-8）、是否开启思考模式
- **WHEN** 用户修改参数后
- **THEN** 后续对话使用新参数

#### Scenario: 会话管理
- **WHEN** 用户创建新对话
- **THEN** 应用清空当前对话历史，开始新会话
- **WHEN** 用户退出对话页面
- **THEN** 对话历史保存在本地（Room 数据库），下次打开可继续
- **WHEN** 用户删除对话
- **THEN** 对话从本地数据库移除

#### Scenario: 多语言支持
- **WHEN** 用户在设置中切换语言（中文/English）
- **THEN** 应用界面文本切换为对应语言

#### Scenario: 服务异常处理
- **WHEN** 内嵌后端服务启动失败或无响应
- **THEN** 应用显示友好错误提示和重试按钮，不崩溃
- **WHEN** 流式传输中断
- **THEN** 应用保留已接收的内容并提示连接中断

---

### Requirement: Linux rootfs 构建脚本

项目 SHALL 提供自动化脚本构建精简 Linux rootfs：

#### Scenario: rootfs 构建
- **WHEN** 开发者运行 rootfs 构建脚本
- **THEN** 脚本自动创建精简 Linux rootfs，包含 Python、PyTorch（CPU ARM64）、transformers、模型文件等
- **WHEN** 构建完成
- **THEN** rootfs 可打包为压缩包供 APK assets 使用

#### Scenario: rootfs 体积优化
- **WHEN** 构建 rootfs 时
- **THEN** 剔除不必要的文件（文档、测试、缓存等），最小化体积
- **WHEN** PyTorch 仅需 CPU 版本
- **THEN** 仅安装 CPU 版本的 torch 以减少体积

---

## MODIFIED Requirements

### Requirement: serve_openai_api.py 扩展
原有的 `serve_openai_api.py` 需增加以下端点，同时保持现有 `/v1/chat/completions` 端点完全兼容：
- `GET /v1/health` - 健康检查
- `GET /v1/models` - 模型信息
- `POST /v1/shutdown` - 优雅关闭
- 默认绑定地址为 `127.0.0.1`，可通过 `--host` 参数覆盖
- CPU fallback 逻辑：无 CUDA 时自动使用 CPU，打印警告而非报错

## REMOVED Requirements

无移除需求。所有现有功能保持不变。

# MiniMind Android 移动端 + 本地 Linux 后端分离架构 Spec

## Why
MiniMind 目前仅有 Streamlit WebUI 和 OpenAI 兼容 API 服务端，无法在移动端直接使用。需要构建一个前后端分离的架构：Android 原生应用作为前端，在手机本地运行的 Linux 实例（Termux）中部署后端服务，使 MiniMind 能够在安卓手机上以原生体验进行对话交互，所有计算和数据均在本地完成，无需远程服务器。

## What Changes
- 后端：基于现有 `serve_openai_api.py` 扩展，增加健康检查、模型列表等端点，适配 Termux 环境运行
- 前端：新建 Android 原生项目（Kotlin + Jetpack Compose），实现聊天界面、流式响应、思考过程展示、工具调用展示、参数配置、多语言（中/英）、会话历史管理
- 本地通信：Android 前端通过 localhost HTTP/SSE 与 Termux 中运行的后端 API 交互
- 部署：提供 Termux 一键部署脚本，在手机本地 Linux 环境中安装依赖、配置并启动后端服务

## Impact
- Affected specs: `serve_openai_api.py` 需扩展端点
- Affected code: `scripts/serve_openai_api.py`（后端增强）、新增 Android 项目目录 `android/`、新增部署脚本 `scripts/deploy_termux.sh`
- 现有训练、模型代码不受影响

---

## ADDED Requirements

### Requirement: 后端 API 增强

后端 SHALL 在现有 `serve_openai_api.py` 基础上扩展以下能力，适配 Termux 本地运行环境：

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

#### Scenario: Termux 环境适配
- **WHEN** 后端在 Termux 环境中启动
- **THEN** 自动检测运行设备（CPU/GPU），默认使用 CPU 模式运行
- **WHEN** Termux 环境中无 CUDA 可用
- **THEN** 后端自动 fallback 到 CPU 推理，不报错退出

---

### Requirement: Android 原生聊天应用

Android 应用 SHALL 使用 Kotlin + Jetpack Compose 构建，提供以下功能：

#### Scenario: 本地服务连接配置
- **WHEN** 用户首次打开应用或进入设置页面
- **THEN** 用户可配置本地服务地址（默认 `http://127.0.0.1:8998`）和端口
- **WHEN** 用户保存配置后
- **THEN** 应用自动测试与本地 Termux 服务的连接并提示连接状态
- **WHEN** 本地服务未启动
- **THEN** 应用提示用户需先在 Termux 中启动后端服务

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

#### Scenario: 本地服务异常处理
- **WHEN** 本地 Termux 服务未启动或无响应
- **THEN** 应用显示友好提示，引导用户启动 Termux 服务，不崩溃
- **WHEN** 流式传输中断
- **THEN** 应用保留已接收的内容并提示连接中断

---

### Requirement: Termux 本地部署方案

后端 SHALL 支持在 Android 手机的 Termux 环境中一键部署和运行：

#### Scenario: Termux 一键部署
- **WHEN** 用户在 Termux 中运行部署脚本
- **THEN** 脚本自动安装 Python、pip 依赖、下载/配置模型文件、启动 API 服务
- **WHEN** 部署脚本检测到已安装的环境
- **THEN** 跳过已完成的步骤，仅更新缺失部分

#### Scenario: Termux 服务管理
- **WHEN** 用户在 Termux 中执行启动命令
- **THEN** 后端服务在后台启动，监听 `127.0.0.1:8998`
- **WHEN** 用户执行停止命令
- **THEN** 后端服务正确停止

#### Scenario: 模型文件管理
- **WHEN** 用户首次部署时本地无模型文件
- **THEN** 部署脚本提示用户下载模型或指定本地模型路径
- **WHEN** 用户指定了模型路径
- **THEN** 后端使用指定路径加载模型

---

## MODIFIED Requirements

### Requirement: serve_openai_api.py 扩展
原有的 `serve_openai_api.py` 需增加以下端点，同时保持现有 `/v1/chat/completions` 端点完全兼容：
- `GET /v1/health` - 健康检查
- `GET /v1/models` - 模型信息
- 默认绑定地址改为 `127.0.0.1`（本地安全），可通过 `--host` 参数覆盖
- CPU fallback 逻辑：无 CUDA 时自动使用 CPU，打印警告而非报错

## REMOVED Requirements

无移除需求。所有现有功能保持不变。

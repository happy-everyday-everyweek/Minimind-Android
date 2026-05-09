# MiniMind Android 移动端 + Linux 后端分离架构 Spec

## Why
MiniMind 目前仅有 Streamlit WebUI 和 OpenAI 兼容 API 服务端，无法在移动端直接使用。需要构建一个前后端分离的架构：Android 原生应用作为前端，Linux 实例运行后端服务，使 MiniMind 能够在安卓手机上以原生体验进行对话交互。

## What Changes
- 后端：基于现有 `serve_openai_api.py` 扩展，增加健康检查、模型列表、会话管理、CORS 支持、简单 Token 鉴权等端点，部署到 Linux 实例
- 前端：新建 Android 原生项目（Kotlin + Jetpack Compose），实现聊天界面、流式响应、思考过程展示、工具调用展示、参数配置、多语言（中/英）、会话历史管理
- 通信：前端通过 HTTP/SSE 与后端 OpenAI 兼容 API 交互

## Impact
- Affected specs: `serve_openai_api.py` 需扩展端点
- Affected code: `scripts/serve_openai_api.py`（后端增强）、新增 Android 项目目录 `android/`
- 现有训练、模型代码不受影响

---

## ADDED Requirements

### Requirement: 后端 API 增强

后端 SHALL 在现有 `serve_openai_api.py` 基础上扩展以下能力：

#### Scenario: 健康检查
- **WHEN** 客户端发送 `GET /v1/health`
- **THEN** 返回 `{"status": "ok", "model_loaded": true}`

#### Scenario: 模型信息查询
- **WHEN** 客户端发送 `GET /v1/models`
- **THEN** 返回当前加载模型的名称、参数量等信息，格式兼容 OpenAI `/v1/models`

#### Scenario: CORS 跨域支持
- **WHEN** 来自移动端的跨域请求到达后端
- **THEN** 后端正确返回 CORS 头，允许跨域访问

#### Scenario: 简单 Token 鉴权
- **WHEN** 客户端请求携带 `Authorization: Bearer <token>` 头
- **THEN** 后端验证 token 是否匹配配置值；不匹配则返回 401
- **WHEN** 后端启动参数 `--api_key` 未设置时
- **THEN** 跳过鉴权检查（向后兼容）

#### Scenario: 流式聊天响应
- **WHEN** 客户端发送 `POST /v1/chat/completions` 且 `stream: true`
- **THEN** 后端以 SSE 格式返回流式响应，包含 `reasoning_content`（思考内容）和 `content`（正文内容）以及 `tool_calls`

#### Scenario: 非流式聊天响应
- **WHEN** 客户端发送 `POST /v1/chat/completions` 且 `stream: false`
- **THEN** 后端返回完整 JSON 响应

---

### Requirement: Android 原生聊天应用

Android 应用 SHALL 使用 Kotlin + Jetpack Compose 构建，提供以下功能：

#### Scenario: 服务器连接配置
- **WHEN** 用户首次打开应用或进入设置页面
- **THEN** 用户可配置服务器地址（如 `http://192.168.1.100:8998`）和 API Key
- **WHEN** 用户保存配置后
- **THEN** 应用自动测试连接并提示连接状态

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

#### Scenario: 网络异常处理
- **WHEN** 网络不可用或服务器无响应
- **THEN** 应用显示友好的错误提示，不崩溃
- **WHEN** 流式传输中断
- **THEN** 应用保留已接收的内容并提示连接中断

---

### Requirement: 后端部署脚本

后端 SHALL 提供一键部署能力：

#### Scenario: Linux 实例部署
- **WHEN** 用户在 Linux 实例上运行部署脚本
- **THEN** 脚本自动安装 Python 依赖、配置防火墙规则、启动 API 服务

#### Scenario: 服务管理
- **WHEN** 用户执行启动/停止/重启命令
- **THEN** 后端服务正确响应

---

## MODIFIED Requirements

### Requirement: serve_openai_api.py 扩展
原有的 `serve_openai_api.py` 需增加以下端点，同时保持现有 `/v1/chat/completions` 端点完全兼容：
- `GET /v1/health` - 健康检查
- `GET /v1/models` - 模型信息
- CORS 中间件
- 可选的 Bearer Token 鉴权中间件
- 启动参数 `--api_key` 用于配置鉴权密钥

## REMOVED Requirements

无移除需求。所有现有功能保持不变。

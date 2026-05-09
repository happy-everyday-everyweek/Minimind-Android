# Tasks

- [ ] Task 1: 后端 API 增强 - 扩展 serve_openai_api.py
  - [ ] SubTask 1.1: 添加 `GET /v1/health` 健康检查端点
  - [ ] SubTask 1.2: 添加 `GET /v1/models` 模型信息端点
  - [ ] SubTask 1.3: 修改默认绑定地址为 `127.0.0.1`，新增 `--host` 启动参数
  - [ ] SubTask 1.4: 添加 CPU fallback 逻辑：无 CUDA 时自动使用 CPU 并打印警告
  - [ ] SubTask 1.5: 验证现有 `/v1/chat/completions` 端点功能不受影响

- [ ] Task 2: Termux 本地部署脚本
  - [ ] SubTask 2.1: 创建 `scripts/deploy_termux.sh` 一键部署脚本（安装 Termux 包、Python 依赖、配置模型路径、启动服务）
  - [ ] SubTask 2.2: 实现服务管理命令（start/stop/status），使用 Termux:Boot 或 nohup 后台运行
  - [ ] SubTask 2.3: 实现模型文件管理逻辑（检测本地模型、提示下载或指定路径）

- [ ] Task 3: Android 项目初始化
  - [ ] SubTask 3.1: 创建 Android 项目目录结构 `android/`，配置 Gradle、Kotlin、Jetpack Compose
  - [ ] SubTask 3.2: 配置项目依赖：Retrofit + OkHttp（网络）、Room（本地存储）、Navigation（导航）、Material3（UI）、DataStore（偏好存储）

- [ ] Task 4: Android 网络层实现
  - [ ] SubTask 4.1: 实现 OpenAI 兼容 API 的 Retrofit 接口定义（chat completions、health、models）
  - [ ] SubTask 4.2: 实现 SSE 流式响应解析器（OkHttp EventSource 或自定义）
  - [ ] SubTask 4.3: 实现本地服务连接异常处理和提示逻辑

- [ ] Task 5: Android 数据层实现
  - [ ] SubTask 5.1: 定义 Room 数据库实体（Conversation、Message）
  - [ ] SubTask 5.2: 实现 DAO 和 Repository，支持会话的增删改查
  - [ ] SubTask 5.3: 实现 DataStore 用户偏好存储（服务地址、端口、参数设置、语言）

- [ ] Task 6: Android 聊天界面实现
  - [ ] SubTask 6.1: 实现聊天列表页面（会话列表、新建/删除会话）
  - [ ] SubTask 6.2: 实现聊天对话页面（消息气泡、输入框、发送按钮）
  - [ ] SubTask 6.3: 实现流式响应实时渲染（逐字显示）
  - [ ] SubTask 6.4: 实现思考内容（reasoning_content）可折叠展示
  - [ ] SubTask 6.5: 实现工具调用（tool_calls）卡片式展示

- [ ] Task 7: Android 设置页面实现
  - [ ] SubTask 7.1: 实现本地服务配置（地址默认 127.0.0.1、端口默认 8998、连接测试）
  - [ ] SubTask 7.2: 实现模型参数配置（Temperature、Max Tokens、历史轮次、思考模式开关）
  - [ ] SubTask 7.3: 实现多语言切换（中文/English）

- [ ] Task 8: Android 导航与整体集成
  - [ ] SubTask 8.1: 实现 Navigation 导航图（会话列表 -> 聊天 -> 设置）
  - [ ] SubTask 8.2: 实现 Application 初始化（DI、数据库、网络客户端）
  - [ ] SubTask 8.3: 端到端集成测试：Android 客户端连接本地 Termux 后端完成一次完整对话

# Task Dependencies
- Task 2 依赖 Task 1（部署脚本需要增强后的 API）
- Task 4 依赖 Task 1（网络层接口需匹配后端 API）
- Task 6 依赖 Task 4 和 Task 5（聊天界面需要网络层和数据层）
- Task 7 依赖 Task 5（设置页面需要 DataStore）
- Task 8 依赖 Task 6 和 Task 7（集成需要所有模块就绪）
- Task 1 和 Task 3 可并行执行

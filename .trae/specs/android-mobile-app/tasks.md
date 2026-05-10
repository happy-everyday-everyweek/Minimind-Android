# Tasks

- [ ] Task 1: 后端 API 增强 - 扩展 serve_openai_api.py
  - [ ] SubTask 1.1: 添加 `GET /v1/health` 健康检查端点
  - [ ] SubTask 1.2: 添加 `GET /v1/models` 模型信息端点
  - [ ] SubTask 1.3: 添加 `POST /v1/shutdown` 优雅关闭端点
  - [ ] SubTask 1.4: 修改默认绑定地址为 `127.0.0.1`，新增 `--host` 启动参数
  - [ ] SubTask 1.5: 添加 CPU fallback 逻辑：无 CUDA 时自动使用 CPU 并打印警告
  - [ ] SubTask 1.6: 验证现有 `/v1/chat/completions` 端点功能不受影响

- [ ] Task 2: Linux rootfs 构建脚本
  - [ ] SubTask 2.1: 创建 `scripts/build_rootfs.sh` 脚本，基于 Alpine/Ubuntu ARM64 构建精简 rootfs
  - [ ] SubTask 2.2: 在 rootfs 中安装 Python 3、pip、PyTorch（CPU ARM64）、transformers、FastAPI、uvicorn 等依赖
  - [ ] SubTask 2.3: 将 MiniMind 项目代码和模型文件复制到 rootfs 中
  - [ ] SubTask 2.4: 剔除不必要的文件（__pycache__、.pyc、文档、测试、缓存），最小化 rootfs 体积
  - [ ] SubTask 2.5: 将 rootfs 打包为 tar.gz 压缩包，供 APK assets 使用
  - [ ] SubTask 2.6: 编译 proot ARM64 二进制，供 APK 内嵌使用

- [ ] Task 3: Android 项目初始化
  - [ ] SubTask 3.1: 创建 Android 项目目录结构 `android/`，配置 Gradle、Kotlin、Jetpack Compose
  - [ ] SubTask 3.2: 配置项目依赖：Retrofit + OkHttp（网络）、Room（本地存储）、Navigation（导航）、Material3（UI）、DataStore（偏好存储）
  - [ ] SubTask 3.3: 配置 APK assets 目录，放置 proot 二进制和 rootfs 压缩包

- [ ] Task 4: Android 内嵌 Linux 环境管理
  - [ ] SubTask 4.1: 实现 ProotManager 类：管理 proot 二进制的提取和执行权限设置
  - [ ] SubTask 4.2: 实现 RootfsInstaller 类：首次启动时从 assets 解压 rootfs 到内部存储，后续跳过
  - [ ] SubTask 4.3: 实现 LinuxServiceManager 类：通过 proot 启动/停止后端 Python 服务
  - [ ] SubTask 4.4: 实现服务健康监控：定期调用 `/v1/health` 检测服务状态，异常时自动重启
  - [ ] SubTask 4.5: 实现初始化进度回调：向前端报告 rootfs 解压和服务启动进度

- [ ] Task 5: Android 网络层实现
  - [ ] SubTask 5.1: 实现 OpenAI 兼容 API 的 Retrofit 接口定义（chat completions、health、models、shutdown）
  - [ ] SubTask 5.2: 实现 SSE 流式响应解析器（OkHttp EventSource 或自定义）
  - [ ] SubTask 5.3: 实现本地服务连接异常处理和重试逻辑

- [ ] Task 6: Android 数据层实现
  - [ ] SubTask 6.1: 定义 Room 数据库实体（Conversation、Message）
  - [ ] SubTask 6.2: 实现 DAO 和 Repository，支持会话的增删改查
  - [ ] SubTask 6.3: 实现 DataStore 用户偏好存储（参数设置、语言）

- [ ] Task 7: Android 聊天界面实现
  - [ ] SubTask 7.1: 实现初始化/加载页面（显示 rootfs 解压和服务启动进度）
  - [ ] SubTask 7.2: 实现聊天列表页面（会话列表、新建/删除会话）
  - [ ] SubTask 7.3: 实现聊天对话页面（消息气泡、输入框、发送按钮）
  - [ ] SubTask 7.4: 实现流式响应实时渲染（逐字显示）
  - [ ] SubTask 7.5: 实现思考内容（reasoning_content）可折叠展示
  - [ ] SubTask 7.6: 实现工具调用（tool_calls）卡片式展示

- [ ] Task 8: Android 设置页面实现
  - [ ] SubTask 8.1: 实现模型参数配置（Temperature、Max Tokens、历史轮次、思考模式开关）
  - [ ] SubTask 8.2: 实现多语言切换（中文/English）
  - [ ] SubTask 8.3: 实现服务状态显示（运行中/已停止）和手动重启按钮

- [ ] Task 9: Android 导航与整体集成
  - [ ] SubTask 9.1: 实现 Navigation 导航图（初始化 -> 会话列表 -> 聊天 -> 设置）
  - [ ] SubTask 9.2: 实现 Application 初始化（DI、数据库、网络客户端、LinuxServiceManager）
  - [ ] SubTask 9.3: 实现应用退出时优雅关闭后端服务
  - [ ] SubTask 9.4: 端到端集成测试：安装 APK -> 自动初始化 -> 完成一次完整对话

# Task Dependencies
- Task 2 依赖 Task 1（rootfs 需包含增强后的 API 代码）
- Task 4 依赖 Task 2 和 Task 3（内嵌 Linux 管理需要 rootfs 和 Android 项目结构）
- Task 5 依赖 Task 1（网络层接口需匹配后端 API）
- Task 7 依赖 Task 5 和 Task 6（聊天界面需要网络层和数据层）
- Task 8 依赖 Task 6（设置页面需要 DataStore）
- Task 9 依赖 Task 7 和 Task 8（集成需要所有模块就绪）
- Task 1、Task 3 可并行执行

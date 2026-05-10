# Tasks

- [x] Task 1: 后端 Linux 实例集成层
  - [x] SubTask 1.1: 确定手机端 Linux 运行方案（PRoot/Termux），编写 Linux 实例启动和管理模块
  - [x] SubTask 1.2: 编写 Linux 实例自动初始化脚本：安装 Python、PyTorch、项目依赖
  - [x] SubTask 1.3: 编写后端 API 服务（FastAPI），封装模型加载、推理、训练等接口
  - [x] SubTask 1.4: 实现 WebSocket 接口，支持推理流式输出和训练进度推送
  - [x] SubTask 1.5: 预置训练好的 Tokenizer 到后端环境，确保开箱即用

- [x] Task 2: 后端推理 API
  - [x] SubTask 2.1: 实现模型权重管理 API（列表、加载、删除、下载预置权重）
  - [x] SubTask 2.2: 实现推理 API，支持参数配置（temperature、top_p、max_tokens、open_thinking）
  - [x] SubTask 2.3: 实现流式推理 WebSocket 接口，支持 reasoning_content 输出

- [x] Task 3: 后端训练 API
  - [x] SubTask 3.1: 实现预训练 API，封装 train_pretrain.py，支持参数配置和进度上报
  - [x] SubTask 3.2: 实现 SFT API，封装 train_full_sft.py，支持参数配置和进度上报
  - [x] SubTask 3.3: 实现黑盒知识蒸馏 API，支持配置外部 LLM API 作为教师模型，自动获取教师输出并训练学生模型
  - [x] SubTask 3.4: 实现 LoRA 训练 API，封装 train_lora.py，支持自定义数据集上传
  - [x] SubTask 3.5: 实现强化学习训练 API，封装 train_dpo.py / train_ppo.py / train_grpo.py，支持算法选择
  - [x] SubTask 3.6: 实现 Agentic RL 训练 API，封装 train_agent.py，支持自定义工具定义和奖励函数
  - [x] SubTask 3.7: 实现训练控制 API（暂停/恢复/停止），支持断点续训
  - [x] SubTask 3.8: 实现训练进度和 loss 数据的 WebSocket 推送

- [x] Task 4: 后端数据管理 API
  - [x] SubTask 4.1: 实现数据集管理 API（列表、上传自定义 jsonl、预览、删除）
  - [x] SubTask 4.2: 实现预置数据集的自动下载和解压
  - [x] SubTask 4.3: 实现 LLM API 配置管理（保存/读取 API 地址、Key、模型名）

- [x] Task 5: Android 前端 - 基础框架
  - [x] SubTask 5.1: 创建 Android 项目，配置 Kotlin + Jetpack Compose
  - [x] SubTask 5.2: 实现底部导航栏和三个顶级页面（开始、推理、训练）的导航框架
  - [x] SubTask 5.3: 实现后端 Linux 实例的启动、状态监控和通信层（HTTP + WebSocket 客户端）
  - [x] SubTask 5.4: 实现首次启动引导流程（自动初始化后端、显示进度）

- [x] Task 6: Android 前端 - 开始页面
  - [x] SubTask 6.1: 实现应用简介和后端状态指示器
  - [x] SubTask 6.2: 实现快速开始卡片（开始对话、开始训练）
  - [x] SubTask 6.3: 实现最近活动记录展示

- [x] Task 7: Android 前端 - 推理页面
  - [x] SubTask 7.1: 实现模型选择器（列出可用模型权重）
  - [x] SubTask 7.2: 实现对话界面，支持流式显示模型回复
  - [x] SubTask 7.3: 实现推理参数配置面板（temperature、top_p、max_tokens、open_thinking）
  - [x] SubTask 7.4: 实现思考过程（reasoning_content）的折叠展示

- [x] Task 8: Android 前端 - 训练页面
  - [x] SubTask 8.1: 实现训练流程步骤列表，显示各步骤状态和依赖关系
  - [x] SubTask 8.2: 实现预训练配置界面（数据集选择、模型配置、训练参数）
  - [x] SubTask 8.3: 实现 SFT 配置界面（基础权重选择、数据集选择、训练参数）
  - [x] SubTask 8.4: 实现黑盒知识蒸馏配置界面（教师 API 配置、学生模型选择、训练参数）
  - [x] SubTask 8.5: 实现 LoRA 微调配置界面（基础权重、数据集上传、LoRA 参数）
  - [x] SubTask 8.6: 实现强化学习配置界面（算法选择 DPO/PPO/GRPO、对应参数配置）
  - [x] SubTask 8.7: 实现 Agentic RL 可视化环境编辑器（工具定义、奖励函数规则编辑、测试场景配置）
  - [x] SubTask 8.8: 实现 AI 辅助生成奖励函数功能（调用配置的 LLM API）
  - [x] SubTask 8.9: 实现训练监控界面（loss 曲线图、训练日志、进度条、控制按钮）

- [x] Task 9: Android 前端 - 模型与数据管理
  - [x] SubTask 9.1: 实现模型权重管理界面（列表、下载预置权重、删除、导出）
  - [x] SubTask 9.2: 实现数据集管理界面（列表、上传 jsonl、预览、删除）
  - [x] SubTask 9.3: 实现 LLM API 配置界面（API 地址、Key、模型名，支持 DeepSeek/智谱等预设）

- [x] Task 10: 集成测试与优化
  - [x] SubTask 10.1: 端到端测试：安装 App -> 初始化 -> 推理对话 -> 训练模型全流程
  - [x] SubTask 10.2: 性能优化：后端 Linux 实例启动速度、内存占用、训练速度
  - [x] SubTask 10.3: 用户体验优化：加载状态、错误提示、操作引导

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1]
- [Task 4] depends on [Task 1]
- [Task 5] depends on [Task 1] (需要后端 API 定义)
- [Task 6] depends on [Task 5]
- [Task 7] depends on [Task 5, Task 2]
- [Task 8] depends on [Task 5, Task 3]
- [Task 9] depends on [Task 5, Task 4]
- [Task 10] depends on [Task 6, Task 7, Task 8, Task 9]
- [Task 8.7, Task 8.8] (Agentic RL 编辑器) 可与 Task 8.2-8.6 并行开发

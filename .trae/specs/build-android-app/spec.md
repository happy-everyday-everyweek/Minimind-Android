# MiniMind Android App Spec

## Why
MiniMind 当前仅提供命令行和 Web 端交互方式，用户需要在 PC 上配置 Python 环境才能使用。为了让普通用户在手机上就能开箱即用地体验 LLM 的推理与训练全流程，需要开发一个 Android 原生 App，通过前后端分离架构，在用户手机上运行一个 Linux 实例作为后端，承载所有训练和推理服务，实现"安装即用"的体验。

## What Changes
- 新增 Android 原生前端应用，提供三个顶级页面：开始、推理、训练
- 新增后端 Linux 实例服务层，运行于用户手机上的 Linux 环境（如 Termux/PRoot），承载 Python 训练和推理服务
- 新增前后端通信协议（HTTP REST API + WebSocket），前端通过 API 调用后端服务
- 新增后端 API 服务，封装 MiniMind 现有的训练脚本和推理接口
- 新增训练可视化配置界面，用户可在手机上配置训练参数、监控训练进度
- 新增 Agentic RL 可视化环境编辑器，用户可通过可视化界面配置智能体环境、工具和奖励函数
- 新增黑盒知识蒸馏功能，用户可配置外部 LLM API（DeepSeek、智谱等）作为教师模型
- 使用项目已有的训练好的 Tokenizer，不暴露 Tokenizer 训练步骤给用户

## Impact
- Affected specs: 推理能力、训练能力、模型管理、数据管理
- Affected code: 新增 Android 前端项目、新增后端 API 服务层、复用现有 trainer/ 和 model/ 代码

## ADDED Requirements

### Requirement: Android 原生前端应用
系统 SHALL 提供一个 Android 原生应用，包含三个顶级页面（底部导航栏切换）：开始、推理、训练。

#### Scenario: 用户启动应用
- **WHEN** 用户首次打开应用
- **THEN** 系统自动初始化后端 Linux 实例，显示启动进度，完成后进入"开始"页面

#### Scenario: 页面导航
- **WHEN** 用户点击底部导航栏的"开始"/"推理"/"训练"
- **THEN** 切换到对应页面，保持页面状态

### Requirement: 开始页面
系统 SHALL 提供一个"开始"页面，作为应用首页，展示应用概览和快捷入口。

#### Scenario: 查看开始页面
- **WHEN** 用户进入开始页面
- **THEN** 显示：应用简介、后端服务状态指示器、快速开始卡片（如"开始对话"、"开始训练"）、最近活动记录

#### Scenario: 后端服务状态异常
- **WHEN** 后端 Linux 实例未运行或异常
- **THEN** 显示错误状态和"重新初始化"按钮

### Requirement: 推理页面
系统 SHALL 提供一个"推理"页面，用户可与模型进行对话交互。

#### Scenario: 进入推理页面
- **WHEN** 用户进入推理页面
- **THEN** 显示模型选择器（列出可用模型权重）、对话界面、推理参数配置（temperature、top_p、max_tokens、open_thinking 开关）

#### Scenario: 发送消息
- **WHEN** 用户输入消息并发送
- **THEN** 通过 WebSocket 流式接收模型回复，实时显示生成内容，支持思考过程（reasoning_content）展示

#### Scenario: 切换模型
- **WHEN** 用户在推理页面切换模型权重
- **THEN** 后端加载对应权重，后续推理使用新模型

### Requirement: 训练页面
系统 SHALL 提供一个"训练"页面，包含训练流程的各阶段入口，用户可从预训练到后训练完整执行。

#### Scenario: 查看训练页面
- **WHEN** 用户进入训练页面
- **THEN** 显示训练流程步骤列表，每个步骤显示状态（未开始/进行中/已完成），以及训练进度和日志查看入口

#### Scenario: 训练流程步骤
- **THEN** 训练页面 SHALL 展示以下步骤（按顺序）：
  1. 预训练 (Pretrain) - 必须
  2. 有监督微调 (SFT) - 必须
  3. 黑盒知识蒸馏 (Black-box Distillation) - 可选
  4. LoRA 微调 - 可选
  5. 强化学习 (RL) - 可选，内含 DPO/PPO/GRPO 三种算法选择
  6. Agentic RL - 可选

### Requirement: 预训练配置与执行
系统 SHALL 允许用户配置并执行预训练。

#### Scenario: 配置预训练
- **WHEN** 用户点击"预训练"步骤
- **THEN** 显示配置界面，包含：数据集选择（pretrain_t2t / pretrain_t2t_mini）、模型配置（dim、n_layers）、训练参数（learning_rate、batch_size、epochs、save_interval）、输出权重名称

#### Scenario: 执行预训练
- **WHEN** 用户启动预训练
- **THEN** 后端执行 train_pretrain.py，前端实时显示训练 loss 曲线和日志，训练完成后自动保存权重

### Requirement: SFT 配置与执行
系统 SHALL 允许用户配置并执行有监督微调。

#### Scenario: 配置 SFT
- **WHEN** 用户点击"SFT"步骤
- **THEN** 显示配置界面，包含：基础权重选择（预训练输出权重）、数据集选择（sft_t2t / sft_t2t_mini）、训练参数（learning_rate、batch_size、epochs、max_seq_len）

#### Scenario: 执行 SFT
- **WHEN** 用户启动 SFT
- **THEN** 后端执行 train_full_sft.py，前端实时显示训练 loss 曲线和日志

### Requirement: 黑盒知识蒸馏
系统 SHALL 提供黑盒知识蒸馏功能，用户可配置外部 LLM API 作为教师模型。

#### Scenario: 配置知识蒸馏
- **WHEN** 用户点击"知识蒸馏"步骤
- **THEN** 显示配置界面，包含：教师模型 API 配置（API 地址、API Key、模型名称，支持 DeepSeek、智谱等 OpenAI 兼容 API）、学生模型权重选择、蒸馏数据集配置、训练参数

#### Scenario: 执行知识蒸馏
- **WHEN** 用户启动知识蒸馏
- **THEN** 后端通过配置的 API 获取教师模型输出，将其作为训练数据对学生模型进行微调，前端显示蒸馏进度

### Requirement: LoRA 微调
系统 SHALL 允许用户配置并执行 LoRA 微调。

#### Scenario: 配置 LoRA
- **WHEN** 用户点击"LoRA 微调"步骤
- **THEN** 显示配置界面，包含：基础权重选择、LoRA 数据集（支持用户自定义 jsonl 上传）、LoRA 参数（rank、alpha）、训练参数

#### Scenario: 执行 LoRA
- **WHEN** 用户启动 LoRA 训练
- **THEN** 后端执行 train_lora.py，前端显示训练进度

### Requirement: 强化学习训练
系统 SHALL 提供强化学习训练步骤，用户可选择 DPO、PPO 或 GRPO 算法。

#### Scenario: 配置强化学习
- **WHEN** 用户点击"强化学习"步骤
- **THEN** 显示算法选择器（DPO / PPO / GRPO），根据选择显示对应配置参数：
  - DPO：基础权重选择、偏好数据集、beta 参数
  - PPO：基础权重选择、RL 数据集、KL 系数、clip epsilon
  - GRPO：基础权重选择、RL 数据集、分组大小、KL 系数、loss_type（grpo/cispo）

#### Scenario: 执行强化学习训练
- **WHEN** 用户启动 RL 训练
- **THEN** 后端执行对应训练脚本，前端显示 reward 曲线和训练日志

### Requirement: Agentic RL 训练
系统 SHALL 提供 Agentic RL 训练功能，包含可视化环境编辑器。

#### Scenario: 可视化环境编辑
- **WHEN** 用户点击"Agentic RL"步骤
- **THEN** 显示可视化环境编辑器，用户可：
  - 定义工具（Tool）列表：工具名称、参数 schema、描述
  - 配置奖励函数：通过可视化规则编辑器定义奖励条件（格式正确性、工具调用合法性、结果匹配等），或通过 LLM API 辅助生成奖励函数代码
  - 配置测试场景：用户输入、预期工具调用、预期输出

#### Scenario: 配置 Agentic RL 训练
- **WHEN** 用户完成环境编辑后进入训练配置
- **THEN** 显示训练参数配置（基础权重选择、算法选择 GRPO/CISPO、训练轮次等）

#### Scenario: 执行 Agentic RL 训练
- **WHEN** 用户启动 Agentic RL 训练
- **THEN** 后端执行 train_agent.py，使用用户配置的环境和奖励函数，前端显示训练进度和 agent 行为日志

### Requirement: 后端 Linux 实例
系统 SHALL 在用户手机上运行一个 Linux 实例，承载所有训练和推理服务。

#### Scenario: 初始化后端
- **WHEN** 应用首次启动或用户触发重新初始化
- **THEN** 系统在手机上启动 Linux 实例（通过 PRoot/Termux 方案），自动安装 Python 环境、PyTorch 及项目依赖，部署 API 服务

#### Scenario: 后端 API 服务
- **WHEN** 后端初始化完成
- **THEN** 提供 REST API 用于模型管理、训练配置和启动；提供 WebSocket 用于推理流式输出和训练进度推送

### Requirement: 开箱即用
系统 SHALL 实现开箱即用体验，用户安装 App 后无需额外配置即可使用。

#### Scenario: 首次使用
- **WHEN** 用户首次安装并打开应用
- **THEN** 系统自动完成所有初始化（Linux 实例、Python 环境、预置模型权重下载），用户可直接开始推理或训练

#### Scenario: 预置资源
- **WHEN** 后端初始化完成
- **THEN** 系统预置：训练好的 Tokenizer、可选的预训练权重下载、示例数据集

### Requirement: 模型与数据管理
系统 SHALL 提供模型权重和数据集的管理功能。

#### Scenario: 模型管理
- **WHEN** 用户查看模型列表
- **THEN** 显示所有本地模型权重（来源：下载/训练产出），支持加载、删除、导出

#### Scenario: 数据集管理
- **WHEN** 用户查看数据集列表
- **THEN** 显示所有可用数据集（预置+用户上传），支持上传自定义 jsonl 数据集、预览数据内容

### Requirement: 训练监控
系统 SHALL 提供训练过程的实时监控。

#### Scenario: 查看训练进度
- **WHEN** 训练正在执行
- **THEN** 前端实时显示：当前 epoch/step、loss 值、loss 曲线图、训练日志、预计剩余时间

#### Scenario: 训练控制
- **WHEN** 训练正在执行
- **THEN** 用户可暂停/恢复/停止训练，训练断点自动保存，支持从断点恢复

### Requirement: LLM API 辅助配置
系统 SHALL 允许用户配置外部 LLM API，用于辅助生成复杂配置。

#### Scenario: 配置 LLM API
- **WHEN** 用户在设置中配置 LLM API（API 地址、Key、模型名）
- **THEN** 系统保存配置，可在知识蒸馏和 Agentic RL 奖励函数生成中使用

#### Scenario: AI 辅助生成奖励函数
- **WHEN** 用户在 Agentic RL 环境编辑器中点击"AI 生成奖励函数"
- **THEN** 系统将当前工具定义和场景描述发送给配置的 LLM API，返回奖励函数代码建议，用户确认后应用

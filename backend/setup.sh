#!/bin/bash
set -e

echo "===== MiniMind Backend 初始化脚本 ====="

echo "[1/6] 检查 Python 版本..."
PYTHON_CMD=""
if command -v python3 &> /dev/null; then
    PYTHON_CMD=python3
elif command -v python &> /dev/null; then
    PYTHON_CMD=python
else
    echo "未找到 Python，正在安装 Python 3.10..."
    apt-get update
    apt-get install -y python3.10 python3.10-venv python3-pip
    PYTHON_CMD=python3.10
fi

PYTHON_VERSION=$($PYTHON_CMD -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')")
echo "Python 版本: $PYTHON_VERSION"

echo "[2/6] 安装 PyTorch (CPU 版本)..."
$PYTHON_CMD -m pip install --upgrade pip
$PYTHON_CMD -m pip install torch==2.6.0 --index-url https://download.pytorch.org/whl/cpu --quiet

echo "[3/6] 安装项目依赖..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
$PYTHON_CMD -m pip install -r "$SCRIPT_DIR/requirements.txt" --quiet

echo "[4/6] 检查 Tokenizer 文件..."
MINIMIND_ROOT="$(dirname "$SCRIPT_DIR")"
TOKENIZER_DIR="$MINIMIND_ROOT/model"
if [ ! -f "$TOKENIZER_DIR/tokenizer.json" ]; then
    echo "警告: 未找到 tokenizer.json，请确保 MiniMind 项目已正确部署"
else
    echo "Tokenizer 文件已就绪: $TOKENIZER_DIR"
fi

echo "[5/6] 检查模型权重..."
OUT_DIR="$MINIMIND_ROOT/out"
mkdir -p "$OUT_DIR"
if [ -z "$(ls -A "$OUT_DIR"/*.pth 2>/dev/null)" ]; then
    echo "警告: 未找到模型权重文件，请将权重文件放置于 $OUT_DIR 目录"
else
    echo "已找到模型权重: $OUT_DIR"
    ls -lh "$OUT_DIR"/*.pth 2>/dev/null || true
fi

echo "[6/6] 启动 FastAPI 服务..."
cd "$SCRIPT_DIR"
$PYTHON_CMD main.py

echo "===== 初始化完成 ====="

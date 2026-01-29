#!/bin/bash

echo "==================================="
echo "Sentra 前端应用启动脚本"
echo "==================================="
echo ""

# 检查是否选择了桌面模式
if [ "$1" == "desktop" ]; then
    echo "🖥️  启动 Tauri 桌面开发模式..."
    echo ""
    echo "⚠️  注意：首次运行需要先构建 Next.js"
    echo ""
    npm run tauri dev
else
    echo "🌐 启动 Web 开发模式..."
    echo ""
    echo "✅ 推荐：使用此模式进行日常开发"
    echo ""
    npm run dev
fi

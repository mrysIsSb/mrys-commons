#!/bin/bash

echo "启动Swagger MCP服务端..."
echo

# 检查Java版本
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java运行环境，请确保已安装Java 21或更高版本"
    exit 1
fi

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven，请确保已安装Maven"
    exit 1
fi

# 编译项目
echo "正在编译项目..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

echo "编译完成，启动服务..."
echo
echo "提示: 使用 Ctrl+C 停止服务"
echo

# 启动MCP服务端
mvn exec:java -Dexec.mainClass="top.mrys.swagger.mcp.SwaggerMcpServer" -q

echo
echo "服务已停止"
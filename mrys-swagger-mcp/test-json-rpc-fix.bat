@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ========================================
echo 测试 JSON-RPC 协议修复
echo ========================================
echo.

echo 1. 检查 JAR 文件是否存在...
if not exist "target\mrys-swagger-mcp-0.0.1-jar-with-dependencies.jar" (
    echo 错误: JAR 文件不存在，请先编译项目
    echo 运行: mvn clean package
    pause
    exit /b 1
)

echo 2. 启动 HTTP 服务器（后台运行）...
start /b java -jar "target\mrys-swagger-mcp-0.0.1-jar-with-dependencies.jar" 8080

echo 等待服务器启动...
timeout /t 3 /nobreak > nul

echo 3. 测试健康检查...
curl -s -X POST http://localhost:8080/health
echo.
echo.

echo 4. 测试 MCP 初始化请求（包含 ID）...
echo {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}}}} | curl -s -X POST -H "Content-Type: application/json" -d @- http://localhost:8080/mcp
echo.
echo.

echo 5. 测试工具列表请求（包含 ID）...
echo {"jsonrpc":"2.0","id":2,"method":"tools/list"} | curl -s -X POST -H "Content-Type: application/json" -d @- http://localhost:8080/mcp
echo.
echo.

echo 6. 测试无效方法请求（包含 ID）...
echo {"jsonrpc":"2.0","id":3,"method":"invalid_method"} | curl -s -X POST -H "Content-Type: application/json" -d @- http://localhost:8080/mcp
echo.
echo.

echo 7. 测试无 ID 的请求（应该自动添加 ID）...
echo {"jsonrpc":"2.0","method":"tools/list"} | curl -s -X POST -H "Content-Type: application/json" -d @- http://localhost:8080/mcp
echo.
echo.

echo ========================================
echo 测试完成！
echo 请检查上述响应是否都包含正确的 'id' 字段
echo ========================================
echo.
echo 按任意键停止服务器并退出...
pause > nul

echo 停止服务器...
taskkill /f /im java.exe > nul 2>&1

echo 测试结束。
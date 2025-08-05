@echo off
echo 测试Swagger MCP HTTP模式...
echo.

REM 检查curl是否可用
curl --version >nul 2>&1
if errorlevel 1 (
    echo 警告: 未找到curl命令，将跳过HTTP测试
    echo 请手动访问 http://localhost:8080 来测试HTTP服务器
    pause
    exit /b 0
)

REM 设置服务器URL
set SERVER_URL=http://localhost:8080
if not "%1"=="" set SERVER_URL=%1

echo 测试服务器: %SERVER_URL%
echo.

echo 1. 测试服务器根页面...
curl -s %SERVER_URL% >nul
if errorlevel 1 (
    echo ❌ 服务器根页面访问失败
    echo 请确保HTTP服务器已启动：.\start-http-server.bat
    pause
    exit /b 1
) else (
    echo ✅ 服务器根页面访问成功
)

echo.
echo 2. 测试健康检查接口...
curl -s %SERVER_URL%/health
if errorlevel 1 (
    echo ❌ 健康检查接口访问失败
) else (
    echo ✅ 健康检查接口访问成功
)

echo.
echo.
echo 3. 测试MCP初始化...
curl -s -X POST %SERVER_URL%/mcp ^
  -H "Content-Type: application/json" ^
  -d "{
    \"jsonrpc\": \"2.0\",
    \"id\": 1,
    \"method\": \"initialize\",
    \"params\": {
      \"protocolVersion\": \"2024-11-05\",
      \"capabilities\": {},
      \"clientInfo\": {
        \"name\": \"test-client\",
        \"version\": \"1.0.0\"
      }
    }
  }"
if errorlevel 1 (
    echo ❌ MCP初始化失败
) else (
    echo ✅ MCP初始化成功
)

echo.
echo.
echo 4. 测试工具列表...
curl -s -X POST %SERVER_URL%/mcp ^
  -H "Content-Type: application/json" ^
  -d "{
    \"jsonrpc\": \"2.0\",
    \"id\": 2,
    \"method\": \"tools/list\"
  }"
if errorlevel 1 (
    echo ❌ 获取工具列表失败
) else (
    echo ✅ 获取工具列表成功
)

echo.
echo.
echo 5. 测试列出Swagger源...
curl -s -X POST %SERVER_URL%/mcp ^
  -H "Content-Type: application/json" ^
  -d "{
    \"jsonrpc\": \"2.0\",
    \"id\": 3,
    \"method\": \"tools/call\",
    \"params\": {
      \"name\": \"list_swagger_sources\",
      \"arguments\": {}
    }
  }"
if errorlevel 1 (
    echo ❌ 列出Swagger源失败
) else (
    echo ✅ 列出Swagger源成功
)

echo.
echo.
echo 🎉 HTTP模式测试完成！
echo.
echo 💡 提示：
echo - 访问 %SERVER_URL% 查看服务器信息页面
echo - 访问 %SERVER_URL%/health 查看健康状态
echo - 使用 .\start-mcp-client.bat 启动MCP客户端
echo.
pause
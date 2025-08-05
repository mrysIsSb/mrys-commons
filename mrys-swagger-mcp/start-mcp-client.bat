@echo off
echo 启动Swagger MCP客户端...
echo.

REM 检查Java版本
java -version 2>nul
if errorlevel 1 (
    echo 错误: 未找到Java运行环境，请确保已安装Java 21或更高版本
    pause
    exit /b 1
)

REM 检查JAR文件
if not exist "target\mrys-swagger-mcp-0.0.1-jar-with-dependencies.jar" (
    echo 错误: 未找到可执行的JAR文件，请先运行 build.bat 构建项目
    pause
    exit /b 1
)

REM 设置服务器URL（默认localhost:8080）
set SERVER_URL=http://localhost:8080
if not "%1"=="" set SERVER_URL=%1

echo 连接到MCP HTTP服务器: %SERVER_URL%
echo.
echo 提示: 使用 Ctrl+C 停止客户端
echo 说明: 此客户端将转发标准输入输出的MCP协议到HTTP服务器
echo.

REM 运行MCP客户端
java -cp "target\mrys-swagger-mcp-0.0.1-jar-with-dependencies.jar" top.mrys.swagger.mcp.SwaggerMcpClient %SERVER_URL%

echo.
echo MCP客户端已停止
pause
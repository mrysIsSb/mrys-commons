@echo off
echo 启动Swagger MCP服务端...
echo.

REM 检查Java版本
java -version 2>nul
if errorlevel 1 (
    echo 错误: 未找到Java运行环境，请确保已安装Java 21或更高版本
    pause
    exit /b 1
)

REM 编译项目
echo 正在编译项目...
mvn clean compile -q
if errorlevel 1 (
    echo 错误: 项目编译失败
    pause
    exit /b 1
)

echo 编译完成，启动服务...
echo.
echo 提示: 使用 Ctrl+C 停止服务
echo.

REM 启动MCP服务端
mvn exec:java -Dexec.mainClass="top.mrys.swagger.mcp.SwaggerMcpServer" -q

echo.
echo 服务已停止
pause
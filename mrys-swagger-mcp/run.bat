@echo off
echo 运行Swagger MCP服务端...
echo.

REM 检查Java版本
java -version 2>nul
if errorlevel 1 (
    echo 错误: 未找到Java运行环境，请确保已安装Java 21或更高版本
    pause
    exit /b 1
)

REM 检查编译文件
if not exist "target\classes\top\mrys\swagger\mcp\SwaggerMcpServer.class" (
    echo 错误: 未找到编译后的类文件，请先运行 build.bat 构建项目
    pause
    exit /b 1
)

echo 启动服务...
echo.
echo 提示: 使用 Ctrl+C 停止服务
echo 配置: 可通过环境变量 SWAGGER_SOURCES 或配置文件设置Swagger源
echo.

REM 设置默认环境变量（如果未设置）
if "%SWAGGER_SOURCES%"=="" (
    set SWAGGER_SOURCES=default=http://localhost:8080/v3/api-docs,petstore=https://petstore3.swagger.io/api/v3/openapi.json
)

REM 运行MCP服务端
java -cp "target\classes;target\dependency\*" top.mrys.swagger.mcp.SwaggerMcpServer

echo.
echo 服务已停止
pause
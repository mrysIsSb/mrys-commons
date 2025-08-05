@echo off
echo 启动Swagger MCP HTTP服务端...
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

REM 设置端口（默认8080）
set HTTP_PORT=8080
if not "%1"=="" set HTTP_PORT=%1

echo 启动HTTP服务器，端口: %HTTP_PORT%
echo.
echo 提示: 使用 Ctrl+C 停止服务
echo 配置: 可通过环境变量 SWAGGER_SOURCES 或配置文件设置Swagger源
echo 访问地址: http://localhost:%HTTP_PORT%
echo MCP接口: http://localhost:%HTTP_PORT%/mcp
echo 健康检查: http://localhost:%HTTP_PORT%/health
echo.

REM 设置默认环境变量（如果未设置）
if "%SWAGGER_SOURCES%"=="" (
    set SWAGGER_SOURCES=default=http://localhost:8080/v3/api-docs,petstore=https://petstore3.swagger.io/api/v3/openapi.json
)

REM 运行MCP HTTP服务端
java -jar "target\mrys-swagger-mcp-0.0.1-jar-with-dependencies.jar" %HTTP_PORT%

echo.
echo HTTP服务器已停止
pause
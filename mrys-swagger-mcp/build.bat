@echo off
echo 构建Swagger MCP服务端...
echo.

REM 检查Java版本
java -version 2>nul
if errorlevel 1 (
    echo 错误: 未找到Java运行环境，请确保已安装Java 21或更高版本
    pause
    exit /b 1
)

REM 创建目录
if not exist "target\classes" mkdir "target\classes"
if not exist "target\dependency" mkdir "target\dependency"

echo 正在下载依赖...
echo 注意: 需要手动下载以下JAR文件到 target\dependency\ 目录:
echo - jackson-databind-2.15.2.jar
echo - jackson-core-2.15.2.jar
echo - jackson-annotations-2.15.2.jar
echo - httpclient5-5.2.1.jar
echo - httpcore5-5.2.1.jar
echo - slf4j-api-2.0.7.jar
echo - logback-classic-1.4.8.jar
echo - logback-core-1.4.8.jar
echo - lombok-1.18.28.jar
echo.
echo 或者使用Maven命令: mvn dependency:copy-dependencies
echo.

REM 编译Java源码
echo 正在编译Java源码...
javac -cp "target\dependency\*" -d target\classes -sourcepath src\main\java src\main\java\top\mrys\swagger\mcp\*.java src\main\java\top\mrys\swagger\mcp\config\*.java src\main\java\top\mrys\swagger\mcp\service\*.java

if errorlevel 1 (
    echo 错误: 编译失败
    pause
    exit /b 1
)

REM 复制资源文件
if exist "src\main\resources" (
    echo 复制资源文件...
    xcopy /s /y "src\main\resources\*" "target\classes\"
)

echo 构建完成！
echo.
echo 运行服务: java -cp ".\target\classes;.\target\dependency\*" top.mrys.swagger.mcp.SwaggerMcpServerecho.
pause

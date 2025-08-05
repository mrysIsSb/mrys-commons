# Swagger MCP HTTP模式使用指南

本项目现在支持两种运行模式：

## 1. 标准MCP模式（原有模式）

使用标准输入输出进行通信，符合MCP协议规范。

### 启动方式
```bash
# 使用脚本启动
.\run.bat

# 或直接使用Java命令
java -cp "target\classes;target\dependency\*" top.mrys.swagger.mcp.SwaggerMcpServer
```

### 特点
- 符合MCP协议标准
- 通过标准输入输出通信
- 适合与支持MCP协议的AI工具集成

## 2. HTTP模式（新增模式）

通过HTTP REST API提供MCP功能，便于调试和集成。

### 启动HTTP服务器
```bash
# 使用脚本启动（默认端口8080）
.\start-http-server.bat

# 指定端口启动
.\start-http-server.bat 9090

# 或直接使用Java命令
java -jar "target\mrys-swagger-mcp-0.0.1-jar-with-dependencies.jar" 8080
```

### 启动MCP客户端
```bash
# 使用脚本启动（连接到localhost:8080）
.\start-mcp-client.bat

# 连接到指定服务器
.\start-mcp-client.bat http://localhost:9090

# 或直接使用Java命令
java -cp "target\mrys-swagger-mcp-0.0.1-jar-with-dependencies.jar" top.mrys.swagger.mcp.SwaggerMcpClient http://localhost:8080
```

### HTTP模式特点
- **易于调试**: 可以通过浏览器或HTTP工具直接访问
- **可扩展性**: 支持多个客户端同时连接
- **监控友好**: 提供健康检查和状态接口
- **开发便利**: 便于开发和测试

## HTTP接口说明

### 主要接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/` | GET | 服务器信息页面 |
| `/mcp` | POST | MCP协议接口 |
| `/health` | GET | 健康检查接口 |

### 使用示例

#### 1. 访问服务器信息
```bash
curl http://localhost:8080/
```

#### 2. 健康检查
```bash
curl http://localhost:8080/health
```

#### 3. MCP协议调用
```bash
# 初始化
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": {
        "name": "test-client",
        "version": "1.0.0"
      }
    }
  }'

# 获取工具列表
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list"
  }'

# 调用工具
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "list_swagger_sources",
      "arguments": {}
    }
  }'
```

## 架构说明

### HTTP模式架构
```
AI工具 <---> MCP客户端 <---> HTTP服务器 <---> Swagger文档服务
         (标准MCP协议)    (HTTP REST API)
```

### 组件说明

1. **SwaggerMcpHttpServer**: HTTP服务器，提供REST API接口
2. **SwaggerMcpClient**: MCP客户端，转发标准MCP协议到HTTP服务器
3. **SwaggerMcpServer**: 原有的标准MCP服务器

## 选择建议

### 使用标准MCP模式的场景
- 与支持MCP协议的AI工具集成
- 需要严格遵循MCP协议规范
- 资源使用要求较低

### 使用HTTP模式的场景
- 开发和调试阶段
- 需要支持多个客户端
- 需要监控和管理功能
- 与不支持MCP协议的系统集成
- 需要通过网络访问服务

## 配置说明

两种模式都支持相同的配置方式：

1. **环境变量**: `SWAGGER_SOURCES`
2. **配置文件**: `src/main/resources/swagger-sources.properties`

配置格式：
```
default=http://localhost:8080/v3/api-docs
petstore=https://petstore3.swagger.io/api/v3/openapi.json
actuator=http://localhost:8080/actuator/openapi
```

## 构建和部署

```bash
# 构建项目
mvn clean package

# 生成的JAR文件
target/mrys-swagger-mcp-0.0.1-jar-with-dependencies.jar
```

这个JAR文件包含了所有依赖，可以直接运行HTTP服务器或客户端。
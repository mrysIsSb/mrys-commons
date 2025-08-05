# Swagger MCP Server

一个简单的MCP（Model Context Protocol）服务端，用于提供Swagger/OpenAPI文档。支持从远程URL或本地文件获取文档。

## 功能特性

- 🌐 支持远程URL获取Swagger文档
- 📁 支持本地文件读取Swagger文档
- ⚡ 内置文档缓存机制（5分钟过期）
- 🔧 灵活的配置管理
- 📊 文档解析和API信息提取
- 🛠️ 简单易用的MCP工具接口
- 📝 支持Markdown格式输出

## 快速开始

### 1. 编译项目

```bash
mvn clean package
```

### 2. 运行服务

本项目支持两种运行模式：

#### 标准MCP模式（推荐用于生产）
```bash
# 使用脚本
.\run.bat

# 或直接运行
java -cp "target\classes;target\dependency\*" top.mrys.swagger.mcp.SwaggerMcpServer
```

#### HTTP模式（推荐用于开发和调试）
```bash
# 启动HTTP服务器
.\start-http-server.bat

# 在另一个终端启动MCP客户端
.\start-mcp-client.bat

# 或直接运行
java -jar "target\mrys-swagger-mcp-0.0.1-jar-with-dependencies.jar" 8080
```

> 📖 详细的HTTP模式使用说明请参考 [HTTP-MODE.md](HTTP-MODE.md)

### 3. 配置Swagger源

#### 方式一：配置文件

在 `src/main/resources/swagger-sources.properties` 中配置：

```properties
# 本地服务
default=http://localhost:8080/v3/api-docs

# 远程API
petstore=https://petstore3.swagger.io/api/v3/openapi.json

# 本地文件
local-api=./api-docs.json
```

#### 方式二：环境变量

```bash
export SWAGGER_SOURCES="default=http://localhost:8080/v3/api-docs,petstore=https://petstore3.swagger.io/api/v3/openapi.json"
```

#### 方式三：用户配置文件

在用户目录下创建 `~/.mrys/swagger-sources.properties`：

```properties
my-api=http://my-server:8080/v3/api-docs
```

## MCP工具

### 1. get_swagger_doc

获取Swagger文档内容。

**参数：**
- `source`: Swagger文档源名称或直接的URL/文件路径

**返回：**
- Swagger文档的完整内容

**示例：**
```json
{
  "name": "get_swagger_doc",
  "arguments": {
    "source": "petstore"
  }
}
```

### 2. list_swagger_sources

列出所有配置的Swagger文档源。

**示例：**
```json
{
  "name": "list_swagger_sources",
  "arguments": {}
}
```

### 3. parse_swagger_doc

解析Swagger文档并提取API信息统计，以Markdown格式返回。

**参数：**
- `source`: Swagger文档源名称或直接的URL/文件路径

**返回：** API文档的结构化信息（Markdown格式），包括：
- API标题、版本、描述
- 基础URL信息
- 路径统计
- HTTP方法统计
- Schema数量

**示例：**
```json
{
  "name": "parse_swagger_doc",
  "arguments": {
    "source": "petstore"
  }
}
```

### 4. clear_cache

清除文档缓存。

**示例：**
```json
{
  "name": "clear_cache",
  "arguments": {}
}
```

## 配置优先级

配置加载的优先级（后加载的会覆盖先加载的）：

1. classpath中的 `swagger-sources.properties`
2. 用户目录下的 `~/.mrys/swagger-sources.properties`
3. 环境变量 `SWAGGER_SOURCES`
4. 默认配置

## 支持的文档格式

- OpenAPI 3.x (JSON)
- Swagger 2.0 (JSON)
- 本地JSON文件
- 远程JSON API

## 缓存机制

- 文档缓存时间：5分钟
- 自动清理过期缓存
- 支持手动清除缓存

## 错误处理

- 网络请求超时处理
- 文件不存在处理
- JSON格式验证
- 详细的错误信息返回

## 日志配置

使用SLF4J + Logback进行日志记录，可以通过 `logback.xml` 配置日志级别和输出格式。

## 依赖要求

- Java 21+
- Maven 3.6+
- Jackson（JSON处理）
- Apache HttpClient 5（HTTP请求）

## 许可证

MIT License
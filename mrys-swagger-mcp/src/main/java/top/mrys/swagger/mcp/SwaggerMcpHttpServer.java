package top.mrys.swagger.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import top.mrys.swagger.mcp.config.SwaggerSourceConfig;
import top.mrys.swagger.mcp.service.SwaggerDocumentService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * HTTP版本的Swagger文档MCP服务端
 * 提供REST API接口来处理MCP请求
 * 
 * @author mrys
 * @since 0.0.1
 */
@Slf4j
public class SwaggerMcpHttpServer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SwaggerSourceConfig config = new SwaggerSourceConfig();
    private final SwaggerDocumentService documentService;
    private HttpServer httpServer;
    private final int port;

    public SwaggerMcpHttpServer() {
        this(8080);
    }

    public SwaggerMcpHttpServer(int port) {
        this.port = port;
        this.documentService = new SwaggerDocumentService(config);
    }

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                log.warn("无效的端口号: {}, 使用默认端口 8080", args[0]);
            }
        }
        
        SwaggerMcpHttpServer server = new SwaggerMcpHttpServer(port);
        server.start();
    }

    /**
     * 启动HTTP MCP服务端
     */
    public void start() {
        try {
            log.info("启动Swagger MCP HTTP服务端，端口: {}", port);

            // 加载配置
            config.loadConfiguration();

            // 创建HTTP服务器
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);


            // 设置处理器
            httpServer.createContext("/mcp", new McpHandler());
            httpServer.createContext("/health", new HealthHandler());
            httpServer.createContext("/", new RootHandler());
            
            // 设置虚拟线程池（Java 19+）
            httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            
            // 启动服务器
            httpServer.start();
            
            log.info("Swagger MCP HTTP服务端已启动，访问地址: http://localhost:{}", port);
            log.info("MCP接口地址: http://localhost:{}/mcp", port);
            log.info("健康检查地址: http://localhost:{}/health", port);
            
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
            
        } catch (IOException e) {
            log.error("启动HTTP服务器失败", e);
            throw new RuntimeException("启动HTTP服务器失败", e);
        }
    }

    /**
     * 停止HTTP服务端
     */
    public void stop() {
        if (httpServer != null) {
            log.info("正在停止Swagger MCP HTTP服务端...");
            httpServer.stop(5);
            log.info("Swagger MCP HTTP服务端已停止");
        }
    }

    /**
     * MCP请求处理器
     */
    private class McpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            log.info("处理MCP请求: {}", exchange.getRequestURI());
            // 设置CORS头
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, 405, "只支持POST请求");
                return;
            }

            try {
                // 读取请求体
                String requestBody = readRequestBody(exchange.getRequestBody());
                log.debug("收到MCP请求: {}", requestBody);

                // 解析JSON请求
                JsonNode request = objectMapper.readTree(requestBody);
                
                // 处理MCP请求
                JsonNode response = handleMcpRequest(request);
                
                // 发送响应
                String responseBody = objectMapper.writeValueAsString(response);
                log.debug("发送MCP响应: {}", responseBody);
                
                sendJsonResponse(exchange, 200, responseBody);
                
            } catch (Exception e) {
                log.error("处理MCP请求时发生错误", e);
                JsonNode errorResponse = createErrorResponse(null, "处理请求时发生错误: " + e.getMessage());
                try {
                    String responseBody = objectMapper.writeValueAsString(errorResponse);
                    sendJsonResponse(exchange, 500, responseBody);
                } catch (Exception ex) {
                    log.error("发送错误响应失败", ex);
                    sendErrorResponse(exchange, 500, "内部服务器错误");
                }
            }
        }
    }

    /**
     * 健康检查处理器
     */
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            log.info("处理健康检查请求: {}", exchange.getRequestURI());
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            health.put("service", "swagger-mcp-server");
            health.put("version", "1.0.0");
            health.put("sources", config.getSources().size());
            
            try {
                String responseBody = objectMapper.writeValueAsString(health);
                sendJsonResponse(exchange, 200, responseBody);
            } catch (Exception e) {
                log.error("生成健康检查响应失败", e);
                sendErrorResponse(exchange, 500, "健康检查失败");
            }
        }
    }

    /**
     * 根路径处理器
     */
    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            log.info("处理根路径请求: {}", exchange.getRequestURI());
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Swagger MCP HTTP Server</title>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        .container { max-width: 800px; margin: 0 auto; }
                        .endpoint { background: #f5f5f5; padding: 10px; margin: 10px 0; border-radius: 5px; }
                        .method { font-weight: bold; color: #007acc; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Swagger MCP HTTP Server</h1>
                        <p>欢迎使用Swagger文档MCP HTTP服务端</p>
                        
                        <h2>可用接口</h2>
                        <div class="endpoint">
                            <span class="method">POST</span> /mcp - MCP协议接口
                        </div>
                        <div class="endpoint">
                            <span class="method">GET</span> /health - 健康检查接口
                        </div>
                        
                        <h2>配置的Swagger源</h2>
                        <ul>
                """;
            
            for (Map.Entry<String, String> entry : config.getSources().entrySet()) {
                html += "<li><strong>" + entry.getKey() + ":</strong> " + entry.getValue() + "</li>";
            }
            
            html += """
                        </ul>
                        
                        <h2>使用说明</h2>
                        <p>这是一个HTTP版本的MCP服务器，可以通过HTTP POST请求到 /mcp 接口来调用MCP功能。</p>
                        <p>支持的MCP方法：initialize, tools/list, tools/call</p>
                    </div>
                </body>
                </html>
                """;
            
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            byte[] response = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    /**
     * 处理MCP请求（复用原有逻辑）
     */
    private JsonNode handleMcpRequest(JsonNode request) {
        String method = request.path("method").asText();

        return switch (method) {
            case "initialize" -> handleInitialize(request);
            case "tools/list" -> handleToolsList(request);
            case "tools/call" -> handleToolsCall(request);
            default -> createErrorResponse(request, "未知的方法: " + method);
        };
    }

    /**
     * 处理初始化请求
     */
    private JsonNode handleInitialize(JsonNode request) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", Map.of(
                "tools", Map.of()));
        result.put("serverInfo", Map.of(
                "name", "swagger-mcp-http-server",
                "version", "1.0.0"));

        return objectMapper.valueToTree(Map.of(
                "jsonrpc", "2.0",
                "id", request.path("id"),
                "result", result));
    }

    /**
     * 处理工具列表请求
     */
    private JsonNode handleToolsList(JsonNode request) {
        List<Map<String, Object>> tools = new ArrayList<>();

        // 获取Swagger文档工具
        tools.add(Map.of(
                "name", "get_swagger_doc",
                "description", "获取Swagger API文档",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "source", Map.of(
                                        "type", "string",
                                        "description", "Swagger文档源名称或URL/文件路径")),
                        "required", List.of("source"))));

        // 列出配置的源
        tools.add(Map.of(
                "name", "list_swagger_sources",
                "description", "列出所有配置的Swagger文档源",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of())));

        // 解析Swagger文档
        tools.add(Map.of(
                "name", "parse_swagger_doc",
                "description", "解析Swagger文档并提取API信息",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "source", Map.of(
                                        "type", "string",
                                        "description", "Swagger文档源名称或URL/文件路径")),
                        "required", List.of("source"))));

        // 清除缓存
        tools.add(Map.of(
                "name", "clear_cache",
                "description", "清除Swagger文档缓存",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of())));

        return objectMapper.valueToTree(Map.of(
                "jsonrpc", "2.0",
                "id", request.path("id"),
                "result", Map.of("tools", tools)));
    }

    /**
     * 处理工具调用请求
     */
    private JsonNode handleToolsCall(JsonNode request) {
        JsonNode params = request.path("params");
        String toolName = params.path("name").asText();
        JsonNode arguments = params.path("arguments");

        return switch (toolName) {
            case "get_swagger_doc" -> handleGetSwaggerDoc(request, arguments);
            case "list_swagger_sources" -> handleListSwaggerSources(request);
            case "parse_swagger_doc" -> handleParseSwaggerDoc(request, arguments);
            case "clear_cache" -> handleClearCache(request);
            default -> createErrorResponse(request, "未知的工具: " + toolName);
        };
    }

    /**
     * 处理获取Swagger文档请求
     */
    private JsonNode handleGetSwaggerDoc(JsonNode request, JsonNode arguments) {
        String source = arguments.path("source").asText();
        
        try {
            String swaggerDoc = documentService.getSwaggerDocument(source);
            
            return objectMapper.valueToTree(Map.of(
                "jsonrpc", "2.0",
                "id", request.path("id"),
                "result", Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", swaggerDoc
                    ))
                )
            ));
        } catch (Exception e) {
            log.error("获取Swagger文档失败: {}", source, e);
            return createErrorResponse(request, "获取Swagger文档失败: " + e.getMessage());
        }
    }

    /**
     * 处理列出Swagger源请求
     */
    private JsonNode handleListSwaggerSources(JsonNode request) {
        StringBuilder sources = new StringBuilder("配置的Swagger文档源:\n");
        for (Map.Entry<String, String> entry : config.getSources().entrySet()) {
            sources.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return objectMapper.valueToTree(Map.of(
                "jsonrpc", "2.0",
                "id", request.path("id"),
                "result", Map.of(
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", sources.toString())))));
    }

    /**
     * 处理解析Swagger文档请求
     */
    private JsonNode handleParseSwaggerDoc(JsonNode request, JsonNode arguments) {
        String source = arguments.path("source").asText();
        
        try {
            String swaggerDoc = documentService.getSwaggerDocument(source);
            Map<String, Object> apiInfo = documentService.parseSwaggerDocument(swaggerDoc);
            
            StringBuilder result = new StringBuilder();
            result.append("# API文档信息\n\n");
            result.append("**标题:** ").append(apiInfo.get("title")).append("\n");
            result.append("**版本:** ").append(apiInfo.get("version")).append("\n");
            result.append("**描述:** ").append(apiInfo.get("description")).append("\n");
            result.append("**基础URL:** ").append(apiInfo.get("baseUrl")).append("\n");
            result.append("**总路径数:** ").append(apiInfo.get("totalPaths")).append("\n");
            result.append("**Schema数量:** ").append(apiInfo.get("schemaCount")).append("\n\n");
            result.append("## HTTP方法统计\n\n");
            
            @SuppressWarnings("unchecked")
            Map<String, Integer> methodCounts = (Map<String, Integer>) apiInfo.get("methodCounts");
            for (Map.Entry<String, Integer> entry : methodCounts.entrySet()) {
                result.append("- **").append(entry.getKey()).append(":** ").append(entry.getValue()).append("\n");
            }
            
            return objectMapper.valueToTree(Map.of(
                "jsonrpc", "2.0",
                "id", request.path("id"),
                "result", Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", result.toString()
                    ))
                )
            ));
        } catch (Exception e) {
            log.error("解析Swagger文档失败: {}", source, e);
            return createErrorResponse(request, "解析Swagger文档失败: " + e.getMessage());
        }
    }

    /**
     * 处理清除缓存请求
     */
    private JsonNode handleClearCache(JsonNode request) {
        try {
            documentService.clearCache();

            return objectMapper.valueToTree(Map.of(
                    "jsonrpc", "2.0",
                    "id", request.path("id"),
                    "result", Map.of(
                            "content", List.of(Map.of(
                                    "type", "text",
                                    "text", "缓存已清除")))));
        } catch (Exception e) {
            log.error("清除缓存失败", e);
            return createErrorResponse(request, "清除缓存失败: " + e.getMessage());
        }
    }

    /**
     * 创建错误响应
     */
    private JsonNode createErrorResponse(JsonNode request, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        if (request != null && request.has("id")) {
            response.put("id", request.path("id"));
        }
        response.put("error", Map.of(
            "code", -1,
            "message", message
        ));
        return objectMapper.valueToTree(response);
    }

    /**
     * 读取请求体
     */
    private String readRequestBody(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 发送JSON响应
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> error = Map.of(
            "error", message,
            "timestamp", System.currentTimeMillis()
        );
        
        try {
            String responseBody = objectMapper.writeValueAsString(error);
            sendJsonResponse(exchange, statusCode, responseBody);
        } catch (Exception e) {
            // 如果JSON序列化失败，发送纯文本错误
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            byte[] response = message.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }
}
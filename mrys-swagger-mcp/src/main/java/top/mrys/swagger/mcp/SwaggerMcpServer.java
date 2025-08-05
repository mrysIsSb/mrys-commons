package top.mrys.swagger.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import top.mrys.swagger.mcp.config.SwaggerSourceConfig;
import top.mrys.swagger.mcp.service.SwaggerDocumentService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简单的Swagger文档MCP服务端
 * 支持通过远程URL或本地文件路径提供Swagger文档
 * 
 * @author mrys
 * @since 0.0.1
 */
@Slf4j
public class SwaggerMcpServer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SwaggerSourceConfig config = new SwaggerSourceConfig();
    private final SwaggerDocumentService documentService;

    public SwaggerMcpServer() {
        this.documentService = new SwaggerDocumentService(config);
    }

    public static void main(String[] args) {
        SwaggerMcpServer server = new SwaggerMcpServer();
        server.start();
    }

    /**
     * 启动MCP服务端
     */
    public void start() {
        log.info("启动Swagger MCP服务端...");

        // 加载配置
        config.loadConfiguration();

        // 启动标准输入输出处理循环
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    JsonNode request = objectMapper.readTree(line);
                    JsonNode response = handleRequest(request);
                    System.out.println(objectMapper.writeValueAsString(response));
                } catch (Exception e) {
                    log.error("处理请求时发生错误", e);
                    JsonNode errorResponse = createErrorResponse("处理请求时发生错误: " + e.getMessage());
                    try {
                        System.out.println(objectMapper.writeValueAsString(errorResponse));
                    } catch (Exception ex) {
                        log.error("发送错误响应失败", ex);
                    }
                }
            }
        }
    }

    /**
     * 处理MCP请求
     */
    private JsonNode handleRequest(JsonNode request) {
        String method = request.path("method").asText();

        return switch (method) {
            case "initialize" -> handleInitialize(request);
            case "tools/list" -> handleToolsList();
            case "tools/call" -> handleToolsCall(request);
            default -> createErrorResponse("未知的方法: " + method);
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
                "name", "swagger-mcp-server",
                "version", "1.0.0"));

        return objectMapper.valueToTree(Map.of(
                "jsonrpc", "2.0",
                "id", request.path("id"),
                "result", result));
    }

    /**
     * 处理工具列表请求
     */
    private JsonNode handleToolsList() {
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
            default -> createErrorResponse("未知的工具: " + toolName);
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
            return createErrorResponse("获取Swagger文档失败: " + e.getMessage());
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
            return createErrorResponse("解析Swagger文档失败: " + e.getMessage());
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
            return createErrorResponse("清除缓存失败: " + e.getMessage());
        }
    }


    
    /**
     * 创建错误响应
     */
    private JsonNode createErrorResponse(String message) {
        return objectMapper.valueToTree(Map.of(
            "jsonrpc", "2.0",
            "error", Map.of(
                "code", -1,
                "message", message
            )
        ));
    }
}
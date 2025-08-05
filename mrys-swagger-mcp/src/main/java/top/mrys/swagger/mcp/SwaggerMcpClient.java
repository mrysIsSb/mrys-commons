package top.mrys.swagger.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP客户端，用于转发AI和MCP HTTP服务器之间的通信
 * 实现标准的MCP协议，通过HTTP与服务器通信
 * 
 * @author mrys
 * @since 0.0.1
 */
@Slf4j
public class SwaggerMcpClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient;
    private final String serverUrl;
    private final AtomicLong requestIdCounter = new AtomicLong(1);
    private boolean initialized = false;

    public SwaggerMcpClient(String serverUrl) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl + "mcp" : serverUrl + "/mcp";
        this.httpClient = HttpClients.createDefault();
    }

    public static void main(String[] args) {
        String serverUrl = "http://localhost:8080";
        if (args.length > 0) {
            serverUrl = args[0];
        }
        
        SwaggerMcpClient client = new SwaggerMcpClient(serverUrl);
        client.start();
    }

    /**
     * 启动MCP客户端
     */
    public void start() {
        log.info("启动Swagger MCP客户端，连接到服务器: {}", serverUrl);

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

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
                    JsonNode errorResponse = createErrorResponse(null, "处理请求时发生错误: " + e.getMessage());
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
     * 停止MCP客户端
     */
    public void stop() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
            log.info("Swagger MCP客户端已停止");
        } catch (IOException e) {
            log.error("关闭HTTP客户端时发生错误", e);
        }
    }

    /**
     * 处理MCP请求
     */
    private JsonNode handleRequest(JsonNode request) {
        String method = request.path("method").asText();
        
        // 对于initialize请求，我们需要先初始化与服务器的连接
        if ("initialize".equals(method)) {
            return handleInitialize(request);
        }
        
        // 其他请求直接转发到HTTP服务器
        return forwardToHttpServer(request);
    }

    /**
     * 处理初始化请求
     */
    private JsonNode handleInitialize(JsonNode request) {
        try {
            // 转发初始化请求到HTTP服务器
            JsonNode response = forwardToHttpServer(request);
            
            // 检查响应是否成功
            if (response.has("result")) {
                initialized = true;
                log.info("与MCP HTTP服务器初始化成功");
            } else {
                log.warn("MCP HTTP服务器初始化失败: {}", response);
            }
            
            return response;
        } catch (Exception e) {
            log.error("初始化MCP HTTP服务器连接失败", e);
            return createErrorResponse(request, "初始化失败: " + e.getMessage());
        }
    }

    /**
     * 转发请求到HTTP服务器
     */
    private JsonNode forwardToHttpServer(JsonNode request) {
        try {
            // 确保请求有ID
            JsonNode requestWithId = ensureRequestId(request);
            
            // 创建HTTP POST请求
            HttpPost httpPost = new HttpPost(serverUrl);
            httpPost.setHeader("Content-Type", "application/json");
            
            // 设置请求体
            String requestBody = objectMapper.writeValueAsString(requestWithId);
            httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
            
            log.debug("发送请求到HTTP服务器: {}", requestBody);
            
            // 执行请求
            return httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                
                if (statusCode == 200) {
                    // 读取响应体
                    try (InputStream content = response.getEntity().getContent()) {
                        String responseBody = new String(content.readAllBytes(), StandardCharsets.UTF_8);
                        log.debug("收到HTTP服务器响应: {}", responseBody);
                        return objectMapper.readTree(responseBody);
                    }
                } else {
                    // HTTP错误
                    String errorMessage = "HTTP错误: " + statusCode;
                    try (InputStream content = response.getEntity().getContent()) {
                        String responseBody = new String(content.readAllBytes(), StandardCharsets.UTF_8);
                        errorMessage += ", 响应: " + responseBody;
                    } catch (Exception e) {
                        // 忽略读取错误响应体的异常
                    }
                    log.error(errorMessage);
                    return createErrorResponse(requestWithId, errorMessage);
                }
            });
            
        } catch (Exception e) {
            log.error("转发请求到HTTP服务器失败", e);
            return createErrorResponse(request, "转发请求失败: " + e.getMessage());
        }
    }

    /**
     * 确保请求有ID
     */
    private JsonNode ensureRequestId(JsonNode request) {
        if (request.has("id")) {
            return request;
        }
        
        // 如果请求没有ID，添加一个
        try {
            var requestMap = objectMapper.convertValue(request, java.util.Map.class);
            requestMap.put("id", requestIdCounter.getAndIncrement());
            return objectMapper.valueToTree(requestMap);
        } catch (Exception e) {
            log.warn("添加请求ID失败", e);
            return request;
        }
    }

    /**
     * 创建错误响应
     */
    private JsonNode createErrorResponse(JsonNode request, String message) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("jsonrpc", "2.0");
        if (request != null && request.has("id")) {
            response.put("id", request.path("id"));
        }
        response.put("error", java.util.Map.of(
            "code", -1,
            "message", message
        ));
        return objectMapper.valueToTree(response);
    }

    /**
     * 检查服务器健康状态
     */
    public boolean checkServerHealth() {
        try {
            String healthUrl = serverUrl.replace("/mcp", "/health");
            HttpPost httpPost = new HttpPost(healthUrl);
            
            return httpClient.execute(httpPost, response -> {
                return response.getCode() == 200;
            });
        } catch (Exception e) {
            log.error("检查服务器健康状态失败", e);
            return false;
        }
    }

    /**
     * 获取服务器信息
     */
    public String getServerInfo() {
        try {
            String healthUrl = serverUrl.replace("/mcp", "/health");
            HttpPost httpPost = new HttpPost(healthUrl);
            
            return httpClient.execute(httpPost, response -> {
                if (response.getCode() == 200) {
                    try (InputStream content = response.getEntity().getContent()) {
                        return new String(content.readAllBytes(), StandardCharsets.UTF_8);
                    }
                } else {
                    return "服务器不可用";
                }
            });
        } catch (Exception e) {
            log.error("获取服务器信息失败", e);
            return "获取服务器信息失败: " + e.getMessage();
        }
    }
}
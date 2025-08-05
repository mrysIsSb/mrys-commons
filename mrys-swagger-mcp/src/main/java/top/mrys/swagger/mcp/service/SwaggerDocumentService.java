package top.mrys.swagger.mcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import top.mrys.swagger.mcp.config.SwaggerSourceConfig;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Swagger文档服务类
 * 负责从各种源获取和缓存Swagger文档
 */
@Slf4j
public class SwaggerDocumentService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient;
    private final SwaggerSourceConfig config;
    private final Map<String, CachedDocument> documentCache = new ConcurrentHashMap<>();
    
    // 缓存过期时间（5分钟）
    private static final long CACHE_EXPIRY_MS = Duration.ofMinutes(5).toMillis();
    
    public SwaggerDocumentService(SwaggerSourceConfig config) {
        this.config = config;
        this.httpClient = HttpClients.custom()
            .setConnectionManager(org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(10)
                .setMaxConnPerRoute(5)
                .build())
            .build();
    }
    
    /**
     * 获取Swagger文档
     */
    public String getSwaggerDocument(String source) throws Exception {
        // 检查缓存
        CachedDocument cached = documentCache.get(source);
        if (cached != null && !cached.isExpired()) {
            log.debug("从缓存获取文档: {}", source);
            return cached.getContent();
        }
        
        // 获取实际源地址
        String actualSource = resolveSource(source);
        
        String document;
        if (isRemoteUrl(actualSource)) {
            document = fetchRemoteDocument(actualSource);
        } else {
            document = readLocalDocument(actualSource);
        }
        
        // 验证文档格式
        validateSwaggerDocument(document);
        
        // 缓存文档
        documentCache.put(source, new CachedDocument(document, System.currentTimeMillis()));
        
        log.info("成功获取Swagger文档: {}", source);
        return document;
    }
    
    /**
     * 解析Swagger文档并提取API信息
     */
    public Map<String, Object> parseSwaggerDocument(String document) throws Exception {
        JsonNode root = objectMapper.readTree(document);
        Map<String, Object> result = new HashMap<>();
        
        // 基本信息
        JsonNode info = root.path("info");
        result.put("title", info.path("title").asText("未知"));
        result.put("version", info.path("version").asText("未知"));
        result.put("description", info.path("description").asText(""));
        
        // 服务器信息
        JsonNode servers = root.path("servers");
        if (servers.isArray() && servers.size() > 0) {
            result.put("baseUrl", servers.get(0).path("url").asText(""));
        }
        
        // API路径统计
        JsonNode paths = root.path("paths");
        Map<String, Integer> methodCounts = new HashMap<>();
        int totalPaths = 0;
        
        if (paths.isObject()) {
            paths.fields().forEachRemaining(pathEntry -> {
                JsonNode pathNode = pathEntry.getValue();
                pathNode.fields().forEachRemaining(methodEntry -> {
                    String method = methodEntry.getKey().toUpperCase();
                    if (!method.equals("PARAMETERS") && !method.equals("SUMMARY") && !method.equals("DESCRIPTION")) {
                        methodCounts.merge(method, 1, Integer::sum);
                    }
                });
            });
            totalPaths = paths.size();
        }
        
        result.put("totalPaths", totalPaths);
        result.put("methodCounts", methodCounts);
        
        // 组件信息
        JsonNode components = root.path("components");
        if (components.isObject()) {
            JsonNode schemas = components.path("schemas");
            if (schemas.isObject()) {
                result.put("schemaCount", schemas.size());
            }
        }
        
        return result;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        documentCache.clear();
        log.info("已清除文档缓存");
    }
    
    /**
     * 清除过期缓存
     */
    public void clearExpiredCache() {
        long now = System.currentTimeMillis();
        documentCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }
    
    /**
     * 解析源地址
     */
    private String resolveSource(String source) {
        // 如果是配置的源名称，则获取实际地址
        if (config.hasSource(source)) {
            return config.getSourceUrl(source);
        }
        // 否则直接使用源地址
        return source;
    }
    
    /**
     * 判断是否为远程URL
     */
    private boolean isRemoteUrl(String source) {
        return source.startsWith("http://") || source.startsWith("https://");
    }
    
    /**
     * 从远程URL获取文档
     */
    private String fetchRemoteDocument(String url) throws Exception {
        log.debug("从远程URL获取文档: {}", url);
        
        HttpGet request = new HttpGet(url);
        request.setHeader("Accept", "application/json");
        request.setHeader("User-Agent", "Swagger-MCP-Server/1.0");
        
        return httpClient.execute(request, response -> {
            int statusCode = response.getCode();
            if (statusCode != 200) {
                throw new RuntimeException(String.format("HTTP请求失败，状态码: %d, URL: %s", statusCode, url));
            }
            
            String content = EntityUtils.toString(response.getEntity());
            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("获取到空的文档内容");
            }
            
            return content;
        });
    }
    
    /**
     * 从本地文件读取文档
     */
    private String readLocalDocument(String filePath) throws Exception {
        log.debug("从本地文件读取文档: {}", filePath);
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        
        if (!Files.isReadable(path)) {
            throw new RuntimeException("文件不可读: " + filePath);
        }
        
        String content = Files.readString(path);
        if (content.trim().isEmpty()) {
            throw new RuntimeException("文件内容为空: " + filePath);
        }
        
        return content;
    }
    
    /**
     * 验证Swagger文档格式
     */
    private void validateSwaggerDocument(String document) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(document);
            
            // 检查是否为有效的OpenAPI/Swagger文档
            boolean isOpenAPI = root.has("openapi");
            boolean isSwagger = root.has("swagger");
            
            if (!isOpenAPI && !isSwagger) {
                throw new RuntimeException("不是有效的OpenAPI/Swagger文档");
            }
            
            // 检查必要字段
            if (!root.has("info")) {
                throw new RuntimeException("缺少info字段");
            }
            
            if (!root.has("paths")) {
                throw new RuntimeException("缺少paths字段");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("文档格式验证失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 缓存文档类
     */
    private static class CachedDocument {
        private final String content;
        private final long timestamp;
        
        public CachedDocument(String content, long timestamp) {
            this.content = content;
            this.timestamp = timestamp;
        }
        
        public String getContent() {
            return content;
        }
        
        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }
        
        public boolean isExpired(long now) {
            return (now - timestamp) > CACHE_EXPIRY_MS;
        }
    }
}
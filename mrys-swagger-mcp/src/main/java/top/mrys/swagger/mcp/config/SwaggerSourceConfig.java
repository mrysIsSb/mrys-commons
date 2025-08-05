package top.mrys.swagger.mcp.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Swagger源配置管理类
 * 支持从配置文件、环境变量等方式加载配置
 */
@Data
@Slf4j
public class SwaggerSourceConfig {
    
    private Map<String, String> sources = new HashMap<>();
    
    /**
     * 加载配置
     */
    public void loadConfiguration() {
        // 1. 从classpath加载配置文件
        loadFromClasspath();
        
        // 2. 从用户目录加载配置文件
        loadFromUserHome();
        
        // 3. 从环境变量加载配置
        loadFromEnvironment();
        
        // 4. 设置默认配置
        setDefaultConfiguration();
        
        log.info("已加载Swagger源配置: {}", sources);
    }
    
    /**
     * 从classpath加载配置文件
     */
    private void loadFromClasspath() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("swagger-sources.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                props.forEach((key, value) -> sources.put(key.toString(), value.toString()));
                log.debug("从classpath加载配置: {}", props.size());
            }
        } catch (IOException e) {
            log.debug("无法从classpath加载配置文件: {}", e.getMessage());
        }
    }
    
    /**
     * 从用户目录加载配置文件
     */
    private void loadFromUserHome() {
        Path configPath = Paths.get(System.getProperty("user.home"), ".mrys", "swagger-sources.properties");
        if (Files.exists(configPath)) {
            try {
                Properties props = new Properties();
                props.load(Files.newInputStream(configPath));
                props.forEach((key, value) -> sources.put(key.toString(), value.toString()));
                log.debug("从用户目录加载配置: {}", props.size());
            } catch (IOException e) {
                log.warn("无法从用户目录加载配置文件: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 从环境变量加载配置
     */
    private void loadFromEnvironment() {
        String swaggerSources = System.getenv("SWAGGER_SOURCES");
        if (swaggerSources != null && !swaggerSources.trim().isEmpty()) {
            String[] sourceArray = swaggerSources.split(",");
            for (String source : sourceArray) {
                String[] parts = source.split("=", 2);
                if (parts.length == 2) {
                    sources.put(parts[0].trim(), parts[1].trim());
                }
            }
            log.debug("从环境变量加载配置: {}", sourceArray.length);
        }
    }
    
    /**
     * 设置默认配置
     */
    private void setDefaultConfiguration() {
        if (sources.isEmpty()) {
            sources.put("default", "http://localhost:8080/v3/api-docs");
            sources.put("petstore", "https://petstore3.swagger.io/api/v3/openapi.json");
            log.debug("设置默认配置");
        }
    }
    
    /**
     * 获取源地址
     */
    public String getSourceUrl(String sourceName) {
        return sources.get(sourceName);
    }
    
    /**
     * 添加源
     */
    public void addSource(String name, String url) {
        sources.put(name, url);
    }
    
    /**
     * 移除源
     */
    public void removeSource(String name) {
        sources.remove(name);
    }
    
    /**
     * 检查源是否存在
     */
    public boolean hasSource(String name) {
        return sources.containsKey(name);
    }
}
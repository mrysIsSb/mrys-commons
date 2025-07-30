package top.mrys.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * mrys-auth-token 示例应用程序
 * 演示如何使用 mrys-auth-token 进行权限验证和用户认证
 * 
 * 新版本特性：
 * - 基于拦截器的认证机制（不再使用 AOP）
 * - 支持 SpEL 表达式权限控制
 * - 自动配置，开箱即用
 * - 轻量级依赖策略
 * 
 * @author mrys
 */
@SpringBootApplication
public class ExampleAuthTokenApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleAuthTokenApplication.class, args);
        System.out.println("\n=== mrys-auth-token 示例应用启动成功 ===");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("API 文档: 查看 UserController 和 AdminController");
        System.out.println("========================================\n");
    }
}
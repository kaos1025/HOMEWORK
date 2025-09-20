package kr.co._29cm.homework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("29CM 상품 주문 API")
                        .description("29CM 백엔드 포지션 과제 - 상품 조회 및 주문 API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("29CM Backend Team")
                                .email("backend@29cm.co.kr"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://api.29cm.co.kr")
                                .description("프로덕션 서버")
                ));
    }
}

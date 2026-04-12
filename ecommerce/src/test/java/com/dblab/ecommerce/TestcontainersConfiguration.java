package com.dblab.ecommerce;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers 설정
 * - 운영 환경과 동일한 postgres:17-alpine 이미지 사용
 * - init.sql을 통해 운영 DDL과 동일한 스키마를 Testcontainer에 적용
 * - public: 다른 패키지 테스트에서 @Import로 참조 가능
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    @SuppressWarnings({"rawtypes", "resource"})
    PostgreSQLContainer postgresContainer() {
        // raw type 사용: testcontainers 버전별 제네릭 호환성 처리
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                .withInitScript("init.sql"); // src/test/resources/init.sql
    }
}

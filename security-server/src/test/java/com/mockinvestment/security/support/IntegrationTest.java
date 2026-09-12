package com.mockinvestment.security.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * 스프링 컨텍스트가 필요한 통합 테스트 공통 베이스.
 * - MySQL 8.4 컨테이너를 JVM 당 1개만 띄워 모든 통합 테스트가 공유한다 (싱글턴 컨테이너 패턴).
 *   @Container 를 쓰면 테스트 클래스마다 컨테이너를 새로 띄워 느려지므로 static 블록에서 직접 start.
 *   종료는 Testcontainers 의 Ryuk 이 JVM 종료 시 정리한다.
 * - @ServiceConnection: 컨테이너 접속 정보를 spring.datasource.* 에 자동 주입 (application.yml 의 ${DB_*} 를 덮어씀)
 * - 시크릿은 테스트 전용 더미 값. 형식과 길이는 실제와 동일 (AES 32바이트, JWT 32바이트 이상)
 */
@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItbW9hLXNlY3VyaXR5LXNlcnZlci10ZXN0cw==",
        "aes.key=dGVzdC1hZXMtMjU2LWdjbS1rZXktMzItYnl0ZXMhISE="
})
public abstract class IntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName("moa_security");

    static {
        MYSQL.start();
    }
}

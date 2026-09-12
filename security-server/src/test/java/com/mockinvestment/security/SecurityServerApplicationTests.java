package com.mockinvestment.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 스프링 컨텍스트 기동 테스트.
 * 로컬 MySQL/환경변수에 의존하지 않도록 Testcontainers MySQL을 띄우고
 * {@link ServiceConnection}으로 datasource 설정을 자동 주입한다.
 * (application.yml의 ${DB_USERNAME} 등은 컨테이너 값으로 덮어써진다)
 * 기동 시 Flyway V1 마이그레이션 + JPA ddl-auto=validate 까지 함께 검증된다.
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
		// 시크릿은 테스트 전용 더미 값. 형식만 실제와 동일 (Base64, 각각 48/32바이트)
		"jwt.secret=dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItbW9hLXNlY3VyaXR5LXNlcnZlci10ZXN0cw==",
		"aes.key=dGVzdC1hZXMtMjU2LWdjbS1rZXktMzJieXRlcyE="
})
class SecurityServerApplicationTests {

	@Container
	@ServiceConnection
	static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0")
			.withDatabaseName("moa_security");

	@Test
	void contextLoads() {
	}

}

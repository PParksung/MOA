package com.mockinvestment.security;

import com.mockinvestment.security.support.IntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * 컨텍스트 기동 테스트. 기동 과정에서 Flyway 마이그레이션 + JPA ddl-auto=validate(Entity ↔ 스키마 일치)까지 검증된다.
 */
class SecurityServerApplicationTests extends IntegrationTest {

	@Test
	void contextLoads() {
	}

}

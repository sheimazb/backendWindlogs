package com.windlogs.tickets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "spring.cloud.config.import-check.enabled=false"
})
@ActiveProfiles("test")
class TicketsApplicationTests {

	@Test
	void contextLoads() {
	}

}

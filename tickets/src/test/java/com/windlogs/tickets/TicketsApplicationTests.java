package com.windlogs.tickets;

import com.windlogs.tickets.config.TestExclusionConfig;
import com.windlogs.tickets.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
    },
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = TestExclusionConfig.class
)
@ActiveProfiles("test")
class TicketsApplicationTests {

	// Mock all repositories to prevent Spring from trying to create actual repository beans
	@MockBean
	private CommentRepository commentRepository;
	
	@MockBean
	private SolutionRepository solutionRepository;
	
	@MockBean
	private TicketRepository ticketRepository;

	@Test
	void contextLoads() {
	}

}

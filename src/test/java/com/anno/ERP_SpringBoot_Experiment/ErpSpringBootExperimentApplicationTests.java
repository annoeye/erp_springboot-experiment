package com.anno.ERP_SpringBoot_Experiment;

import com.anno.ERP_SpringBoot_Experiment.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ErpSpringBootExperimentApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}

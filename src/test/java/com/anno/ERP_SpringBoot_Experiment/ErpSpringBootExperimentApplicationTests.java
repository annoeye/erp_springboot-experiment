package com.anno.ERP_SpringBoot_Experiment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.anno.ERP_SpringBoot_Experiment.config.TestElasticsearchConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest
@ActiveProfiles("test")
class ErpSpringBootExperimentApplicationTests {

	@MockBean
	private com.anno.ERP_SpringBoot_Experiment.event.listener.ElasticsearchSyncListener elasticsearchSyncListener;

	@MockBean
	private org.springframework.data.elasticsearch.core.ElasticsearchOperations elasticsearchOperations;

	@MockBean
	private com.anno.ERP_SpringBoot_Experiment.service.EmailService emailService;

	@MockBean
	private com.anno.ERP_SpringBoot_Experiment.service.MinioService minioService;

	@Test
	void contextLoads() {
	}

}

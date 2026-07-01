package com.anno.ERP_SpringBoot_Experiment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ErpSpringBootExperimentApplicationTests {

	@MockBean
	private com.anno.ERP_SpringBoot_Experiment.repository.search.ProductSearchRepository productSearchRepository;

	@MockBean
	private com.anno.ERP_SpringBoot_Experiment.event.listener.ElasticsearchSyncListener elasticsearchSyncListener;

	@MockBean
	private com.anno.ERP_SpringBoot_Experiment.service.EmailService emailService;

	@MockBean
	private com.anno.ERP_SpringBoot_Experiment.service.search.ProductElasticSearchService productElasticSearchService;

	@MockBean
	private com.anno.ERP_SpringBoot_Experiment.service.MinioService minioService;

	@Test
	void contextLoads() {
	}

}

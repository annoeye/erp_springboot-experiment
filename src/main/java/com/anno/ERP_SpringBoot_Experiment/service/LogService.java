package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.dto.LogDto;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Log;
import com.anno.ERP_SpringBoot_Experiment.repository.RoleRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.LogRepository;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogService implements iLog {
    private final LogRepository logRepository;

    @Override
    public void log(LogDto body) {
        Log log = new Log();
        log.setName(String.valueOf(body.getStatus()));
        log.setDescription(body.getDescription());
        log.setTargetId(body.getTargetId());
        log.setPerformedBy(body.getPerformedBy());
        logRepository.save(log);
    }
}

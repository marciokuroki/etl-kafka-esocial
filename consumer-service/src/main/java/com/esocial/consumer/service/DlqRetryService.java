package com.esocial.consumer.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.esocial.consumer.model.entity.DlqEvent;
import com.esocial.consumer.repository.DlqEventRepository;

@Service
public class DlqRetryService {

    @Autowired
    private DlqEventRepository dlqEventRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final int maxRetries = 5;

    @Scheduled(fixedDelayString = "${dlq.retry.delay:60000}")
    public void processDlqEvents() {
        List<DlqEvent> toRetry = dlqEventRepository.findByStatusAndRetryCountLessThan("PENDING", maxRetries);

        for (DlqEvent dlqEvent : toRetry) {
            try {
                kafkaTemplate.send("employee.events", dlqEvent.getEventPayload()).get(); // síncrono para confiabilidade
                dlqEvent.setStatus("RETRIED");
                dlqEventRepository.save(dlqEvent);
            } catch (Exception e) {
                dlqEvent.setRetryCount(dlqEvent.getRetryCount() + 1);
                if (dlqEvent.getRetryCount() >= maxRetries) {
                    dlqEvent.setStatus("FAILED");
                }
                dlqEventRepository.save(dlqEvent);
            }
        }
    }
}

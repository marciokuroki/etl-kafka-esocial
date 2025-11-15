package com.esocial.consumer.service;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.esocial.consumer.model.entity.DlqEvent;
import com.esocial.consumer.repository.DlqEventRepository;

public class DlqRetryServiceUnitTest {

    @Mock
    private DlqEventRepository dlqEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private DlqRetryService dlqRetryService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testProcessDlqEventsSuccess() throws Exception {
        DlqEvent event = new DlqEvent();
        event.setId(1L);
        event.setEventPayload("payload");
        event.setStatus("PENDING");
        event.setRetryCount(0);

        when(dlqEventRepository.findByStatusAndRetryCountLessThan("PENDING", 5))
            .thenReturn(List.of(event));

        SendResult<String, String> sendResult = mock(SendResult.class);

        when(kafkaTemplate.send("employee.events", "payload").get())
            .thenReturn(sendResult);

        dlqRetryService.processDlqEvents();

        verify(dlqEventRepository, times(1)).save(argThat(savedEvent -> savedEvent.getStatus().equals("RETRIED")));
    }

    @Test
    public void testProcessDlqEventsFailureMaxRetry() throws Exception {
        DlqEvent event = new DlqEvent();
        event.setId(1L);
        event.setEventPayload("payload");
        event.setStatus("PENDING");
        event.setRetryCount(4);

        when(dlqEventRepository.findByStatusAndRetryCountLessThan("PENDING", 5))
            .thenReturn(List.of(event));

        when(kafkaTemplate.send("employee.events", "payload").get())
            .thenThrow(new ExecutionException(new RuntimeException("Kafka error")));

        dlqRetryService.processDlqEvents();

        verify(dlqEventRepository, times(1)).save(argThat(savedEvent -> 
            savedEvent.getStatus().equals("FAILED") && savedEvent.getRetryCount() == 5
        ));
    }
}

package com.esocial.consumer.integration;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.support.Acknowledgment;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class TestHelper {

    /**
     * Cria Headers Kafka simulados com correlationId personalizado
     */
    public static Headers createHeadersWithCorrelationId(String correlationId) {
        List<Header> headerList = Arrays.asList(
            new RecordHeader("X-Correlation-Id", correlationId.getBytes(StandardCharsets.UTF_8))
        );
        return new RecordHeaders(headerList);
    }

    /**
     * Cria Acknowledgment stub para testes
     */
    public static Acknowledgment createAcknowledgmentStub() {
        return new Acknowledgment() {
            @Override
            public void acknowledge() {
                // noop para testes
            }
        };
    }
}

package com.spearhead.ufc.config;

import com.spearhead.ufc.jms.ReportRequest;
import jakarta.jms.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

/**
 * JMS configuration: message converter (JSON) + listener container factory.
 * Uses the auto-configured embedded Artemis ConnectionFactory.
 */
@Configuration
public class JmsConfig {

    public static final String REPORT_QUEUE = "report.generation.queue";

    /**
     * JSON-based message converter.  Uses the Spring Boot auto-configured ObjectMapper
     * (which includes JavaTimeModule for LocalDate serialization).
     */
    @Bean
    public MessageConverter jacksonJmsMessageConverter(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setTypeIdMappings(Map.of("ReportRequest", ReportRequest.class));
        converter.setObjectMapper(objectMapper);
        return converter;
    }

    /** JmsTemplate pre-wired with the JSON converter for use by the producer. */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory,
                                   MessageConverter jacksonJmsMessageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(jacksonJmsMessageConverter);
        return template;
    }

    /** Listener container factory pre-wired with the JSON converter. */
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jacksonJmsMessageConverter) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonJmsMessageConverter);
        factory.setConcurrency("2-5");
        return factory;
    }
}

package com.spearhead.ufc.jms;

import com.spearhead.ufc.config.JmsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Sends a {@link ReportRequest} to the report generation JMS queue and
 * immediately marks the request as PENDING in the status store.
 */
@Component
public class ReportQueueProducer {

    private static final Logger log = LoggerFactory.getLogger(ReportQueueProducer.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private ReportStatusStore reportStatusStore;

    public void enqueue(ReportRequest request) {
        log.info("Enqueuing report request - requestId={}, reportType={}", request.getRequestId(), request.getReportType());
        reportStatusStore.setPending(request.getRequestId());
        jmsTemplate.convertAndSend(JmsConfig.REPORT_QUEUE, request);
        log.info("Report request enqueued - requestId={}", request.getRequestId());
    }
}

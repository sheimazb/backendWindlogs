package com.windlogs.tickets.kafka;

import com.windlogs.tickets.entity.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketProducer  {

    private final KafkaTemplate<String, TicketP> kafkaTemplate;

    public void sendTicketP(TicketP ticketP) {
        log.info("sending ticket of log");
        Message<TicketP> message =MessageBuilder
                .withPayload(ticketP)
                .setHeader(KafkaHeaders.TOPIC,"ticket-topic")
                .build();
        kafkaTemplate.send(message);
    }
}

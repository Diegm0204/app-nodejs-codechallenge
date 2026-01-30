package com.yape.challenge.transaction.infrastructure.kafka;

import com.yape.challenge.shared.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {
    
    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;
    
    @Value("${yape.kafka.topics.transaction-created}")
    private String transactionCreatedTopic;
    
    
    public void sendTransactionCreatedEvent(TransactionCreatedEvent event) {
        log.info("Enviando evento de transacción creada para: {}", event.getTransactionExternalId());
        
        CompletableFuture<SendResult<String, TransactionCreatedEvent>> future = 
            kafkaTemplate.send(transactionCreatedTopic, event.getTransactionExternalId().toString(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Evento de transacción creada enviado exitosamente", 
                    event.getTransactionExternalId(), 
                    result.getRecordMetadata().partition());
            } else {
                log.error("Error al enviar evento de transacción creada", event.getTransactionExternalId(), ex);
            }
        });
    }
}

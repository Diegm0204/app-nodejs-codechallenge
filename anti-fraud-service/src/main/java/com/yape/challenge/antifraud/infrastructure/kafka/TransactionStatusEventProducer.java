package com.yape.challenge.antifraud.infrastructure.kafka;

import com.yape.challenge.shared.event.TransactionStatusUpdatedEvent;
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
public class TransactionStatusEventProducer {
    
    private final KafkaTemplate<String, TransactionStatusUpdatedEvent> kafkaTemplate;
    
    @Value("${yape.kafka.topics.transaction-status-updated}")
    private String transactionStatusUpdatedTopic;
    
   
    public void sendTransactionStatusUpdatedEvent(TransactionStatusUpdatedEvent event) {
        log.info("Enviando evento de actualización de estado para: {} con estado: {}", 
            event.getTransactionExternalId(), event.getStatus());
        
        CompletableFuture<SendResult<String, TransactionStatusUpdatedEvent>> future = 
            kafkaTemplate.send(transactionStatusUpdatedTopic, event.getTransactionExternalId().toString(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Evento de actualización de estado enviado exitosamente: {} a partición: {}", 
                    event.getTransactionExternalId(), 
                    result.getRecordMetadata().partition());
            } else {
                log.error("Error al enviar evento de actualización de estado: {}", event.getTransactionExternalId(), ex);
                // En producción, implementar lógica de reintentos o cola de mensajes muertos
            }
        });
    }
}

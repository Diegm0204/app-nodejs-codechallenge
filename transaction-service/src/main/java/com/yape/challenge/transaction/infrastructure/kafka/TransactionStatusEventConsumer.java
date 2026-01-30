package com.yape.challenge.transaction.infrastructure.kafka;

import com.yape.challenge.shared.event.TransactionStatusUpdatedEvent;
import com.yape.challenge.transaction.application.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionStatusEventConsumer {
    
    private final TransactionService transactionService;
    
    
    @KafkaListener(
        topics = "${yape.kafka.topics.transaction-status-updated}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionStatusUpdatedEvent(
        @Payload TransactionStatusUpdatedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Recibido evento de actualización de estado: {} con estado: {} desde partición: {}, offset: {}",
            event.getTransactionExternalId(), event.getStatus(), partition, offset);
        
        try {
            // Actualizar estado de transacción
            String statusName = event.getStatus().name().toLowerCase();
            transactionService.updateTransactionStatus(event.getTransactionExternalId(), statusName);
            
            log.info("Estado de transacción actualizado exitosamente: {}", event.getTransactionExternalId());
        } catch (Exception ex) {
            log.error("Error procesando evento de actualización de estado: {}", event.getTransactionExternalId(), ex);
            // En producción, implementar estrategia de manejo de errores (reintentos, DLQ, etc.)
            throw ex; // Re-lanzar para activar mecanismo de reintento de Kafka
        }
    }
}

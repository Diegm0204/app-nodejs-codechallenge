package com.yape.challenge.antifraud.infrastructure.kafka;

import com.yape.challenge.antifraud.domain.service.FraudDetectionService;
import com.yape.challenge.shared.event.TransactionCreatedEvent;
import com.yape.challenge.shared.event.TransactionStatusUpdatedEvent;
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
public class TransactionCreatedEventConsumer {
    
    private final FraudDetectionService fraudDetectionService;
    private final TransactionStatusEventProducer statusEventProducer;
    
   
    @KafkaListener(
        topics = "${yape.kafka.topics.transaction-created}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionCreatedEvent(
        @Payload TransactionCreatedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Recibido evento de transacción creada: {} con monto: {} desde partición: {}, offset: {}",
            event.getTransactionExternalId(), event.getValue(), partition, offset);
        
        try {
            // Validar transacción usando servicio de detección de fraude
            TransactionStatusUpdatedEvent statusEvent = fraudDetectionService.validateTransaction(event);
            
            // Enviar evento de actualización de estado de vuelta al Transaction Service
            statusEventProducer.sendTransactionStatusUpdatedEvent(statusEvent);
            
            log.info("Validación de transacción completada: {} -> {}", 
                event.getTransactionExternalId(), statusEvent.getStatus());
        } catch (Exception ex) {
            log.error("Error procesando evento de transacción creada: {}", event.getTransactionExternalId(), ex);
            // En producción, implementar estrategia de manejo de errores (reintentos, DLQ, etc.)
            throw ex; // Re-lanzar para activar mecanismo de reintento de Kafka
        }
    }
}

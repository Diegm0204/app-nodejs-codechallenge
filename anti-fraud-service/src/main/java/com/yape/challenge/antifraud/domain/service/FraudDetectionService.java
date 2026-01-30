package com.yape.challenge.antifraud.domain.service;

import com.yape.challenge.shared.event.TransactionCreatedEvent;
import com.yape.challenge.shared.event.TransactionStatusUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


@Service
@Slf4j
public class FraudDetectionService {
    
    @Value("${yape.fraud.max-transaction-value}")
    private BigDecimal maxTransactionValue;
    
    @Value("${yape.fraud.processing-delay-ms:0}")
    private long processingDelayMs;
    
   
    public TransactionStatusUpdatedEvent validateTransaction(TransactionCreatedEvent event) {
        log.info("Validando transacción con monto", 
            event.getTransactionExternalId(), event.getValue());
        
        // Simular tiempo de procesamiento 
        if (processingDelayMs > 0) {
            try {
                Thread.sleep(processingDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Procesamiento interrumpido", e);
            }
        }
        
        // Aplicar reglas de deteccion de fraude
        TransactionStatusUpdatedEvent.TransactionStatus status;
        String reason;
        
        if (event.getValue().compareTo(maxTransactionValue) > 0) {
            status = TransactionStatusUpdatedEvent.TransactionStatus.REJECTED;
            reason = String.format("Monto de transacción %.2f excede el máximo permitido %.2f", 
                event.getValue(), maxTransactionValue);
            log.warn("Transacción RECHAZADA", event.getTransactionExternalId(), reason);
        } else {
            status = TransactionStatusUpdatedEvent.TransactionStatus.APPROVED;
            reason = "Transacción aprobó todas las validaciones de fraude";
            log.info("Transacción APROBADA", event.getTransactionExternalId());
        }
        
        return TransactionStatusUpdatedEvent.builder()
            .transactionExternalId(event.getTransactionExternalId())
            .status(status)
            .reason(reason)
            .eventId(UUID.randomUUID().toString())
            .eventTimestamp(OffsetDateTime.now())
            .build();
    }
    
    
}

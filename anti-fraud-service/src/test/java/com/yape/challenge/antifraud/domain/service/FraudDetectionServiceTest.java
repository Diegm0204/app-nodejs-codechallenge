package com.yape.challenge.antifraud.domain.service;

import com.yape.challenge.shared.event.TransactionCreatedEvent;
import com.yape.challenge.shared.event.TransactionStatusUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas de Fraud Detection Service")
class FraudDetectionServiceTest {
    
    private FraudDetectionService fraudDetectionService;
    
    @BeforeEach
    void setUp() {
        fraudDetectionService = new FraudDetectionService();
        ReflectionTestUtils.setField(fraudDetectionService, "maxTransactionValue", BigDecimal.valueOf(1000));
        ReflectionTestUtils.setField(fraudDetectionService, "processingDelayMs", 0L);
    }
    
    @Test
    @DisplayName("Debe aprobar transacción con monto por debajo del límite")
    void shouldApproveTransactionBelowThreshold() {
        // Given
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
            .transactionExternalId(UUID.randomUUID())
            .accountExternalIdDebit(UUID.randomUUID())
            .accountExternalIdCredit(UUID.randomUUID())
            .transferTypeId(1)
            .value(BigDecimal.valueOf(500))
            .createdAt(OffsetDateTime.now())
            .build();
        
        TransactionStatusUpdatedEvent result = fraudDetectionService.validateTransaction(event);
        
        assertThat(result).isNotNull();
        assertThat(result.getTransactionExternalId()).isEqualTo(event.getTransactionExternalId());
        assertThat(result.getStatus()).isEqualTo(TransactionStatusUpdatedEvent.TransactionStatus.APPROVED);
        assertThat(result.getReason()).contains("aprobó todas las validaciones");
    }
    
    @Test
    @DisplayName("Debe aprobar transacción con monto exacto en el límite")
    void shouldApproveTransactionAtThreshold() {
        
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
            .transactionExternalId(UUID.randomUUID())
            .value(BigDecimal.valueOf(1000))
            .build();
        
        
        TransactionStatusUpdatedEvent result = fraudDetectionService.validateTransaction(event);
        
        
        assertThat(result.getStatus()).isEqualTo(TransactionStatusUpdatedEvent.TransactionStatus.APPROVED);
    }
    
    @Test
    @DisplayName("Debe rechazar transacción con monto por encima del límite")
    void shouldRejectTransactionAboveThreshold() {
        
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
            .transactionExternalId(UUID.randomUUID())
            .accountExternalIdDebit(UUID.randomUUID())
            .accountExternalIdCredit(UUID.randomUUID())
            .transferTypeId(1)
            .value(BigDecimal.valueOf(1500))
            .createdAt(OffsetDateTime.now())
            .build();
        
        
        TransactionStatusUpdatedEvent result = fraudDetectionService.validateTransaction(event);
        
        
        assertThat(result).isNotNull();
        assertThat(result.getTransactionExternalId()).isEqualTo(event.getTransactionExternalId());
        assertThat(result.getStatus()).isEqualTo(TransactionStatusUpdatedEvent.TransactionStatus.REJECTED);
        assertThat(result.getReason()).contains("excede el máximo permitido");
    }
    
    @Test
    @DisplayName("Debe rechazar transacción con monto justo por encima del límite")
    void shouldRejectTransactionJustAboveThreshold() {
        
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
            .transactionExternalId(UUID.randomUUID())
            .value(BigDecimal.valueOf(1000.01))
            .build();
        
        
        TransactionStatusUpdatedEvent result = fraudDetectionService.validateTransaction(event);
        
        
        assertThat(result.getStatus()).isEqualTo(TransactionStatusUpdatedEvent.TransactionStatus.REJECTED);
    }
    
    @Test
    @DisplayName("Debe generar ID de evento y timestamp")
    void shouldGenerateEventIdAndTimestamp() {
        
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
            .transactionExternalId(UUID.randomUUID())
            .value(BigDecimal.valueOf(100))
            .build();
        
        
        TransactionStatusUpdatedEvent result = fraudDetectionService.validateTransaction(event);
        
        
        assertThat(result.getEventId()).isNotNull();
        assertThat(result.getEventTimestamp()).isNotNull();
    }
}

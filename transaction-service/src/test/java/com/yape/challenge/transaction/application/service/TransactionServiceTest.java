package com.yape.challenge.transaction.application.service;

import com.yape.challenge.transaction.application.dto.CreateTransactionRequest;
import com.yape.challenge.transaction.application.dto.TransactionResponse;
import com.yape.challenge.transaction.domain.model.Transaction;
import com.yape.challenge.transaction.domain.model.TransactionStatus;
import com.yape.challenge.transaction.domain.model.TransactionType;
import com.yape.challenge.transaction.domain.repository.TransactionRepository;
import com.yape.challenge.transaction.domain.repository.TransactionStatusRepository;
import com.yape.challenge.transaction.domain.repository.TransactionTypeRepository;
import com.yape.challenge.transaction.infrastructure.kafka.TransactionEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Transaction Service")
class TransactionServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private TransactionTypeRepository transactionTypeRepository;
    
    @Mock
    private TransactionStatusRepository transactionStatusRepository;
    
    @Mock
    private TransactionEventProducer eventProducer;
    
    @Mock
    private TransactionMapper transactionMapper;
    
    @InjectMocks
    private TransactionService transactionService;
    
    private TransactionType transactionType;
    private TransactionStatus pendingStatus;
    private TransactionStatus approvedStatus;
    
    @BeforeEach
    void setUp() {
        transactionType = TransactionType.builder()
            .id(1)
            .name("transfer")
            .build();
        
        pendingStatus = TransactionStatus.builder()
            .id(1)
            .name("pending")
            .build();
        
        approvedStatus = TransactionStatus.builder()
            .id(2)
            .name("approved")
            .build();
    }
    
    @Test
    @DisplayName("Debe crear transacción exitosamente")
    void shouldCreateTransactionSuccessfully() {
        
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .accountExternalIdDebit(UUID.randomUUID())
            .accountExternalIdCredit(UUID.randomUUID())
            .transferTypeId(1)
            .value(BigDecimal.valueOf(100))
            .build();
        
        Transaction savedTransaction = Transaction.builder()
            .id(1L)
            .transactionExternalId(UUID.randomUUID())
            .accountExternalIdDebit(request.getAccountExternalIdDebit())
            .accountExternalIdCredit(request.getAccountExternalIdCredit())
            .transactionType(transactionType)
            .transactionStatus(pendingStatus)
            .value(request.getValue())
            .createdAt(OffsetDateTime.now())
            .build();
        
        TransactionResponse expectedResponse = TransactionResponse.builder()
            .transactionId(savedTransaction.getTransactionExternalId())
            .value(savedTransaction.getValue())
            .build();
        
        when(transactionTypeRepository.findById(anyInt())).thenReturn(Optional.of(transactionType));
        when(transactionStatusRepository.findByName("pending")).thenReturn(Optional.of(pendingStatus));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(expectedResponse);
        
        
        TransactionResponse response = transactionService.createTransaction(request);
        
        
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(savedTransaction.getTransactionExternalId());
        
        verify(transactionRepository).save(any(Transaction.class));
        verify(eventProducer).sendTransactionCreatedEvent(any());
    }
    
    @Test
    @DisplayName("Debe lanzar excepción cuando tipo de transacción no existe")
    void shouldThrowExceptionWhenTransactionTypeNotFound() {
        
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .accountExternalIdDebit(UUID.randomUUID())
            .accountExternalIdCredit(UUID.randomUUID())
            .transferTypeId(999)
            .value(BigDecimal.valueOf(100))
            .build();
        
        when(transactionTypeRepository.findById(999)).thenReturn(Optional.empty());
        
        
        assertThatThrownBy(() -> transactionService.createTransaction(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ID de tipo de transacción inválido");
        
        verify(transactionRepository, never()).save(any());
        verify(eventProducer, never()).sendTransactionCreatedEvent(any());
    }
    
    @Test
    @DisplayName("Debe actualizar estado de transacción a aprobado")
    void shouldUpdateTransactionStatusToApproved() {
        
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
            .id(1L)
            .transactionExternalId(transactionId)
            .transactionStatus(pendingStatus)
            .build();
        
        when(transactionRepository.findByTransactionExternalIdForUpdate(transactionId))
            .thenReturn(Optional.of(transaction));
        when(transactionStatusRepository.findByName("approved")).thenReturn(Optional.of(approvedStatus));
        
        
        transactionService.updateTransactionStatus(transactionId, "approved");
        
        
        verify(transactionRepository).save(transaction);
        assertThat(transaction.getTransactionStatus()).isEqualTo(approvedStatus);
    }
    
    @Test
    @DisplayName("Debe lanzar excepción al actualizar transacción inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentTransaction() {
        // Given
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findByTransactionExternalIdForUpdate(transactionId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> transactionService.updateTransactionStatus(transactionId, "approved"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Transacción no encontrada");
    }
    
    @Test
    @DisplayName("Debe obtener transacción por ID externo")
    void shouldRetrieveTransactionByExternalId() {
        
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
            .transactionExternalId(transactionId)
            .build();
        
        TransactionResponse expectedResponse = TransactionResponse.builder()
            .transactionId(transactionId)
            .build();
        
        when(transactionRepository.findByTransactionExternalId(transactionId))
            .thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(expectedResponse);
        
        
        TransactionResponse response = transactionService.getTransaction(transactionId);
        
        
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(transactionId);
    }
    
    @Test
    @DisplayName("Debe devolver transacción existente con idempotencyKey duplicado")
    void shouldReturnExistingTransactionWhenIdempotencyKeyIsDuplicated() {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .accountExternalIdDebit(UUID.randomUUID())
            .accountExternalIdCredit(UUID.randomUUID())
            .transferTypeId(1)
            .value(BigDecimal.valueOf(500))
            .idempotencyKey(idempotencyKey)
            .build();
        
        Transaction existingTransaction = Transaction.builder()
            .id(1L)
            .transactionExternalId(UUID.randomUUID())
            .idempotencyKey(idempotencyKey)
            .accountExternalIdDebit(request.getAccountExternalIdDebit())
            .accountExternalIdCredit(request.getAccountExternalIdCredit())
            .transactionType(transactionType)
            .transactionStatus(pendingStatus)
            .value(request.getValue())
            .createdAt(OffsetDateTime.now())
            .build();
        
        TransactionResponse expectedResponse = TransactionResponse.builder()
            .transactionId(existingTransaction.getTransactionExternalId())
            .value(existingTransaction.getValue())
            .status("PENDING")
            .build();
        
        when(transactionRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Optional.of(existingTransaction));
        when(transactionMapper.toResponse(existingTransaction))
            .thenReturn(expectedResponse);
        
        // Act
        TransactionResponse response = transactionService.createTransaction(request);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(existingTransaction.getTransactionExternalId());
        
        // Verificar que NO se intentó crear una nueva transacción
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(transactionRepository, times(1)).findByIdempotencyKey(idempotencyKey);
        
        // Verificar que NO se envió evento a Kafka
        verify(eventProducer, never()).sendTransactionCreatedEvent(any());
    }
    
    @Test
    @DisplayName("Debe crear nueva transacción cuando idempotencyKey es nulo")
    void shouldCreateNewTransactionWhenIdempotencyKeyIsNull() {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .accountExternalIdDebit(UUID.randomUUID())
            .accountExternalIdCredit(UUID.randomUUID())
            .transferTypeId(1)
            .value(BigDecimal.valueOf(500))
            .idempotencyKey(null)
            .build();
        
        Transaction savedTransaction = Transaction.builder()
            .id(1L)
            .transactionExternalId(UUID.randomUUID())
            .accountExternalIdDebit(request.getAccountExternalIdDebit())
            .accountExternalIdCredit(request.getAccountExternalIdCredit())
            .transactionType(transactionType)
            .transactionStatus(pendingStatus)
            .value(request.getValue())
            .createdAt(OffsetDateTime.now())
            .build();
        
        TransactionResponse expectedResponse = TransactionResponse.builder()
            .transactionId(savedTransaction.getTransactionExternalId())
            .value(savedTransaction.getValue())
            .build();
        
        when(transactionTypeRepository.findById(anyInt())).thenReturn(Optional.of(transactionType));
        when(transactionStatusRepository.findByName("pending")).thenReturn(Optional.of(pendingStatus));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(expectedResponse);
        
        // Act
        TransactionResponse response = transactionService.createTransaction(request);
        
        // Assert
        assertThat(response).isNotNull();
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(eventProducer, times(1)).sendTransactionCreatedEvent(any());
        
        // NO debe buscar por idempotencyKey cuando es null
        verify(transactionRepository, never()).findByIdempotencyKey(any());
    }
}

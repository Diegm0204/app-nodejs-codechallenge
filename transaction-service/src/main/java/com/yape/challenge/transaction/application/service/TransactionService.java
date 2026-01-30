package com.yape.challenge.transaction.application.service;

import com.yape.challenge.shared.event.TransactionCreatedEvent;
import com.yape.challenge.transaction.application.dto.CreateTransactionRequest;
import com.yape.challenge.transaction.application.dto.TransactionResponse;
import com.yape.challenge.transaction.domain.model.Transaction;
import com.yape.challenge.transaction.domain.model.TransactionStatus;
import com.yape.challenge.transaction.domain.model.TransactionType;
import com.yape.challenge.transaction.domain.repository.TransactionRepository;
import com.yape.challenge.transaction.domain.repository.TransactionStatusRepository;
import com.yape.challenge.transaction.domain.repository.TransactionTypeRepository;
import com.yape.challenge.transaction.infrastructure.kafka.TransactionEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final TransactionEventProducer eventProducer;
    private final TransactionMapper transactionMapper;
    
   
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        log.info("Creando transacción con monto: {}", request.getValue());
        
        // IDEMPOTENCIA: Verificar si ya existe una transacción con este idempotencyKey
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingTransaction.isPresent()) {
                log.info("Transacción duplicada detectada con idempotencyKey: {}. Devolviendo transacción existente: {}", 
                    request.getIdempotencyKey(), existingTransaction.get().getTransactionExternalId());
                return transactionMapper.toResponse(existingTransaction.get());
            }
        }
        
        // Validar y obtener tipo de transacción
        TransactionType transactionType = transactionTypeRepository.findById(request.getTransferTypeId())
            .orElseThrow(() -> new IllegalArgumentException("ID de tipo de transacción inválido: " + request.getTransferTypeId()));
        
        // Obtener estado pendiente
        TransactionStatus pendingStatus = transactionStatusRepository.findByName("pending")
            .orElseThrow(() -> new IllegalStateException("Estado pendiente no encontrado en base de datos"));
        
        // Crear entidad de transacción
        Transaction transaction = Transaction.builder()
            .transactionExternalId(UUID.randomUUID())
            .idempotencyKey(request.getIdempotencyKey())
            .accountExternalIdDebit(request.getAccountExternalIdDebit())
            .accountExternalIdCredit(request.getAccountExternalIdCredit())
            .transactionType(transactionType)
            .transactionStatus(pendingStatus)
            .value(request.getValue())
            .build();
        
        // Guardar transacción
        transaction = transactionRepository.save(transaction);
        log.info("Transacción creada con ID: {}", transaction.getTransactionExternalId());
        
        // Emitir evento a Kafka para validación antifraude
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
            .transactionExternalId(transaction.getTransactionExternalId())
            .accountExternalIdDebit(transaction.getAccountExternalIdDebit())
            .accountExternalIdCredit(transaction.getAccountExternalIdCredit())
            .transferTypeId(transaction.getTransactionType().getId())
            .value(transaction.getValue())
            .createdAt(transaction.getCreatedAt())
            .eventId(UUID.randomUUID().toString())
            .eventTimestamp(OffsetDateTime.now())
            .build();
        
        eventProducer.sendTransactionCreatedEvent(event);
        
        return transactionMapper.toResponse(transaction);
    }
    
   
    @Transactional(readOnly = true)
    @Cacheable(value = "transactions", key = "#transactionExternalId")
    public TransactionResponse getTransaction(UUID transactionExternalId) {
        log.debug("Consultando transacción: {}", transactionExternalId);
        
        Transaction transaction = transactionRepository.findByTransactionExternalId(transactionExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada: " + transactionExternalId));
        
        return transactionMapper.toResponse(transaction);
    }
    
   
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(int page, int size) {
        log.debug("Consultando transacciones página: {}, tamaño: {}", page, size);
        
        Page<Transaction> transactions = transactionRepository.findAllByOrderByCreatedAtDesc(
            PageRequest.of(page, size)
        );
        
        return transactions.map(transactionMapper::toResponse);
    }
    
   
    @Cacheable(value = "transactions", key = "'status:' + #status + ':' + #page + ':' + #size")
    public Page<TransactionResponse> getTransactionsByStatus(String status, int page, int size) {
        log.debug("Consultando transacciones con status: {}, página: {}, tamaño: {}", status, page, size);
        
        Page<Transaction> transactions = transactionRepository.findByTransactionStatusNameOrderByCreatedAtDesc(
            status, 
            PageRequest.of(page, size)
        );
        
        return transactions.map(transactionMapper::toResponse);
    }
    
 
    @Transactional
    @CacheEvict(value = "transactions", key = "#transactionExternalId")
    public void updateTransactionStatus(UUID transactionExternalId, String statusName) {
        log.info("Actualizando transacción {} a estado: {}", transactionExternalId, statusName);
        
        // Usar bloqueo pesimista para prevenir actualizaciones concurrentes
        Transaction transaction = transactionRepository.findByTransactionExternalIdForUpdate(transactionExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada: " + transactionExternalId));
        
        TransactionStatus newStatus = transactionStatusRepository.findByName(statusName)
            .orElseThrow(() -> new IllegalArgumentException("Estado inválido: " + statusName));
        
        // Usar método de dominio para actualizar estado (valida reglas de negocio)
        if ("approved".equals(statusName)) {
            transaction.approve(newStatus);
        } else if ("rejected".equals(statusName)) {
            transaction.reject(newStatus);
        } else {
            throw new IllegalArgumentException("Transición de estado inválida: " + statusName);
        }
        
        transactionRepository.save(transaction);
        log.info("Transacción {} actualizada exitosamente", transactionExternalId);
    }
}

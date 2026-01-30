package com.yape.challenge.transaction.domain.repository;

import com.yape.challenge.transaction.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
   
    Optional<Transaction> findByTransactionExternalId(UUID transactionExternalId);
    
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.transactionExternalId = :transactionExternalId")
    Optional<Transaction> findByTransactionExternalIdForUpdate(UUID transactionExternalId);
    
   
    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
   
    Page<Transaction> findByTransactionStatusNameOrderByCreatedAtDesc(String statusName, Pageable pageable);
   
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
  
    boolean existsByTransactionExternalId(UUID transactionExternalId);
}

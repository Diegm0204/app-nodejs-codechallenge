package com.yape.challenge.transaction.domain.repository;

import com.yape.challenge.transaction.domain.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionStatusRepository extends JpaRepository<TransactionStatus, Integer> {
    
    Optional<TransactionStatus> findByName(String name);
}

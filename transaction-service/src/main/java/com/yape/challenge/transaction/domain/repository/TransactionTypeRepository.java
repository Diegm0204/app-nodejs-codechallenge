package com.yape.challenge.transaction.domain.repository;

import com.yape.challenge.transaction.domain.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Integer> {
    
    Optional<TransactionType> findByName(String name);
}

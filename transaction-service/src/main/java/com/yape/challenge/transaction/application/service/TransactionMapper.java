package com.yape.challenge.transaction.application.service;

import com.yape.challenge.transaction.application.dto.TransactionResponse;
import com.yape.challenge.transaction.domain.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    
    public TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
            .transactionId(transaction.getTransactionExternalId())
            .accountExternalIdDebit(transaction.getAccountExternalIdDebit())
            .accountExternalIdCredit(transaction.getAccountExternalIdCredit())
            .transactionType(TransactionResponse.TransactionTypeDto.builder()
                .id(transaction.getTransactionType().getId())
                .name(transaction.getTransactionType().getName())
                .build())
            .status(transaction.getTransactionStatus().getName())
            .value(transaction.getValue())
            .createdAt(transaction.getCreatedAt())
            .updatedAt(transaction.getUpdatedAt())
            .build();
    }
}

package com.yape.challenge.transaction.infrastructure.graphql;

import com.yape.challenge.transaction.application.dto.CreateTransactionRequest;
import com.yape.challenge.transaction.application.dto.TransactionResponse;
import com.yape.challenge.transaction.application.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;


@Controller
@RequiredArgsConstructor
@Validated
@Slf4j
public class TransactionGraphQLController {
    
    private final TransactionService transactionService;
    
    @QueryMapping
    public TransactionResponse transaction(@Argument UUID id) {
        log.info("GraphQL Query: transaction({})", id);
        return transactionService.getTransaction(id);
    }
    
   
    @QueryMapping
    public TransactionPageResponse transactions(
        @Argument(name = "page") Integer page,
        @Argument(name = "size") Integer size,
        @Argument(name = "status") String status
    ) {
        log.info("GraphQL Query: transactions(page={}, size={}, status={})", page, size, status);
        
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        
        Page<TransactionResponse> transactionPage;
        if (status != null && !status.isBlank()) {
            transactionPage = transactionService.getTransactionsByStatus(status, pageNumber, pageSize);
        } else {
            transactionPage = transactionService.getAllTransactions(pageNumber, pageSize);
        }
        
        return TransactionPageResponse.builder()
            .content(transactionPage.getContent())
            .totalElements((int) transactionPage.getTotalElements())
            .totalPages(transactionPage.getTotalPages())
            .currentPage(transactionPage.getNumber())
            .size(transactionPage.getSize())
            .build();
    }
    
   
    @MutationMapping
    public TransactionResponse createTransaction(@Argument("input") @Valid CreateTransactionRequest input) {
        log.info("GraphQL Mutation: createTransaction(accountDebit={}, accountCredit={}, value={})",
            input.getAccountExternalIdDebit(), input.getAccountExternalIdCredit(), input.getValue());
        
        return transactionService.createTransaction(input);
    }
}

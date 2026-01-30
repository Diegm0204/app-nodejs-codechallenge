package com.yape.challenge.transaction.infrastructure.graphql;

import com.yape.challenge.transaction.application.dto.TransactionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPageResponse {
    private List<TransactionResponse> content;
    private int totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
}

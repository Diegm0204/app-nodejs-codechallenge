package com.yape.challenge.transaction.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {
    
    @NotNull(message = "ID externo de cuenta débito es requerido")
    private UUID accountExternalIdDebit;
    
    @NotNull(message = "ID externo de cuenta crédito es requerido")
    private UUID accountExternalIdCredit;
    
    @NotNull(message = "ID de tipo de transferencia es requerido")
    private Integer transferTypeId;
    
    @NotNull(message = "Monto es requerido")
    @DecimalMin(value = "0.01", message = "Monto debe ser mayor a 0")
    private BigDecimal value;
    
  
    private String idempotencyKey;
}

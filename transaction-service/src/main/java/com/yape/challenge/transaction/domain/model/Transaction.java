package com.yape.challenge.transaction.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_external_id", columnList = "transaction_external_id"),
    @Index(name = "idx_transaction_status", columnList = "status_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_external_id", nullable = false, unique = true, updatable = false)
    private UUID transactionExternalId;
    
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;
    
    @Column(name = "account_external_id_debit", nullable = false)
    private UUID accountExternalIdDebit;
    
    @Column(name = "account_external_id_credit", nullable = false)
    private UUID accountExternalIdCredit;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transfer_type_id", nullable = false)
    private TransactionType transactionType;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    private TransactionStatus transactionStatus;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal value;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @Version
    private Long version;
    
    public void approve(TransactionStatus approvedStatus) {
        if (!this.transactionStatus.getName().equals("pending")) {
            throw new IllegalStateException("Solo se pueden aprobar transacciones pendientes");
        }
        this.transactionStatus = approvedStatus;
    }
    
    public void reject(TransactionStatus rejectedStatus) {
        if (!this.transactionStatus.getName().equals("pending")) {
            throw new IllegalStateException("Solo se pueden rechazar transacciones pendientes");
        }
        this.transactionStatus = rejectedStatus;
    }
    
    public boolean isPending() {
        return this.transactionStatus.getName().equals("pending");
    }
}

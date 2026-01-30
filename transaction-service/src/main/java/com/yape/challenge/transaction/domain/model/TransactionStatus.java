package com.yape.challenge.transaction.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "transaction_statuses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, unique = true, length = 20)
    private String name;
    
    @Column(length = 255)
    private String description;
}

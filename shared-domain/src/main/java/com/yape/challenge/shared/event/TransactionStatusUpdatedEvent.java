package com.yape.challenge.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatusUpdatedEvent {
    
    private UUID transactionExternalId;
    private TransactionStatus status;
    private String reason;
    
    private String eventId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime eventTimestamp;
    
    public enum TransactionStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}

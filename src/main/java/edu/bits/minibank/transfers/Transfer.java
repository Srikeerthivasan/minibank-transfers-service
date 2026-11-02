package edu.bits.minibank.transfers;
import java.math.BigDecimal;
import java.util.UUID;
public record Transfer(UUID id, String fromAccountId, String toAccountId, BigDecimal amount,
                       String currency, String status, String reason, int attempts) {
    public TransferRequest request() { return new TransferRequest(fromAccountId,toAccountId,amount,currency); }
}

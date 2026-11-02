package edu.bits.minibank.transfers;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

// Duplicated small wire contract keeps the two repositories independently buildable.
public record TransferRequest(
    @NotBlank @Size(max=30) String fromAccountId,
    @NotBlank @Size(max=30) String toAccountId,
    @NotNull @DecimalMin("0.01") @Digits(integer=12, fraction=2) BigDecimal amount,
    @NotNull @Pattern(regexp="INR") String currency) {
    @AssertTrue(message="source and destination must differ")
    public boolean isDifferentAccounts() {
        return fromAccountId == null || !fromAccountId.equals(toAccountId);
    }
}

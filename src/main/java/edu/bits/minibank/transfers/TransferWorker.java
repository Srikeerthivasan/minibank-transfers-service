package edu.bits.minibank.transfers;

import org.slf4j.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="bank.worker-enabled", havingValue="true", matchIfMissing=true)
public class TransferWorker {
    private static final Logger log=LoggerFactory.getLogger(TransferWorker.class);
    private final TransfersService transfers;
    private final AccountsClient accounts;
    public TransferWorker(TransfersService transfers,AccountsClient accounts) { this.transfers=transfers;this.accounts=accounts; }
    @Scheduled(fixedDelayString="${bank.worker-delay-ms:1000}",initialDelay=1000)
    public void processPending() {
        for (Transfer t:transfers.claimPending()) {
            try {
                // ALWAYS replay the stable transfer UUID. A timeout may follow a commit.
                var outcome=accounts.post(t);
                transfers.finish(t.id(),outcome.status(),outcome.reason());
                log.info("Transfer {} -> {}",t.id(),outcome.status());
            } catch (RuntimeException e) {
                transfers.retry(t);
                log.warn("Transfer {} remains PENDING; attempt {} ({})",t.id(),t.attempts(),e.getClass().getSimpleName());
            }
        }
    }
}

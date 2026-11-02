package edu.bits.minibank.transfers;

import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AccountsClient {
    private final RestClient http;
    public record Posting(UUID transferId,String fromAccountId,String toAccountId,
                          java.math.BigDecimal amount,String currency,String status,String reason) { }
    public AccountsClient(@Value("${bank.accounts-url}") String url,@Value("${bank.internal-token}") String token) {
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        http=RestClient.builder().baseUrl(url).requestFactory(factory).defaultHeader("X-Internal-Token",token).build();
    }
    public Posting post(Transfer t) {
        Posting p=http.post().uri("/internal/transfers/{id}/post",t.id()).body(t.request()).retrieve().body(Posting.class);
        // Never finalize a request from an invalid or unrelated response.
        if (p==null || !t.id().equals(p.transferId()) || !t.fromAccountId().equals(p.fromAccountId())
                || !t.toAccountId().equals(p.toAccountId()) || p.amount()==null || t.amount().compareTo(p.amount())!=0
                || !t.currency().equals(p.currency()) || !("COMPLETED".equals(p.status()) || "REJECTED".equals(p.status())))
            throw new IllegalStateException("Invalid Accounts response");
        return p;
    }
}

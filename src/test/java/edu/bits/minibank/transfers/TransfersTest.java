package edu.bits.minibank.transfers;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="bank.worker-enabled=false")
class TransfersTest extends MySqlTestBase {
    @Autowired TransfersService service;
    @Autowired JdbcTemplate db;
    @Autowired TestRestTemplate http;
    @MockitoBean AccountsClient accounts;
    TransferRequest request(String amount) { return new TransferRequest("A1001","A1002",new BigDecimal(amount),"INR"); }
    @BeforeEach void reset() { db.execute("TRUNCATE transfers"); }
    @Test void repeatedKeyReturnsSameIdButChangedBodyConflicts() {
        Transfer first=service.submit("same-key",request("125.00"));
        assertThat(service.submit("same-key",request("125")).id()).isEqualTo(first.id());
        assertThatThrownBy(() -> service.submit("same-key",request("126"))).hasMessageContaining("different request");
        assertThat(db.queryForObject("SELECT COUNT(*) FROM transfers",Integer.class)).isEqualTo(1);
    }
    @Test void simultaneousSubmissionsCreateOneRequest() throws Exception {
        Set<UUID> ids=new HashSet<>();
        var pool=Executors.newFixedThreadPool(8);
        try {
            var tasks=new ArrayList<Callable<UUID>>();
            for(int i=0;i<20;i++) tasks.add(() -> service.submit("concurrent-key",request("1")).id());
            for(var result:pool.invokeAll(tasks)) ids.add(result.get());
        } finally {
            pool.shutdownNow();
        }
        assertThat(ids).hasSize(1);
    }
    @Test void transientFailureKeepsPendingAndReplayUsesSameId() {
        Transfer first=service.submit("retry-key",request("20"));
        var completed=new AccountsClient.Posting(first.id(),"A1001","A1002",new BigDecimal("20"),"INR","COMPLETED",null);
        when(accounts.post(any())).thenThrow(new ResourceAccessException("reply lost")).thenReturn(completed);
        var worker=new TransferWorker(service,accounts);
        worker.processPending();
        assertThat(service.get(first.id()).status()).isEqualTo("PENDING");
        db.update("UPDATE transfers SET next_attempt_at=CURRENT_TIMESTAMP WHERE id=?",first.id().toString());
        worker.processPending();
        assertThat(service.get(first.id()).status()).isEqualTo("COMPLETED");
        var captured=org.mockito.ArgumentCaptor.forClass(Transfer.class);
        verify(accounts,times(2)).post(captured.capture());
        assertThat(captured.getAllValues()).allMatch(t -> t.id().equals(first.id()));
    }
    @Test void expiredLeaseCanBeRecovered() {
        Transfer first=service.submit("lease-key",request("1"));
        assertThat(service.claimPending()).hasSize(1);
        assertThat(service.claimPending()).isEmpty();
        db.update("UPDATE transfers SET lease_until=CURRENT_TIMESTAMP-INTERVAL 1 MINUTE WHERE id=?",first.id().toString());
        assertThat(service.claimPending()).extracting(Transfer::id).containsExactly(first.id());
    }
    @Test void businessRejectionIsFinal() {
        Transfer first=service.submit("reject-key",request("99999"));
        when(accounts.post(any())).thenReturn(new AccountsClient.Posting(first.id(),"A1001","A1002",new BigDecimal("99999"),"INR","REJECTED","INSUFFICIENT_FUNDS"));
        new TransferWorker(service,accounts).processPending();
        assertThat(service.get(first.id()).reason()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(service.claimPending()).isEmpty();
    }
    @Test void httpValidatesAndReturnsStatusLocation() {
        var headers=new HttpHeaders();headers.set("Idempotency-Key","http-test");
        var accepted=http.postForEntity("/transfers",new HttpEntity<>(request("125"),headers),Transfer.class);
        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(accepted.getHeaders().getLocation()).hasToString("/transfers/"+accepted.getBody().id());
        assertThat(http.postForEntity("/transfers",new HttpEntity<>(request("0"),headers),String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.postForEntity("/transfers",request("1"),String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.postForEntity("/transfers",new HttpEntity<>(request("126"),headers),String.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}

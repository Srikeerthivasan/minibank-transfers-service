package edu.bits.minibank.transfers;

import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class TransfersService {
    private final JdbcTemplate db;
    public TransfersService(JdbcTemplate db) { this.db=db; }
    private final RowMapper<Transfer> mapper=(r,n) -> new Transfer(UUID.fromString(r.getString("id")),r.getString("from_account"),r.getString("to_account"),r.getBigDecimal("amount"),r.getString("currency"),r.getString("status"),r.getString("reason"),r.getInt("attempts"));
    @Transactional
    public Transfer submit(String key, TransferRequest r) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,100}"))
            throw new ResponseStatusException(BAD_REQUEST,"Idempotency-Key must be 1..100 letters, digits or . _ : -");
        db.update("INSERT INTO transfers(id,idempotency_key,from_account,to_account,amount,currency) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE idempotency_key=idempotency_key",
            UUID.randomUUID().toString(),key,r.fromAccountId(),r.toAccountId(),r.amount(),r.currency());
        Transfer saved=db.queryForObject("SELECT * FROM transfers WHERE idempotency_key=?",mapper,key);
        if (!saved.fromAccountId().equals(r.fromAccountId()) || !saved.toAccountId().equals(r.toAccountId())
                || saved.amount().compareTo(r.amount())!=0 || !saved.currency().equals(r.currency()))
            throw new ResponseStatusException(CONFLICT,"Idempotency-Key already used with a different request");
        return saved;
    }
    public Transfer get(UUID id) {
        return db.query("SELECT * FROM transfers WHERE id=?",mapper,id.toString()).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"Transfer not found"));
    }
    public List<Transfer> history(String accountId,int limit,int offset) {
        if (limit<1 || limit>100 || offset<0) throw new ResponseStatusException(BAD_REQUEST,"limit must be 1..100; offset must be nonnegative");
        return db.query("SELECT * FROM transfers WHERE from_account=? OR to_account=? ORDER BY created_at DESC,id LIMIT ? OFFSET ?",mapper,accountId,accountId,limit,offset);
    }
    // Claim rows in a short transaction. Locks end before the worker calls Accounts.
    // An expired lease lets a replacement pod resume interrupted work.
    @Transactional
    public List<Transfer> claimPending() {
        var batch = db.query("""
            SELECT * FROM transfers WHERE status='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP(6)
              AND (lease_until IS NULL OR lease_until<CURRENT_TIMESTAMP(6))
              ORDER BY created_at,id LIMIT 5 FOR UPDATE SKIP LOCKED
            """, mapper);
        var claimed = new ArrayList<Transfer>();
        for (Transfer t : batch) {
            db.update("UPDATE transfers SET lease_until=CURRENT_TIMESTAMP(6)+INTERVAL 60 SECOND,attempts=attempts+1 WHERE id=?", t.id().toString());
            claimed.add(get(t.id()));
        }
        return claimed;
    }
    public void finish(UUID id,String status,String reason) {
        db.update("UPDATE transfers SET status=?,reason=?,lease_until=NULL WHERE id=? AND status='PENDING'",status,reason,id.toString());
    }
    public void retry(Transfer t) {
        long delay=Math.min(30000L,1000L << Math.min(t.attempts(),5));
        db.update("UPDATE transfers SET lease_until=NULL,next_attempt_at=TIMESTAMPADD(MICROSECOND,?,CURRENT_TIMESTAMP(6)) WHERE id=? AND status='PENDING'",delay*1000,t.id().toString());
    }
}

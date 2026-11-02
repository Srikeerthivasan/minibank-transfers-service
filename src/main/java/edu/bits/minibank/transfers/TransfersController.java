package edu.bits.minibank.transfers;

import java.net.URI;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TransfersController {
    private final TransfersService transfers;
    public TransfersController(TransfersService transfers) { this.transfers=transfers; }
    @PostMapping("/transfers")
    public ResponseEntity<Transfer> submit(@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody TransferRequest request) {
        Transfer result=transfers.submit(key,request);
        return ResponseEntity.accepted().location(URI.create("/transfers/"+result.id())).body(result);
    }
    @GetMapping("/transfers/{id}") public Transfer get(@PathVariable UUID id) { return transfers.get(id); }
    @GetMapping("/transfers")
    public List<Transfer> history(@RequestParam String accountId,@RequestParam(defaultValue="20") int limit,@RequestParam(defaultValue="0") int offset) {
        return transfers.history(accountId,limit,offset);
    }
}

package tj.abad.duobtms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tj.abad.duobtms.request.ApplicationTransactionRequest;
import tj.abad.duobtms.response.ApplicationClientBalanceResponse;
import tj.abad.duobtms.response.ApplicationMoneyTransferResponse;
import tj.abad.duobtms.response.application.ApplicationCargoIssueResponse;
import tj.abad.duobtms.response.application.ApplicationResponse;
import tj.abad.duobtms.service.MoneyTransactionService;

@RestController
@RequestMapping("money-transactions")
@RequiredArgsConstructor
public class MoneyTransactionController {
    private final MoneyTransactionService service;

    @PostMapping("fill-client-balance/{applicationId}")
    public ResponseEntity<ApplicationClientBalanceResponse> fillClientBalance(@PathVariable Long applicationId,
                                                                              @RequestBody ApplicationTransactionRequest request) {
        return ResponseEntity.ok(service.transactionClientBalance(applicationId, request));
    }

    @PostMapping("income-by-article/{applicationId}")
    public ResponseEntity<ApplicationResponse> incomeByArticle(@PathVariable Long applicationId, @RequestBody ApplicationTransactionRequest request) {
        return ResponseEntity.ok(service.transactionByArticle(applicationId, request));
    }

    @PostMapping("outcome-by-article/{applicationId}")
    public ResponseEntity<ApplicationResponse> outcomeByArticle(@PathVariable Long applicationId, @RequestBody ApplicationTransactionRequest request) {
        return ResponseEntity.ok(service.transactionByArticle(applicationId, request));
    }

    @PostMapping("transfer-to-warehouse/{applicationId}")
    public ResponseEntity<ApplicationMoneyTransferResponse> transferMoneyInWarehouse(@PathVariable Long applicationId, @RequestBody ApplicationTransactionRequest request) {
        return ResponseEntity.ok(service.transactionMoneyTransferOnRoad(applicationId, request));
    }

    @GetMapping("transfer-to-warehouse/{applicationId}")
    public ResponseEntity<ApplicationMoneyTransferResponse> transferMoneyToWarehouse(@PathVariable Long applicationId) {
        if (service.checkUserBelongWarehouse(applicationId)) {
            return ResponseEntity.ok(service.transactionMoneyTransferDelivered(applicationId));
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("cargo-issue/{applicationId}")
    public ResponseEntity<ApplicationCargoIssueResponse> cargoIssue(@PathVariable Long applicationId) {
        return ResponseEntity.ok(service.transactionCargoIssue(applicationId));
    }
}

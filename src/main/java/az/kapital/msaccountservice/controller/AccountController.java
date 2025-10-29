package az.kapital.msaccountservice.controller;

import az.kapital.msaccountservice.domain.entity.AccountEntity;
import az.kapital.msaccountservice.model.AccountResponse;
import az.kapital.msaccountservice.model.BalanceUpdateRequest;
import az.kapital.msaccountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{userId}/balance")
    public ResponseEntity<List<AccountEntity>> getBalance(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorizationHeader) {

        List<AccountEntity> balances = accountService.getBalanceForUser(userId, authorizationHeader);
        return ResponseEntity.ok(balances);
    }

    @PostMapping("/internal/update-balance")
    public ResponseEntity<AccountResponse> updateBalance(
            @RequestBody BalanceUpdateRequest request) {

        AccountResponse updatedAccount = accountService.updateBalance(
                request.getUserId(),
                request.getCurrency(),
                request.getAmount()
        );
        return ResponseEntity.ok(updatedAccount);
    }
}
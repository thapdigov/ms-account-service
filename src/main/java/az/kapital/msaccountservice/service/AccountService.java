package az.kapital.msaccountservice.service;

import az.kapital.msaccountservice.client.AuthServiceClient;
import az.kapital.msaccountservice.domain.entity.AccountEntity;
import az.kapital.msaccountservice.domain.entity.UserEntity;
import az.kapital.msaccountservice.domain.repository.AccountRepository;
import az.kapital.msaccountservice.domain.repository.UserRepository;
import az.kapital.msaccountservice.exception.ForbiddenAccessException;
import az.kapital.msaccountservice.exception.InsufficientFundsException;
import az.kapital.msaccountservice.exception.InvalidTokenException;
import az.kapital.msaccountservice.exception.UserNotFoundException;
import az.kapital.msaccountservice.model.AccountResponse;
import az.kapital.msaccountservice.model.Currency;
import az.kapital.msaccountservice.model.TokenValidationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuthServiceClient authServiceClient;

    @Transactional(readOnly = true)
    public List<AccountEntity> getBalanceForUser(Long userId, String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        String authenticatedUsername = authServiceClient.validateToken(new TokenValidationRequest(token));

        Long requesterId = userRepository.findByUsername(authenticatedUsername)
                .map(UserEntity::getId)
                .orElseThrow(() -> new UserNotFoundException("Requester not found in local DB."));

        if (!requesterId.equals(userId)) {
            throw new ForbiddenAccessException("You are not allowed to view this user’s balance.");
        }

        return accountRepository.findByUserId(userId);
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new InvalidTokenException("Missing Authorization header or Bearer token");
    }

    @Transactional
    public AccountResponse updateBalance(Long userId, Currency currency, BigDecimal amount) {

        AccountEntity account = accountRepository
                .findByUserIdAndCurrencyWithLock(userId, currency)
                .orElseThrow(() -> new RuntimeException("Account not found."));

        BigDecimal newBalance = account.getBalance().add(amount);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Current: %s %s, Tried to spend: %s",
                            account.getBalance(), currency, amount.negate())
            );
        }

        account.setBalance(newBalance);
        AccountEntity savedAccount = accountRepository.save(account);

        return new AccountResponse(
                savedAccount.getId(),
                savedAccount.getUser().getId(),
                savedAccount.getCurrency(),
                savedAccount.getBalance()
        );
    }
}
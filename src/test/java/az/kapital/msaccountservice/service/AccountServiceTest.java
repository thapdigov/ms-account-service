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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private AccountService accountService;

    private Long userId;
    private String validToken;
    private String authenticatedUsername;
    private Long requesterId;
    private AccountEntity accountEntity;
    private Currency currency;
    private BigDecimal initialBalance;
    private BigDecimal addAmount;

    @BeforeEach
    void setUp() {
        userId = 1L;
        validToken = "valid-jwt-token";
        authenticatedUsername = "testuser@example.com";
        requesterId = 1L;
        currency = Currency.USD;
        initialBalance = new BigDecimal("100.00");
        addAmount = new BigDecimal("50.00");

        accountEntity = new AccountEntity();
        accountEntity.setId(1L);
        accountEntity.setBalance(initialBalance);
        accountEntity.setCurrency(currency);
        UserEntity user = new UserEntity();
        user.setId(requesterId);
        accountEntity.setUser(user);
    }

    @Test
    @DisplayName("should return accounts when user is authorized")
    void getBalanceForUser_shouldReturnAccounts_WhenAuthorized() {
        String authHeader = "Bearer " + validToken;
        when(authServiceClient.validateToken(any(TokenValidationRequest.class))).thenReturn(authenticatedUsername);
        UserEntity mockUserEntity = new UserEntity();
        mockUserEntity.setId(requesterId);
        when(userRepository.findByUsername(authenticatedUsername)).thenReturn(Optional.of(mockUserEntity));
        when(accountRepository.findByUserId(userId)).thenReturn(List.of(accountEntity));

        List<AccountEntity> result = accountService.getBalanceForUser(userId, authHeader);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBalance()).isEqualByComparingTo(initialBalance);
        verify(authServiceClient, times(1)).validateToken(any(TokenValidationRequest.class));
        verify(userRepository, times(1)).findByUsername(authenticatedUsername);
        verify(accountRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("should throw forbidden exception when user ids do not match")
    void getBalanceForUser_shouldThrowForbidden_WhenIdsDoNotMatch() {
        Long differentRequesterId = 2L;
        String authHeader = "Bearer " + validToken;
        when(authServiceClient.validateToken(any(TokenValidationRequest.class))).thenReturn(authenticatedUsername);
        when(userRepository.findByUsername(authenticatedUsername)).thenReturn(Optional.of(new UserEntity() {{
            setId(differentRequesterId);
        }}));

        assertThatThrownBy(() -> accountService.getBalanceForUser(userId, authHeader))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage("You are not allowed to view this user’s balance.");

        verify(authServiceClient, times(1)).validateToken(any(TokenValidationRequest.class));
        verify(userRepository, times(1)).findByUsername(authenticatedUsername);
        verify(accountRepository, never()).findByUserId(anyLong());
    }

    @Test
    @DisplayName("should throw user not found when requester missing")
    void getBalanceForUser_shouldThrowUserNotFound_WhenRequesterMissing() {
        String authHeader = "Bearer " + validToken;
        when(authServiceClient.validateToken(any(TokenValidationRequest.class))).thenReturn(authenticatedUsername);
        when(userRepository.findByUsername(authenticatedUsername)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getBalanceForUser(userId, authHeader))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Requester not found in local DB.");

        verify(authServiceClient, times(1)).validateToken(any(TokenValidationRequest.class));
        verify(userRepository, times(1)).findByUsername(authenticatedUsername);
        verify(accountRepository, never()).findByUserId(anyLong());
    }

    @Test
    @DisplayName("should throw invalid token when header malformed")
    void getBalanceForUser_shouldThrowInvalidToken_WhenHeaderMalformed() {
        String malformedHeader = "Bearer";

        assertThatThrownBy(() -> accountService.getBalanceForUser(userId, malformedHeader))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Missing Authorization header or Bearer token");

        verifyNoInteractions(authServiceClient, userRepository, accountRepository);
    }

    @Test
    @DisplayName("should update balance successfully when sufficient funds")
    void updateBalance_shouldUpdateSuccessfully_WhenSufficientFunds() {
        BigDecimal expectedBalance = initialBalance.add(addAmount);
        when(accountRepository.findByUserIdAndCurrencyWithLock(userId, currency)).thenReturn(Optional.of(accountEntity));
        doReturn(accountEntity).when(accountRepository).save(accountEntity);

        AccountResponse result = accountService.updateBalance(userId, currency, addAmount);

        assertThat(result.getBalance()).isEqualByComparingTo(expectedBalance);
        assertThat(result.getCurrency()).isEqualTo(currency);
        verify(accountRepository, times(1)).findByUserIdAndCurrencyWithLock(userId, currency);
        verify(accountRepository, times(1)).save(accountEntity);
    }

    @Test
    @DisplayName("should throw insufficient funds when balance would go negative")
    void updateBalance_shouldThrowInsufficient_WhenNegativeBalance() {
        BigDecimal subtractAmount = new BigDecimal("-150.00");
        when(accountRepository.findByUserIdAndCurrencyWithLock(userId, currency)).thenReturn(Optional.of(accountEntity));

        assertThatThrownBy(() -> accountService.updateBalance(userId, currency, subtractAmount))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Insufficient funds. Current: 100.00 USD, Tried to spend: 150.00");

        verify(accountRepository, times(1)).findByUserIdAndCurrencyWithLock(userId, currency);
        verify(accountRepository, never()).save(any(AccountEntity.class));
    }

    @Test
    @DisplayName("should throw runtime exception when account not found")
    void updateBalance_shouldThrowRuntime_WhenAccountNotFound() {
        when(accountRepository.findByUserIdAndCurrencyWithLock(userId, currency)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.updateBalance(userId, currency, addAmount))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account not found.");

        verify(accountRepository, times(1)).findByUserIdAndCurrencyWithLock(userId, currency);
        verify(accountRepository, never()).save(any(AccountEntity.class));
    }
}
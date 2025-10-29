package az.kapital.msaccountservice.domain.repository;

import az.kapital.msaccountservice.domain.entity.AccountEntity;
import az.kapital.msaccountservice.model.Currency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    List<AccountEntity> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.user.id = :userId AND a.currency = :currency")
    Optional<AccountEntity> findByUserIdAndCurrencyWithLock(
            @Param("userId") Long userId,
            @Param("currency") Currency currency
    );
}

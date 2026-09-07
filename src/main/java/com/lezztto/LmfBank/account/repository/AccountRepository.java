package com.lezztto.LmfBank.account.repository;

import com.lezztto.LmfBank.account.domain.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByDocumentNumber(String documentNumber);

    @Query("""
    SELECT a
    FROM Account a
    LEFT JOIN FETCH a.addresses
    LEFT JOIN FETCH a.balance
    WHERE a.documentNumber = :documentNumber
    """)
    Optional<Account> findByDocumentNumberWithRelations(String documentNumber);

    @Query("""
    SELECT a
    FROM Account a
    LEFT JOIN FETCH a.addresses
    LEFT JOIN FETCH a.balance
    WHERE a.id = :id
    """)
    Optional<Account> findByIdWithRelations(Long id);

    /**
     * Loads the account row with a database-level write lock ({@code SELECT ... FOR UPDATE}).
     * Used by debit and transfer flows to serialize concurrent balance checks on the same
     * account, preventing an overdraft race when two withdrawals/transfers run in parallel.
     * No {@code JOIN FETCH} here: PostgreSQL rejects {@code FOR UPDATE} combined with the
     * outer join of a collection.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT a
    FROM Account a
    WHERE a.id = :id
    """)
    Optional<Account> findByIdForUpdate(Long id);

}

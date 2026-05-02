package com.lezztto.LmfBank.account.repository;

import com.lezztto.LmfBank.account.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

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

}

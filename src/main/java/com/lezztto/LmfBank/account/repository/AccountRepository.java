package com.lezztto.LmfBank.account.repository;

import com.lezztto.LmfBank.account.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByDocumentNumber(String documentNumber);

    Optional<Account> findByDocumentNumber(String documentNumber);

}

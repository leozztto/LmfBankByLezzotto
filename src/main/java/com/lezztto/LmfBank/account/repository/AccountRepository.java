package com.lezztto.LmfBank.account.repository;

import com.lezztto.LmfBank.account.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByDocumentNumber(String documentNumber);

}

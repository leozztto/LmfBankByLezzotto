package com.lezztto.LmfClients.account.repository;

import com.lezztto.LmfClients.account.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByDocumentNumber(String documentNumber);

}

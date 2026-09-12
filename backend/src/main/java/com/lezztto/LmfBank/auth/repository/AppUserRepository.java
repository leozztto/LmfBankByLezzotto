package com.lezztto.LmfBank.auth.repository;

import com.lezztto.LmfBank.auth.domain.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByAccountId(Long accountId);

    boolean existsByUsername(String username);

    /**
     * {@code @Transactional} porque, ao contrário de {@code save}/{@code deleteAll}
     * (cobertos pela anotação de classe em {@code SimpleJpaRepository}), uma query de
     * delete derivada não abre transação própria — sem isso, chamar este método fora de
     * um contexto transacional (como o {@code AbstractIntegrationTest}, que é
     * deliberadamente não-transacional) estoura {@code TransactionRequiredException}.
     */
    @Transactional
    void deleteByUsernameStartingWith(String prefix);
}

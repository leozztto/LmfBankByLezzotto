package com.lezztto.LmfBank.auth.service;

import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.auth.domain.entity.AppUser;
import com.lezztto.LmfBank.auth.exception.AccountAlreadyLinkedException;
import com.lezztto.LmfBank.auth.exception.AppUserNotFoundException;
import com.lezztto.LmfBank.auth.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Admin-only user management (ADR 0010) — today, just linking a login to its one account. */
@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final AccountService accountService;

    public void linkAccount(String username, Long accountId) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new AppUserNotFoundException(username));

        // 404 se a conta não existir.
        accountService.findByIdAccount(accountId);

        if (appUserRepository.existsByAccountId(accountId)) {
            throw new AccountAlreadyLinkedException(accountId);
        }

        user.setAccountId(accountId);
        try {
            // saveAndFlush (não save): força a constraint UNIQUE de app_user.account_id a
            // estourar AQUI, dentro do catch — duas requisições concorrentes podem passar
            // as duas pelo existsByAccountId acima antes de qualquer uma salvar (check-then-act).
            appUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new AccountAlreadyLinkedException(accountId);
        }
    }
}

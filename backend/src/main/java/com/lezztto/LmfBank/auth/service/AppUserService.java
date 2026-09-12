package com.lezztto.LmfBank.auth.service;

import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.auth.domain.AppUserResponse;
import com.lezztto.LmfBank.auth.domain.entity.AppUser;
import com.lezztto.LmfBank.auth.domain.enums.Role;
import com.lezztto.LmfBank.auth.exception.AccountAlreadyLinkedException;
import com.lezztto.LmfBank.auth.exception.AppUserNotFoundException;
import com.lezztto.LmfBank.auth.exception.UsernameAlreadyExistsException;
import com.lezztto.LmfBank.auth.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/** Admin-only user management (ADR 0010) — creating a login (always role USER — creating
 *  another admin isn't exposed here) and linking one to its account. */
@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

    public AppUserResponse createUser(String username, String password) {
        if (appUserRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        AppUser user = AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(Role.USER)
                .build();

        saveOrThrowOnDuplicate(user, "app_user_username_key", () -> new UsernameAlreadyExistsException(username));

        return new AppUserResponse(user.getUsername(), user.getRole(), user.getAccountId());
    }

    public void linkAccount(String username, Long accountId) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new AppUserNotFoundException(username));

        // 404 se a conta não existir.
        accountService.findByIdAccount(accountId);

        if (appUserRepository.existsByAccountId(accountId)) {
            throw new AccountAlreadyLinkedException(accountId);
        }

        user.setAccountId(accountId);
        saveOrThrowOnDuplicate(user, "app_user_account_id_key", () -> new AccountAlreadyLinkedException(accountId));
    }

    /**
     * {@code saveAndFlush} (não {@code save}): força a violação da constraint UNIQUE
     * relevante a estourar AQUI, dentro do catch — o check anterior (existsByX) e este save
     * não são atômicos, então duas requisições concorrentes podem passar as duas pelo check
     * antes de qualquer uma salvar. Sem o catch, a segunda vazaria como 500 em vez do 409
     * esperado.
     *
     * <p>Só a violação de {@code expectedConstraint} vira {@code onDuplicate} — qualquer
     * outra {@code DataIntegrityViolationException} (ex.: uma constraint nova que apareça
     * no futuro) sobe como está, em vez de ser atribuída à causa errada.
     */
    private void saveOrThrowOnDuplicate(
            AppUser user,
            String expectedConstraint,
            Supplier<? extends RuntimeException> onDuplicate
    ) {
        try {
            appUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            Throwable cause = ex.getMostSpecificCause();
            if (cause.getMessage() != null && cause.getMessage().contains(expectedConstraint)) {
                throw onDuplicate.get();
            }
            throw ex;
        }
    }
}

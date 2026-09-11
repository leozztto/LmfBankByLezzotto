package com.lezztto.LmfBank.auth.service;

import com.lezztto.LmfBank.auth.domain.entity.AppUser;
import com.lezztto.LmfBank.auth.exception.InvalidCredentialsException;
import com.lezztto.LmfBank.auth.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @return o {@link AppUser} autenticado (role + accountId, pro caller montar o JWT).
     * @throws InvalidCredentialsException se o usuário não existir ou a senha não bater —
     *         mesma mensagem/status nos dois casos, pra não revelar quais usuários existem.
     */
    public AppUser authenticate(String username, String password) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }
}

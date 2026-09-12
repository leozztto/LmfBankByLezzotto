package com.lezztto.LmfBank.auth.security;

import com.lezztto.LmfBank.auth.domain.AuthenticatedUser;
import com.lezztto.LmfBank.auth.domain.enums.Role;
import com.lezztto.LmfBank.auth.exception.ForbiddenActionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAccessGuardTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private AccountAccessGuard guard;

    @Test
    @DisplayName("admin passa pra qualquer accountId")
    void adminBypassesOwnership() {
        when(currentUserProvider.get()).thenReturn(new AuthenticatedUser("admin", Role.ADMIN, null));

        assertThatCode(() -> guard.assertOwnerOrAdmin(999L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("usuário comum: passa quando accountId bate com o seu")
    void ownerMatches() {
        when(currentUserProvider.get()).thenReturn(new AuthenticatedUser("owner", Role.USER, 7L));

        assertThatCode(() -> guard.assertOwnerOrAdmin(7L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("usuário comum: 403 quando accountId é de outro dono")
    void rejectsMismatch() {
        when(currentUserProvider.get()).thenReturn(new AuthenticatedUser("owner", Role.USER, 7L));

        assertThatThrownBy(() -> guard.assertOwnerOrAdmin(8L))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("usuário sem conta vinculada: 403 pra qualquer accountId")
    void rejectsWhenNoAccountLinked() {
        when(currentUserProvider.get()).thenReturn(new AuthenticatedUser("nobody", Role.USER, null));

        assertThatThrownBy(() -> guard.assertOwnerOrAdmin(1L))
                .isInstanceOf(ForbiddenActionException.class);
    }
}

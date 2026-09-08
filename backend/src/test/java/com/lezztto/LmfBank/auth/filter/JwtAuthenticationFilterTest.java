package com.lezztto.LmfBank.auth.filter;

import com.lezztto.LmfBank.auth.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse invoke(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertThat(chain.getRequest()).as("filter chain must always proceed").isNotNull();
        return response;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/auth/login", "/webjars/x", "/actuator/health",
            "/swagger-ui/index.html", "/v3/api-docs", "/swagger-ui.html"
    })
    @DisplayName("rotas públicas passam direto sem tocar no JwtService")
    void skipsPublicRoutes(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.addHeader("Authorization", "Bearer whatever");

        invoke(request);

        verifyNoInteractions(jwtService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("sem header Authorization: segue sem autenticar")
    void noAuthorizationHeader() throws Exception {
        invoke(new MockHttpServletRequest("GET", "/accounts/1"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("header sem prefixo 'Bearer ': segue sem autenticar")
    void nonBearerHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/accounts/1");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        invoke(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("token válido: popula o SecurityContext com o username")
    void validTokenAuthenticates() throws Exception {
        when(jwtService.isValid("good-token")).thenReturn(true);
        when(jwtService.extractUsername("good-token")).thenReturn("alice");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/accounts/1");
        request.addHeader("Authorization", "Bearer good-token");

        invoke(request);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("alice");
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("token inválido: segue sem autenticar")
    void invalidTokenIsIgnored() throws Exception {
        when(jwtService.isValid("bad-token")).thenReturn(false);
        lenient().when(jwtService.extractUsername("bad-token")).thenReturn("should-not-be-used");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/accounts/1");
        request.addHeader("Authorization", "Bearer bad-token");

        invoke(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}

package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.domain.entity.Account;
import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.movement.domain.entity.Transfer;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.domain.request.TransferRequest;
import com.lezztto.LmfBank.movement.domain.response.TransferResponse;
import com.lezztto.LmfBank.movement.exception.AccountStatusException;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import com.lezztto.LmfBank.movement.idempotency.TransferIdempotencyService;
import com.lezztto.LmfBank.movement.mapper.TransferMapper;
import com.lezztto.LmfBank.movement.repository.TransferRepository;
import com.lezztto.LmfBank.movement.util.AccountValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Regras de TRANSFERÊNCIA do ledger: gera um par de lançamentos
 * (DÉBITO na origem + CRÉDITO no destino) de forma atômica e idempotente.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final Long FROM = 1L;
    private static final Long TO = 2L;
    private static final UUID KEY = UUID.randomUUID();

    @Mock
    private TransferRepository transferRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private TransferMapper transferMapper;
    @Mock
    private AccountValidator accountValidator;
    @Mock
    private TransferIdempotencyService idempotencyService;
    @Mock
    private TransactionDomainService transactionDomainService;
    @Mock
    private BalanceCalculatorService balanceCalculatorService;
    @Mock
    private BalanceProjectionService balanceProjectionService;

    @InjectMocks
    private TransferService transferService;

    private TransferRequest request(Long from, Long to, BigDecimal amount) {
        return TransferRequest.builder()
                .fromAccountId(from)
                .toAccountId(to)
                .amount(amount)
                .idempotencyKey(KEY)
                .build();
    }

    private Account account(Long id, AccountStatus status) {
        return Account.builder().id(id).accountStatus(status).build();
    }

    private void stubActiveAccounts() {
        when(accountService.findByIdAccountForUpdate(FROM)).thenReturn(account(FROM, AccountStatus.ACTIVE));
        when(accountService.findByIdAccount(TO)).thenReturn(account(TO, AccountStatus.ACTIVE));
    }

    @Test
    @DisplayName("caminho feliz: gera DÉBITO na origem e CRÉDITO no destino com o mesmo transferId")
    void shouldCreateDebitAndCreditLegs() {
        BigDecimal amount = new BigDecimal("50.00");
        when(idempotencyService.tryLock(KEY)).thenReturn(true);
        stubActiveAccounts();
        when(balanceCalculatorService.calculate(FROM)).thenReturn(new BigDecimal("200.00"));
        when(transferMapper.toTransferResponse(any(Transfer.class)))
                .thenReturn(TransferResponse.builder().build());

        transferService.createTransfer(request(FROM, TO, amount));

        ArgumentCaptor<UUID> debitTransferId = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> creditTransferId = ArgumentCaptor.forClass(UUID.class);
        verify(transactionDomainService).create(eq(FROM), eq(TransactionType.DEBIT), eq(amount), anyString(), debitTransferId.capture());
        verify(transactionDomainService).create(eq(TO), eq(TransactionType.CREDIT), eq(amount), anyString(), creditTransferId.capture());
        assertThat(debitTransferId.getValue()).isEqualTo(creditTransferId.getValue());
    }

    @Test
    @DisplayName("persiste a transferência como COMPLETED e atualiza a projeção de saldo das duas contas")
    void shouldCompleteAndRefreshBothProjections() {
        when(idempotencyService.tryLock(KEY)).thenReturn(true);
        stubActiveAccounts();
        when(balanceCalculatorService.calculate(FROM)).thenReturn(new BigDecimal("200.00"));
        when(transferMapper.toTransferResponse(any(Transfer.class)))
                .thenReturn(TransferResponse.builder().build());

        transferService.createTransfer(request(FROM, TO, new BigDecimal("10.00")));

        ArgumentCaptor<Transfer> saved = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(saved.getValue().getFromAccountId()).isEqualTo(FROM);
        assertThat(saved.getValue().getToAccountId()).isEqualTo(TO);

        verify(balanceProjectionService).refresh(FROM);
        verify(balanceProjectionService).refresh(TO);
    }

    @Test
    @DisplayName("conta de origem e destino iguais: lança IllegalArgumentException antes de qualquer lançamento")
    void shouldRejectTransferToSameAccount() {
        when(idempotencyService.tryLock(KEY)).thenReturn(true);

        assertThatThrownBy(() -> transferService.createTransfer(request(FROM, FROM, BigDecimal.TEN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be different");

        verifyNoInteractions(accountService);
        verifyNoInteractions(transactionDomainService);
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("chave de idempotência já usada: devolve a transferência existente sem novos lançamentos")
    void shouldReturnExistingTransferWhenKeyAlreadyLocked() {
        Transfer existing = Transfer.builder().id(UUID.randomUUID()).status(TransactionStatus.COMPLETED).build();
        TransferResponse expected = TransferResponse.builder().transferId(existing.getId()).build();
        when(idempotencyService.tryLock(KEY)).thenReturn(false);
        when(idempotencyService.findByKey(KEY)).thenReturn(existing);
        when(transferMapper.toTransferResponse(existing)).thenReturn(expected);

        TransferResponse result = transferService.createTransfer(request(FROM, TO, BigDecimal.TEN));

        assertThat(result).isSameAs(expected);
        verifyNoInteractions(transactionDomainService);
        verifyNoInteractions(accountService);
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("saldo insuficiente na origem: lança InsufficientBalanceException e não persiste transferência")
    void shouldRejectTransferWhenInsufficientBalance() {
        when(idempotencyService.tryLock(KEY)).thenReturn(true);
        stubActiveAccounts();
        when(balanceCalculatorService.calculate(FROM)).thenReturn(new BigDecimal("9.99"));

        assertThatThrownBy(() -> transferService.createTransfer(request(FROM, TO, new BigDecimal("10.00"))))
                .isInstanceOf(InsufficientBalanceException.class);

        verifyNoInteractions(transactionDomainService);
        verify(transferRepository, never()).save(any());
        verifyNoInteractions(balanceProjectionService);
    }

    @Test
    @DisplayName("valor igual ao saldo disponível é permitido")
    void shouldAllowTransferEqualToBalance() {
        BigDecimal amount = new BigDecimal("100.00");
        when(idempotencyService.tryLock(KEY)).thenReturn(true);
        stubActiveAccounts();
        when(balanceCalculatorService.calculate(FROM)).thenReturn(new BigDecimal("100.00"));
        when(transferMapper.toTransferResponse(any(Transfer.class)))
                .thenReturn(TransferResponse.builder().build());

        transferService.createTransfer(request(FROM, TO, amount));

        verify(transactionDomainService).create(eq(FROM), eq(TransactionType.DEBIT), eq(amount), anyString(), any());
    }

    @Test
    @DisplayName("conta de destino não ativa: propaga AccountStatusException e não gera lançamentos")
    void shouldRejectWhenDestinationAccountNotActive() {
        when(idempotencyService.tryLock(KEY)).thenReturn(true);
        when(accountService.findByIdAccountForUpdate(FROM)).thenReturn(account(FROM, AccountStatus.ACTIVE));
        when(accountService.findByIdAccount(TO)).thenReturn(account(TO, AccountStatus.BLOCKED));
        org.mockito.Mockito.lenient()
                .doThrow(new AccountStatusException(TO, AccountStatus.BLOCKED.name()))
                .when(accountValidator)
                .validateStatusAccountForTransaction(TO, AccountStatus.BLOCKED.name());

        assertThatThrownBy(() -> transferService.createTransfer(request(FROM, TO, BigDecimal.TEN)))
                .isInstanceOf(AccountStatusException.class);

        verifyNoInteractions(transactionDomainService);
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("falha ao gerar um lançamento: marca a transferência como FAILED, grava o motivo e repropaga")
    void shouldMarkTransferFailedWhenLegCreationThrows() {
        when(idempotencyService.tryLock(KEY)).thenReturn(true);
        stubActiveAccounts();
        when(balanceCalculatorService.calculate(FROM)).thenReturn(new BigDecimal("200.00"));
        when(transactionDomainService.create(eq(FROM), eq(TransactionType.DEBIT), any(), anyString(), any()))
                .thenThrow(new RuntimeException("ledger indisponível"));

        assertThatThrownBy(() -> transferService.createTransfer(request(FROM, TO, new BigDecimal("10.00"))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ledger indisponível");

        ArgumentCaptor<Transfer> saved = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(saved.getValue().getFailureReason()).isEqualTo("ledger indisponível");
        verifyNoInteractions(balanceProjectionService);
    }
}

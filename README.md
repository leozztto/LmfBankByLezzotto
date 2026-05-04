# LMF Bank API



Sistema bancário digital construído com foco em **consistência financeira, escalabilidade e segurança transacional**, utilizando arquitetura moderna baseada em **ledger + projection + event-driven design**.

# Visão geral

O projeto simula um sistema bancário com suporte a:

- Criação de contas
- Depósitos (CREDIT)
- Saques (DEBIT)
- Transferências entre contas
- Consulta de saldo
- Histórico de transações

## Fluxos principais

### Depósito (CREDIT)

- Validação da conta
- Criação da transação
- Atualização da projeção de saldo

### Saque (DEBIT)

- Validação da conta
- Cálculo de saldo disponível
- Validação de saldo suficiente
- Criação da transação
- Atualização da projeção de saldo

### Transferência

- Validação das contas origem e destino
- Validação de saldo da conta origem
- Criação de duas transações:
    - DEBIT (origem)
    - CREDIT (destino)
- Registro da transferência
- Atualização dos saldos

## Idempotência

Para evitar duplicidade de operações:

- Cada transação possui `idempotencyKey`
- Transfers possuem controle via `TransferIdempotencyService`
- Eventos Kafka possuem controle via `ProcessedEvent`

## Segurança

Autenticação baseada em **JWT**.

### Fluxo:

- Login gera token
- Token enviado via header `Authorization: Bearer`
- Filtro valida token em cada requisição

## Kafka

Utilizado para processamento de eventos de criação de conta.

Características:

- Consumer com ACK manual
- Controle de duplicidade de eventos
- Processamento idempotente

## Banco de dados

Principais entidades:

- Account
- AccountBalance
- Transaction
- Transfer
- ProcessedEvent

## Tecnologias utilizadas

- Java 17+
- Spring Boot
- Spring Security
- Spring Data JPA
- Kafka
- JWT
- MapStruct
- Lombok
- OpenAPI (Swagger)
- Docker
- Kubernetes
- PostgreSql

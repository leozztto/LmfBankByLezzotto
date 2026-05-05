![Java](https://img.shields.io/badge/Java-17-red?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green?logo=springboot)
![Kafka](https://img.shields.io/badge/Kafka-Event%20Streaming-black?logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Container-blue?logo=docker)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Orchestration-326CE5?logo=kubernetes)
![JWT](https://img.shields.io/badge/JWT-Security-orange)

# 💳 LMF Bank API

Sistema bancário digital construído com foco em **consistência financeira, escalabilidade e segurança transacional**, utilizando arquitetura moderna baseada em **ledger + projection + event-driven design**.

---

# 📌 Visão geral

O projeto simula um sistema bancário completo com suporte a:

- 🏦 Criação de contas
- 💰 Depósitos (CREDIT)
- 💸 Saques (DEBIT)
- 🔁 Transferências entre contas
- 📊 Consulta de saldo
- 📜 Histórico de transações

---

# 🔄 Fluxos principais

## 💰 Depósito (CREDIT)

- Validação da conta
- Criação da transação
- Atualização da projeção de saldo

---

## 💸 Saque (DEBIT)

- Validação da conta
- Cálculo de saldo disponível
- Validação de saldo suficiente
- Criação da transação
- Atualização da projeção de saldo

---

## 🔁 Transferência

- Validação das contas (origem e destino)
- Validação de saldo da conta origem
- Criação de duas transações:
  - DEBIT (origem)
  - CREDIT (destino)
- Registro da transferência
- Atualização dos saldos

---

# 🧠 Idempotência

Para evitar duplicidade de operações:

- Cada transação possui `idempotencyKey`
- Transfers possuem controle via `TransferIdempotencyService`
- Eventos Kafka possuem controle via `ProcessedEvent`

---

# 🔐 Segurança

Autenticação baseada em **JWT (JSON Web Token)**.

## 🔑 Fluxo:

- Login gera token JWT
- Token enviado via header:

- Filtro valida token em cada requisição

---

# 📨 Kafka

Utilizado para processamento assíncrono de eventos de criação de conta.

## ⚙️ Características:

- Consumer com ACK manual
- Controle de duplicidade de eventos
- Processamento idempotente

---

# 🗄️ Banco de dados

## 📦 Entidades principais:

- `Account`
- `AccountBalance`
- `Transaction`
- `Transfer`
- `ProcessedEvent`

---

# 🧰 Tecnologias utilizadas

- ☕ Java 17+
- 🌱 Spring Boot
- 🔐 Spring Security
- 🗄️ Spring Data JPA
- 📨 Apache Kafka
- 🔑 JWT
- 🧩 MapStruct
- 🪶 Lombok
- 📄 OpenAPI (Swagger)
- 🐳 Docker
- ☸️ Kubernetes
- 🐘 PostgreSQL

---

# 🚀 Arquitetura

- Event-driven architecture
- Ledger-based financial model
- Projection for optimized balance queries
- Idempotent operations
- Distributed processing via Kafka

---

# 📈 Objetivo do projeto

Simular um **core banking system simplificado**, com foco em:

- consistência de dados
- resiliência a falhas
- escalabilidade horizontal
- prevenção de duplicidade de transações

---
# Hemorede — Sistema de Logística de Hemocomponentes

Projeto Spring Boot (Maven) com o modelo de domínio, repositories, services
(regras de negócio) e controllers REST descritos no design do sistema.

## Como rodar

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`, com banco H2 em memória
(console em `http://localhost:8080/h2-console`).

## Estrutura

```
com.hemorede
├── domain.model      → entidades JPA
├── domain.enums       → enums (HemoComponente, TipoSanguineo, status, etc.)
├── repository          → Spring Data JPA
├── service              → regras de negócio
├── controller           → endpoints REST
└── exception             → exceções de negócio + handler global
```

## Regras de negócio implementadas

1. Compatibilidade sanguínea — `TipoSanguineo.compativel()` + `AlocacaoService`
2. FEFO (menor validade primeiro) — query ordenada em `BolsaRepository` + `AlocacaoService`
3. Bloqueio/descarte de bolsas vencidas — `AlocacaoService` + job `ValidadeService`
4. Intervalo mínimo entre doações — `DoacaoService`
5. Priorização de rotas urgentes — `RoteirizacaoService`
6. Compatibilidade de refrigeração do veículo — `RoteirizacaoService`
7. Alerta de estoque mínimo — `EstoqueService`
8. Aprovação de requisição só com 100% dos itens alocáveis — `RequisicaoService`

## Abrir no VS Code

1. Extraia o `.zip`.
2. Abra a pasta `hemorede` no VS Code.
3. Instale a extensão **Extension Pack for Java** (Microsoft) se ainda não tiver.
4. O VS Code deve reconhecer o `pom.xml` automaticamente e baixar as dependências.

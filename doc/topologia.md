# Topologia de Rede — Rota Vital

**Task:** PI2-16 — W04-Topologia (RSD)
**Responsável:** Micaella Cabral
**Disciplina:** Infraestrutura de Comunicação (RSD)
**Sprint:** W04

Este documento consolida a topologia de rede do sistema **Rota Vital**, evoluindo o esboço inicial registrado em `doc/contratos-api.md` (Seções 2, 3 e 4) para a versão final exigida na entrega de RSD: diagrama de arquitetura, tabela de ligações/protocolos e mapeamento dos requisitos de negócio para requisitos de rede.

---

## 1. Diagrama de Arquitetura de Rede

O diagrama abaixo mostra todos os componentes do sistema — clientes, borda, aplicação, dados, serviços simulados e pipeline de deploy — e como eles se comunicam entre si.

![Diagrama de Arquitetura de Rede — Rota Vital](imagens/topologia_rede.png)

*(Exportado em PNG a partir do desenho de arquitetura; a fonte editável — .drawio/.excalidraw — deve ser versionada em `doc/topologia/` junto a este arquivo.)*

### Visão geral das zonas

| Zona | Componentes | Função |
|------|-------------|--------|
| **1 — Clientes (Internet)** | Navegador (Hospitais), Navegador (Hemocentro), App do Motorista/Operador, Painel Gerencial | Pontos de acesso dos usuários finais às histórias de usuário HU01–HU07 |
| **2 — Borda / Cloud pública** | API Gateway / Load Balancer | Terminação TLS, roteamento e proteção da borda antes da rede privada |
| **3 — Aplicação (VPC privada)** | Spring Boot: Controllers, Services, módulo de Algoritmos (Dijkstra, FEFO) | Lógica de negócio, orquestração dos casos de uso |
| **4 — Dados** | PostgreSQL (Produção) e H2 (Dev, em memória) | Persistência de estoque, requisições, rotas e entregas |
| **5 — Integração/Deploy** | GitHub Actions + Plataforma Cloud | CI/CD: build, testes e publicação automática da aplicação |
| **Serviço simulado** | Telemetria de temperatura (cadeia fria) | Fonte simulada de leituras de sensores consumida internamente pela aplicação |

---

## 2. Tabela de Ligações e Protocolos (versão final)

| Origem | Destino | Protocolo / Porta | Direção | Rede | Segurança | Finalidade |
|--------|---------|--------------------|---------|------|-----------|------------|
| Navegador (Hospital / Hemocentro / Gestor) | API Gateway / Load Balancer | HTTPS (TLS 1.2+) / 443 | Cliente → Servidor (request/response) | Externa (Internet) | Certificado TLS na borda; autenticação da requisição a nível de aplicação | Acesso web às telas de estoque, requisições, rotas e painel |
| App do Motorista/Operador | API Gateway / Load Balancer | HTTPS (TLS 1.2+) / 443 | Cliente → Servidor | Externa (Internet) | TLS + autenticação do operador | Consulta de rota e confirmação de entrega (HU05, HU06) |
| API Gateway / Load Balancer | Aplicação Spring Boot | HTTP / 8080 | Servidor de borda → Aplicação | Interna (VPC privada) | Tráfego restrito à rede privada; sem exposição direta à internet | Encaminhamento das chamadas REST já autenticadas na borda |
| Aplicação Spring Boot | PostgreSQL (Produção) | JDBC/TCP / 5432 | Aplicação → Banco de dados | Interna (VPC privada) | Credenciais via variáveis de ambiente; acesso restrito por *security group*/firewall | Persistência de doações, bolsas, hospitais, requisições e rotas |
| Aplicação Spring Boot | H2 Database (Dev) | Conexão em memória (JDBC local, sem rede) | Processo local | Local (máquina do desenvolvedor) | Não aplicável (dados voláteis, resetados a cada execução) | Execução e testes locais sem infraestrutura externa |
| Aplicação Spring Boot | Serviço de Telemetria (simulada) | HTTP/JSON (REST interno) | Aplicação ↔ Serviço | Interna (VPC privada) | Tráfego interno; sem exposição externa | Leitura simulada de temperatura da cadeia fria (`/api/telemetria/temperatura`) |
| GitHub Actions | Plataforma Cloud (deploy) | HTTPS (API/webhook do provedor) | CI/CD → Ambiente de produção | Externa (Internet, entre serviços de nuvem) | Segredos/tokens armazenados como *secrets* no GitHub | Build, testes automatizados e publicação contínua da aplicação |

**Notas sobre ambientes:**

| Ambiente | Banco de Dados | Observação |
|----------|-----------------|------------|
| **Dev** | H2 (em memória) | Uso local, dados voláteis, sem necessidade de rede externa |
| **Prod** | PostgreSQL (nuvem) | Persistência real, suporta concorrência e múltiplos acessos simultâneos |

---

## 3. Mapeamento de Requisitos de Negócio em Rede

| Necessidade de negócio | História(s) relacionada(s) | Requisito de rede derivado |
|---|---|---|
| Registrar doação e disponibilizar bolsa no estoque com confiabilidade | HU01 | Conexão estável e transacional com o banco (TCP/5432); tráfego cliente-servidor protegido por HTTPS para não expor dados do doador |
| Consultar estoque em tempo hábil, por tipo sanguíneo e validade | HU02 | Baixa latência nas chamadas GET (`/api/estoque`); disponibilidade da aplicação e do banco para leitura consistente |
| Registrar requisição hospitalar identificando o hospital solicitante | HU03 | Canal autenticado hospital → aplicação (HTTPS); validação do status do hospital exige round-trip confiável com o banco |
| Alocar bolsas compatíveis priorizando validade (FEFO) sem conflito de concorrência | HU04 | Integridade transacional (ACID) na conexão com o banco; não há tráfego externo adicional além da chamada REST que dispara a alocação |
| Calcular rota de menor custo até o hospital respeitando a cadeia fria | HU05 | Processamento server-side (Dijkstra) sem dependência de rede externa hoje; necessidade futura de integração com serviço de mapas/geolocalização via HTTPS |
| Confirmar entrega em campo, possivelmente com conectividade instável | HU06 | Acesso do app do motorista via HTTPS/443 tolerante a intermitência (retentativas); necessidade futura de estratégia offline-first |
| Visualizar indicadores de estoque, requisições e entregas para gestão | HU07 | Consultas agregadas de leitura (possível *read pool* dedicado); tráfego HTTPS com maior volume de dados para dashboards |
| Garantir integridade da cadeia fria (temperatura) das bolsas em trânsito | Não-funcional (SO/RSD) | Integração interna HTTP/JSON com o serviço de telemetria; caminho futuro para sensores IoT reais (ex.: MQTT) |
| Proteger dados sensíveis de pacientes/hospitais em trânsito e em repouso | Não-funcional (segurança) | TLS obrigatório ponta a ponta na borda; banco de dados isolado em rede privada (VPC), sem exposição direta à internet |
| Garantir disponibilidade do sistema (rede de sangue é serviço crítico) | Não-funcional (disponibilidade) | Load Balancer/API Gateway com *health checks*; possibilidade de múltiplas instâncias da aplicação atrás do balanceador |
| Rastrear e auditar as entregas realizadas | HU06, HU07 | Necessidade futura de centralização de logs e monitoramento de rede (próximo passo, fora do escopo desta entrega) |

---

## 4. URL da Aplicação em Produção

🔲 **Pendente.** Conforme o `README.md` do projeto, a seção "Como Rodar o Projeto" e o deploy em nuvem estão previstos a partir da **Entrega 02 (21/09/2026)**. O ambiente de produção descrito em `doc/contratos-api.md` (Seção 4) prevê **PostgreSQL hospedado em nuvem**, mas a plataforma de hospedagem (ex.: Render, Railway ou similar) ainda não foi definida/publicada pela equipe.

> **Ação para a equipe:** assim que o deploy da Entrega 02 for concluído, atualizar esta seção com a URL pública da aplicação, por exemplo:
>
> `URL de Produção: https://rota-vital.<provedor-escolhido>.app`

---

## Fontes

- [`doc/contratos-api.md`](../doc/contratos-api.md) — versão inicial do diagrama de topologia, tabela de protocolos e ambientes de execução
- [`doc/historias_de_usuario.md`](../doc/historias_de_usuario.md) — histórias de usuário (HU01–HU07) usadas no mapeamento negócio → rede
- [`doc/escopo-grafo.md`](../doc/escopo-grafo.md) — escopo do grafo de rotas (Dijkstra) referenciado na Zona 3
- [`README.md`](../README.md) — status de deploy e ambientes do projeto

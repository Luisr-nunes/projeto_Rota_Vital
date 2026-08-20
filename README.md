<h1 align="center">Rota Vital</h1>

<p align="center">
  <b>Gestão e distribuição de hemocomponentes na rede de sangue</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/HTML-CSS-E34F26?style=for-the-badge&logo=html5&logoColor=white" alt="HTML/CSS"/>
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License"/>
  <img src="https://img.shields.io/badge/Status-Em%20Desenvolvimento-yellow?style=for-the-badge" alt="Status"/>
</p>

<p align="center">
  <a href="#sobre-o-projeto">Sobre</a> •
  <a href="#problema">Problema</a> •
  <a href="#solução">Solução</a> •
  <a href="#tecnologias-utilizadas">Tecnologias</a> •
  <a href="#como-rodar-o-projeto">Como Rodar</a> •
  <a href="#entregas">Entregas</a> •
  <a href="#equipe">Equipe</a>
</p>

---

## Sobre o Projeto

O **Rota Vital** é o Projeto Integrador do 3º semestre do curso de **Análise e Desenvolvimento de Sistemas** da **CESAR School (2026.2)**. Trata-se de uma aplicação web em **Java/Spring Boot** que apoia a rede de sangue na **gestão e distribuição de hemocomponentes**.

O sistema é inspirado no fluxo da **Hemorrede/SUS** (centros de coleta e doação → hemocentro de processamento e controle → estoque → hospitais) e utiliza exclusivamente **dados sintéticos**.

### Disciplinas envolvidas

| Disciplina | Contribuição no Projeto |
|---|---|
| **Programação Orientada a Objetos (POO)** | Núcleo da aplicação web em Java/Spring Boot |
| **Algoritmos e Estruturas de Dados (AED)** | Roteirização (Dijkstra), compatibilidade ABO/Rh, FEFO |
| **Estatística e Probabilidade (EST)** | Indicadores, painéis e análise probabilística |
| **Infraestrutura de Software (SO)** | CI/CD, deploy em nuvem, concorrência (threads) |
| **Infraestrutura de Comunicação (RSD)** | Arquitetura de rede, telemetria, monitoramento |
| **Projeto 3** | Gestão de processo, integração e apresentação final |

---

## Problema

A rede de sangue precisa garantir o **componente certo** (compatível e dentro da validade), **no lugar certo**, **no tempo certo** e **na temperatura certa**. Falhas nesse processo geram:

- Desabastecimento de hemocomponentes
- Descarte por vencimento de validade
- Risco direto ao paciente

**Falta uma plataforma que integre estoque, compatibilidade, roteirização e monitoramento da cadeia fria.**

---

## Solução

O **Rota Vital** é uma aplicação web que:

- **Gerencia o estoque** de hemocomponentes por tipo e componente sanguíneo
- **Recebe requisições** dos hospitais
- **Aloca bolsas compatíveis** (ABO/Rh) priorizando a validade (**FEFO** — *First Expired, First Out*)
- **Calcula rotas de distribuição** respeitando a cadeia fria e as janelas de tempo (Dijkstra)
- **Monitora temperatura e rede** em painéis de indicadores

---

## Tecnologias Utilizadas

| Tecnologia | Uso |
|---|---|
| **Java** | Linguagem principal |
| **Spring Boot** | Framework web (Controllers, Services, Repositories) |
| **HTML / CSS** | Interface web (Thymeleaf ou API REST + Frontend) |
| **Banco de Dados** | Persistência de dados (a definir pela equipe) |
| **Jira** | Gestão e controle de atividades do projeto |
| **GitHub** | Versionamento de código e Issue/Bug Tracker |
| **Figma** | Prototipação Lo-Fi |
| **YouTube** | Screencasts das entregas |

> **Observação:** O uso de Lombok ou qualquer outro mecanismo de geração de código boilerplate **NÃO é permitido**.

---

## Como Rodar o Projeto

> *Seção a ser preenchida a partir da Entrega 02.*

<!--
### Pré-requisitos
- Java 17+
- Maven
- Banco de dados (ex: PostgreSQL / MySQL / H2)

### Instalação
```bash
# Clone o repositório
git clone https://github.com/Luisr-nunes/projeto_Rota_Vital.git

# Acesse a pasta do projeto
cd projeto_Rota_Vital

# Instale as dependências e rode
./mvnw spring-boot:run
```

### Acesso
- Aplicação: `http://localhost:8080`
-->

---

## Entregas

### Entrega 01

**Data de entrega:** 31/08/2026

#### Artefatos

- [ ] **Histórias de Usuário** — Mínimo de 7 histórias bem definidas (formato BDD, arquivo `.md` no GitHub)
  - [Link para as Histórias](./docs/historias.md) <!-- ajustar link -->
- [ ] **Protótipo Lo-Fi (Figma)** — Mínimo de 5 histórias prototipadas
  - [Link para o Figma](#) <!-- inserir link do Figma -->
- [ ] **Screencast do Protótipo** — Vídeo no YouTube explicando o protótipo Figma (com áudio ou legenda)
  - [Assistir no YouTube](#) <!-- inserir link do YouTube -->

---

### Entrega 02

**Data de entrega:** 21/09/2026

#### Histórias Implementadas

> *Adicionar aqui a descrição (formato POST-IT) das histórias implementadas nesta entrega.*

#### Artefatos

- [ ] Ao menos **2 histórias implementadas** (Spring Boot rodando)
- [ ] Ambiente de versionamento atuante (commits frequentes, no mínimo semanais)
- [ ] Issue/Bug Tracker atualizado (GitHub Issues)
  - *Print do Bug Tracker:* <!-- inserir screenshot -->
- [ ] **Screencast do sistema** — Vídeo no YouTube demonstrando as histórias implementadas (com áudio ou legenda)
  - [Assistir no YouTube](#) <!-- inserir link -->
- [ ] **Screencast do código** — Vídeo no YouTube explicando o código Spring Boot (com áudio ou legenda)
  - [Assistir no YouTube](#) <!-- inserir link -->

---

### Entrega 03

**Data de entrega:** 19/10/2026

#### Histórias Implementadas

> *Adicionar aqui a descrição (formato POST-IT) das histórias implementadas nesta entrega.*

#### Artefatos

- [ ] Mais **2 histórias implementadas**
- [ ] Commits frequentes (mínimo semanais)
- [ ] Issue/Bug Tracker atualizado
  - *Print do Bug Tracker:* <!-- inserir screenshot -->
- [ ] **Screencast do sistema** — Novas histórias implementadas (com áudio ou legenda)
  - [Assistir no YouTube](#) <!-- inserir link -->
- [ ] **Screencast do código** — Explicação do código das novas histórias (com áudio ou legenda)
  - [Assistir no YouTube](#) <!-- inserir link -->

---

### Entrega 04

**Data de entrega:** 09/11/2026

#### Histórias Implementadas

> *Adicionar aqui a descrição (formato POST-IT) das histórias implementadas nesta entrega.*

#### Artefatos

- [ ] Implementação das **histórias restantes** (mínimo de 2)
- [ ] Commits frequentes (mínimo semanais)
- [ ] Issue/Bug Tracker atualizado
  - *Print do Bug Tracker:* <!-- inserir screenshot -->
- [ ] **Screencast do sistema** — Ênfase nas novas histórias (com áudio ou legenda)
  - [Assistir no YouTube](#) <!-- inserir link -->
- [ ] **Screencast do código** — Explicação do código das histórias implementadas (com áudio ou legenda)
  - [Assistir no YouTube](#) <!-- inserir link -->

---

### Apresentação Final

**Data:** 09/11 a 13/11/2026 (dia da aula)

Resumo de até **8 minutos** abordando:

| Tópico | Descrição |
|---|---|
| **O Problema** | O que o produto busca resolver |
| **A Solução** | Características, público-alvo, histórias implementadas, diferencial |
| **Fluxo de Trabalho** | Planejamento, requisitos, desenvolvimento, gerência de configuração, testes |
| **Ferramentas** | Ferramentas utilizadas (incluindo links) |
| **Lições Aprendidas** | O que aprendemos com o processo |
| **Demonstração** | Demonstração rápida do produto funcionando |

> **Todos os membros do grupo devem estar presentes na apresentação.**

---

## Equipe

| Função | Matrícula | Responsável | E-mail |
|---|---|---|---|
| Tech Lead | 2025200261 | Lucas Henrique Gomes Medeiros | lhgm@cesar.school |
| Gerente de Projetos | 2025200183 | Luis Felipe Farias Nunes | lffn@cesar.school |
| Desenvolvedor Backend | 2025200142 | João Pedro Cavalcanti Souza | jpcs2@cesar.school |
| Desenvolvedor Backend | 2025200197 | Luis Lucena Wanderley G. | llwg@cesar.school |
| Desenvolvedor Backend | 2025200043 | Matheus Rodrigues Larré | mrl2@cesar.school |
| Engenheiro de Dados | 2025200311 | Micaella Maria Barbosa Cabral | mmbc2@cesar.school |
| Desenvolvedor Frontend | 2025200139 | Mariana Xavier Bezerra | mxb@cesar.school |

---

## Histórico de Membros

| Nome | E-mail | Data de Entrada | Data de Saída | Observação |
|---|---|---|---|---|
| — | — | — | — | *Nenhuma alteração até o momento.* |

---

## Referências

- [A crash course on writing a better README](https://hackernoon.com/a-crash-course-on-writing-a-better-readme-d796d1f6b352)
- [Make a README](https://www.makeareadme.com/)
- [Hall of Fame — Sourcerer](https://github.com/sourcerer-io/hall-of-fame)

---

<p align="center">
  <b>CESAR School — ADS 2026.2 — 3º Semestre</b><br/>
  Projeto Integrador — Rota Vital
</p>


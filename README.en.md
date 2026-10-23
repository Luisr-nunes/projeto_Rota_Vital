<h1 align="center">Rota Vital</h1>

<p align="center">
  <b>Management and distribution of blood components across the blood supply network</b>
</p>

<p align="center">
  <a href="README.md">Ler em Português</a>
</p>

<p align="center">
  <img src="https://img.shields.io/github/actions/workflow/status/Luisr-nunes/projeto_Rota_Vital/ci.yml?branch=main&style=for-the-badge&logo=github&label=CI%2FCD" alt="CI/CD"/>
  <img src="https://img.shields.io/badge/Java-Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/Status-In%20development-yellow?style=for-the-badge" alt="Status"/>
</p>

## About

**Rota Vital** is the 3rd-semester capstone project of the Systems Analysis and Development program at **CESAR School (2026.2)**. It is a **Java/Spring Boot** web application that supports the blood network in **managing and distributing blood components**.

The system is inspired by the flow of Brazil's public blood network (Hemorrede/SUS: collection and donation centers → processing and quality control → inventory → hospitals) and uses **synthetic data only**.

**Live:** [rota-vital-jnmx.onrender.com](https://rota-vital-jnmx.onrender.com)

### Courses involved

| Course | Contribution |
|---|---|
| Object-Oriented Programming | Core Java/Spring Boot web application |
| Algorithms and Data Structures | Routing (Dijkstra), ABO/Rh compatibility, FEFO |
| Statistics and Probability | Indicators, dashboards and probabilistic analysis |
| Software Infrastructure | CI/CD, cloud deploy, concurrency (threads) |
| Communication Infrastructure | Network architecture, telemetry, monitoring |
| Project 3 | Process management, integration and final presentation |

## Problem

The blood network has to deliver the **right component** (compatible and within its shelf life) to the **right place**, at the **right time** and at the **right temperature**. Failures lead to shortages, units discarded after expiring and direct risk to patients. What is missing is a platform that ties together inventory, compatibility, routing and cold-chain monitoring.

## Solution

Rota Vital:

- **manages the inventory** of blood components by blood type and component;
- **receives requests** from hospitals;
- **allocates compatible units** (ABO/Rh), prioritizing the closest expiry date (**FEFO**, First Expired, First Out);
- **computes delivery routes** that respect the cold chain and time windows (Dijkstra);
- **monitors temperature and the network** in indicator dashboards.

## Tech stack

| Technology | Use |
|---|---|
| Java 17 + Spring Boot 3.3 | Web, validation, Data JPA, Actuator |
| HTML / CSS / JavaScript | Static UI (`index.html` and `indicadores.html`) consuming the REST API |
| PostgreSQL / H2 | PostgreSQL in Docker and production (Supabase); in-memory H2 for local development and fast tests |
| Cucumber + Testcontainers | Gherkin acceptance tests and integration tests against a real PostgreSQL |
| Docker + Compose | Multi-stage image and local stack |
| GitHub Actions + Render | CI/CD and deploy |
| [Jira](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2), GitHub Issues, Figma, YouTube | Planning, tracking, lo-fi prototype and screencasts |

Lombok or any other boilerplate generator is **not allowed** in this project.

## Running locally

Requirements: Java 17+, Maven 3.8+ and Git.

```bash
git clone https://github.com/Luisr-nunes/projeto_Rota_Vital.git
cd projeto_Rota_Vital/rotavital
mvn spring-boot:run
```

Or build and run the JAR:

```bash
mvn clean package -DskipTests
java -jar target/hemorede-0.0.1-SNAPSHOT.jar
```

| Access | URL |
|---|---|
| Application / API | `http://localhost:8080` |
| Indicators dashboard | `http://localhost:8080/indicadores.html` |
| Health check | `http://localhost:8080/actuator/health` |
| H2 console | `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:hemorede`, user `sa`, empty password) |

### Running with Docker

Starts the API and PostgreSQL together, without installing Java, Maven or a database. Requires Docker with Compose v2.

```bash
cp .env.example .env          # then set POSTGRES_PASSWORD
docker compose up -d --build  # builds the image (runs the tests) and starts both services
docker compose ps             # db and api should become "healthy"
curl http://localhost:8080/actuator/health
```

Stop with `docker compose down` (data stays in the `pgdata` volume); `docker compose down -v` also deletes the data.

| Variable (`.env`) | Purpose | Default |
|---|---|---|
| `POSTGRES_PASSWORD` | Database password, **required** | — |
| `POSTGRES_DB` / `POSTGRES_USER` | Database and user name | `rotavital` |
| `API_PORT` | API port on your machine | `8080` |
| `HEMOREDE_SEED_ENABLED` | Load synthetic data on first start | `true` |

The image is multi-stage (build and tests on Maven + JDK 17, runtime on the JRE only, non-root user), the API waits for PostgreSQL to be healthy, the database is only reachable inside the Compose network and secrets stay out of the code.

## Tests

```bash
cd rotavital
mvn clean test      # unit, H2 integration and BDD; no Docker needed
mvn clean verify    # plus *IT tests against a real PostgreSQL (Testcontainers); needs Docker
```

| Level | Coverage |
|---|---|
| Unit | Data structures (FEFO, inventory index, graph/Dijkstra), donation interval, indicators and telemetry |
| Integration (H2) | Donation, request, inventory, route and indicator endpoints, and the allocation flow writing to the database |
| Integration (PostgreSQL via Testcontainers) | Request and allocation flow (ABO/Rh, FEFO, expiry, rollback), approval API and synthetic data load |
| Acceptance (Gherkin BDD) | HU08 (compatible allocation + FEFO + route) and HU06 (delivery confirmation) |

## Pipeline and deploy

- **CI:** GitHub Actions runs `mvn -B verify` on every push and pull request; `main` is protected and only accepts merges with the `build-and-test` check green.
- **Deploy:** Render builds the Dockerfile (which runs the tests) and publishes only after CI passes, switching traffic once `/actuator/health` returns 200.
- **Rollback:** Render → Events → Rollback, or `git revert <hash>` and push. Details in [`doc/pipeline-deploy.md`](doc/pipeline-deploy.md) and evidence in [`doc/evidencias/`](doc/evidencias/README.md).

The full decision table and the AI usage log are in the Portuguese README.

## Deliverables

| Deliverable | Date | Content |
|---|---|---|
| 01 | 2026-08-31 | [User stories](doc/historias_de_usuario.md), [lo-fi Figma prototype](https://www.figma.com/site/snA4pKexELM7gW9JGih8iu/rota-vital-lofi?t=bL7QBDZxJXV2m1Pe-6), [prototype screencast](https://www.youtube.com/watch?v=-Tl4rPthJ5Q) |
| 02 | 2026-09-21 | HU04 (compatible FEFO allocation) and HU05 (lowest-cost route), [system](https://youtu.be/GGAWzfbqRF8) and [code](https://youtu.be/SCN13gswS3U) screencasts |
| U1 | 2026-10-02 | Integrated Unit 1 wrap-up with evidence per course (C data structures in [`aed/u1/c`](aed/u1/c), statistics reports, concurrency report, [network topology](doc/topologia.md), [API contracts](doc/contratos-api.md), [telemetry](doc/telemetria.md)) |
| 03 | 2026-10-19 | HU01 (donation and unit registration) and HU03 (hospital request), [system](https://www.youtube.com/watch?v=YsbCOH_n_zY) and [code](https://www.youtube.com/watch?v=5QhViFcMsw4) screencasts |
| 04 | 2026-11-09 | Remaining stories (in progress) |

## Team

| Role | Member |
|---|---|
| Tech Lead | Lucas Henrique Gomes Medeiros |
| Project Manager | Luis Felipe Farias Nunes |
| Back-end Developer | João Pedro Cavalcanti Souza |
| Back-end Developer | Luis Lucena Wanderley G. |
| Back-end Developer | Matheus Rodrigues Larré |
| Data Engineer | Micaella Maria Barbosa Cabral |
| Front-end Developer | Mariana Xavier Bezerra |

<p align="center">
  <b>CESAR School — ADS 2026.2 — 3rd semester</b><br/>
  Capstone Project — Rota Vital
</p>

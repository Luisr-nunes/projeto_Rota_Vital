# ---------- Estágio 1: build + testes ----------
# Imagem com Maven + JDK 17: compila, roda os testes (H2 em memória) e gera o .jar.
# Se um teste falhar, o build da imagem falha e nada é publicado.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia só o pom primeiro: as dependências ficam em cache enquanto o pom não mudar.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# -DskipITs: os testes *IT (PostgreSQL real via Testcontainers, PI2-191) precisam de Docker, que não
# existe dentro de um "docker build". Eles rodam no GitHub Actions (mvn verify) antes do deploy.
RUN mvn -B verify -DskipITs

# ---------- Estágio 2: runtime ----------
# Só o JRE 17 e o .jar: sem Maven, sem JDK, sem código-fonte.
FROM eclipse-temurin:17-jre

# curl para o healthcheck (GET /actuator/health)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuário sem privilégios: a aplicação não roda como root.
RUN useradd --system --no-create-home app

WORKDIR /app
COPY --from=build --chown=app:app /app/target/hemorede-0.0.1-SNAPSHOT.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

# Comparação opcional: virtual threads (Java 21)

Este diretório contém **só** a comparação opcional pedida no item "para destaque" da entrega de
SO ("repitam a medição com as virtual threads do Java 21 e comparem com as threads de
plataforma"). Ela fica **fora** de `src/main/java` de propósito: o projeto Rota Vital continua em
**Java 17** (build normal, CI, `mvn package`); só essa comparação específica precisa de um JDK 21
instalado na máquina de quem for rodá-la.

## Por quê isolado assim

`Executors.newVirtualThreadPerTaskExecutor()` só existe na API do Java 21. Se essa chamada
estivesse dentro de `src/main/java`, o módulo inteiro deixaria de compilar com
`--release 17` (a versão travada no `pom.xml`) e quebraria o CI. Por isso ela vive num arquivo à
parte, no mesmo pacote (`com.hemorede.relatorio`) para poder reaproveitar as classes internas do
serviço (`AgregadoHistorico`, `GeradorHistoricoRequisicoes`), mas compilada e executada
**manualmente**, nunca pelo `mvn test`/`mvn package`/pipeline.

O requisito obrigatório da entrega, sequencial + threads (2, 4, 8) devolvendo a mesma resposta,
já está 100% coberto em Java 17 por `RelatorioHistoricoService` (`src/main/java/...`) e testado em
`RelatorioHistoricoServiceTest`. Isso aqui é só o "bônus" opcional.

## Como rodar (precisa de JDK 21 instalado)

```bash
cd rotavital
mvn -DskipTests package    # gera target/classes com o projeto normal (Java 17)

javac --release 21 -cp target/classes \
    -d /tmp/vt-out \
    scripts/virtual-threads/src/com/hemorede/relatorio/ComparacaoVirtualThreads.java

java --class-path "target/classes:/tmp/vt-out" \
    com.hemorede.relatorio.ComparacaoVirtualThreads 100000 1000000
```

O programa gera os históricos sintéticos (mesmo gerador determinístico do endpoint), roda
sequencial, threads de plataforma (4 e 8) e virtual threads (8 e 64 fatias), confere que todas as
respostas batem byte a byte com a sequencial e imprime os tempos de cada uma. Cada configuração
passa por 8 chamadas de aquecimento (descartadas, para o JIT compilar os hot paths e a JVM
estabilizar) e depois 11 chamadas medidas, reportando a **mediana**, mesma metodologia do
benchmark HTTP em Python (`benchmark_relatorio_historico.py`), para não medir custo de
interpretação/JIT frio em vez do custo real da operação.

Resultado de exemplo (rodado nesta sessão, sandbox de 2 núcleos, 100 mil / 1 milhão / 5 milhões de
registros):

```
=== tamanho=100000 (aquecimento=8, repeticoes=11, mediana reportada) ===
  sequencial       : 5 ms
  plataforma(4)    : 3 ms
  plataforma(8)    : 5 ms
  virtual(8)       : 2 ms
  virtual(64)      : 2 ms

=== tamanho=1000000 (aquecimento=8, repeticoes=11, mediana reportada) ===
  sequencial       : 28 ms
  plataforma(4)    : 22 ms
  plataforma(8)    : 22 ms
  virtual(8)       : 13 ms
  virtual(64)      : 20 ms

=== tamanho=5000000 (aquecimento=8, repeticoes=11, mediana reportada) ===
  sequencial       : 136 ms
  plataforma(4)    : 80 ms
  plataforma(8)    : 74 ms
  virtual(8)       : 70 ms
  virtual(64)      : 72 ms

Todas as versões bateram com a sequencial, sem race condition.
```

Nos tamanhos menores (100 mil) o custo fixo de criar threads/tarefas domina e os números ficam
próximos de ruído de agendamento do SO, por isso o benchmark principal da entrega usa tamanhos
maiores (100 mil a 5 milhões) e a mediana de várias repetições. No maior volume (5 milhões), as
virtual threads ficam no mesmo patamar das threads de plataforma nesta operação, o que é esperado,
já que o trabalho aqui é 100% CPU-bound (sem I/O bloqueante), que é justamente o cenário em que
virtual threads não trazem vantagem sobre threads de plataforma (a vantagem delas aparece quando há
muito mais tarefas do que núcleos e as tarefas bloqueiam em I/O).

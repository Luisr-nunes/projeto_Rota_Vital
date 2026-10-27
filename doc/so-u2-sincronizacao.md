# Sistemas Operacionais U2 — Agregador Sincronizado de Telemetria, Condição de Corrida e Benchmark

**Disciplina:** Infraestrutura de Software (Sistemas Operacionais — SO)
**Atividade:** PI2-169 (subtarefa da PI2-30 — Telemetria concorrente da cadeia fria)
**Responsável:** Matheus Rodrigues Larré
**Data:** 27/10/2026
**Relacionados:** PI2-168 (fila de ingestão de leituras), PI2-170 (simulador de veículos), PI2-121 (`TelemetriaService`)

---

## 1. Objetivo e Contexto

No transporte de hemocomponentes da Hemorrede (**Rota Vital**), os veículos refrigerados enviam leituras contínuas de temperatura durante o trajeto entre o hemocentro e os hospitais. Para bolsas de **hemácias refrigeradas** (`REFRIGERADO_2_6`), a cadeia fria exige manutenção estrita na faixa de **2 °C a 6 °C** (limites inclusos); qualquer leitura `< 2,0 °C` ou `> 6,0 °C` configura **alerta de excursão térmica** (`ALERTA`).

Quando múltiplos sensores/veículos (ou múltiplas threads consumidoras da fila de ingestão) gravam leituras simultaneamente em memória para compor o painel por veículo, o estado compartilhado (totais de leituras, totais de alertas, soma, mínimo e máximo de temperatura) torna-se uma **seção crítica**.

Esta entrega (**C2 · PI2-169**) cumpre três objetivos:

1. **Provar a condição de corrida (*race condition*)** quando múltiplas threads gravam leituras sem sincronização (histórico TDD *Red → Green* e teste comparativo automatizado).
2. **Implementar o agregador thread-safe por veículo (`AgregadorTelemetria`)** combinando `ConcurrentHashMap`, `LongAdder`, `DoubleAdder` e operações atômicas *Compare-And-Swap* (CAS), contabilizando alertas fora de **2–6 °C** sem perda de contagem.
3. **Medir o desempenho sequencial × concorrente** (tempo em milissegundos, *throughput* em leituras/segundo e *speedup*) garantindo paridade exata dos resultados.

---

## 2. Condição de Corrida (Race Condition)

### 2.1 Por que a versão sem sincronização perde leituras

Na versão inicial sem sincronização (commit `6d9df01`), o agregador utilizava um `HashMap<Long, AcumuladorVeiculo>` e contadores primitivos `long`:

```java
// Operação não-atômica (Read-Modify-Write)
totalLeituras++;
if (alerta) {
    totalAlertas++;
}
```

Ao nível de instruções de máquina/bytecode (`getfield` → `ladd` → `putfield`), o incremento `totalLeituras++` compreende três etapas distintas:
1. Ler o valor atual de `totalLeituras` da memória para um registrador da thread.
2. Somar `1` ao valor lido.
3. Escrever o resultado de volta na memória.

Quando $N$ threads executam esse trecho concorrentemente sobre o mesmo veículo (ou sobre o contador global), duas ou mais threads leem o **mesmo valor base** antes que qualquer uma delas conclua a escrita. As escritas subsequentes sobrescrevem umas às outras (*lost updates*). Além disso, inserções simultâneas num `HashMap` não sincronizado durante redimensionamento (*rehash*) podem corromper buckets inteiros.

### 2.2 Evidência do teste falhando sem sincronização (TDD — Red)

No commit `6d9df01` (`test(telemetria): N threads gravando leituras não perdem contagem (falha sem sincronização) (PI2-169)`), o teste `AgregadorTelemetriaTest` disparou **16 threads simultâneas** alinhadas por um `CountDownLatch` para maximizar a contenção:

| Cenário de Teste | Threads | Leituras por Thread | Total Esperado | Valor Obtido (Sem Sincronização) | Leituras Perdidas | Perda (%) |
|---|---:|---:|---:|---:|---:|---:|
| Mesmo veículo (`veiculoId = 1`) | 16 | 10.000 | 160.000 | **34.960** | 125.040 | **78,15%** |
| Múltiplos veículos (8 veículos) | 16 | 8.000 | 128.000 | **31.920** | 96.080 | **75,06%** |

Saída registrada pelo Maven Surefire no estágio *Red*:

```text
[INFO] Running com.hemorede.service.AgregadorTelemetriaTest
[ERROR] Tests run: 2, Failures: 2, Errors: 0, Skipped: 0, Time elapsed: 0.081 s <<< FAILURE! -- in com.hemorede.service.AgregadorTelemetriaTest
[ERROR] com.hemorede.service.AgregadorTelemetriaTest.nThreadsGravandoEmMultiplosVeiculosNaoPerdemContagem -- Time elapsed: 0.040 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <128000> but was: <31920>

[ERROR] com.hemorede.service.AgregadorTelemetriaTest.nThreadsGravandoNoMesmoVeiculoNaoPerdemContagem -- Time elapsed: 0.021 s <<< FAILURE!
org.opentest4j.AssertionFailedError: Total global de leituras não pode sofrer perda por condição de corrida ==> expected: <160000> but was: <34960>
```

---

## 3. Arquitetura do Agregador Sincronizado (`AgregadorTelemetria`)

Implementado em `rotavital/src/main/java/com/hemorede/service/AgregadorTelemetria.java` (commit `e3b6961`) e integrado ao `TelemetriaService`.

### 3.1 Estruturas concorrentes adotadas

| Componente | Estrutura Utilizada | Papel e Justificativa Técnica |
|---|---|---|
| Mapa por veículo | `ConcurrentHashMap<Long, AcumuladorVeiculo>` | Garante criação e recuperação atômica do acumulador de cada veículo via `computeIfAbsent(veiculoId, ...)`, com trava fina por bucket e leituras *lock-free*. |
| Contagem de leituras e alertas | `LongAdder` (`totalLeituras`, `totalAlertas`) | Superior ao `AtomicLong` sob alta contenção de escrita: mantém um valor `base` e, ao detectar colisão de CAS entre threads, distribui os incrementos em um array de células (`Cell[]`) com *striping* por hash da thread, evitando invalidação constante da mesma linha de cache L1/L2 (*cache line bouncing*). A soma final é obtida em `sum()`. |
| Soma de temperaturas | `DoubleAdder` (`somaTemperaturaC`) | Mesma estratégia de *cell striping* do `LongAdder` aplicada a ponto flutuante `double`, permitindo calcular a temperatura média por veículo sem bloqueio (`synchronized`). |
| Temperaturas mínima e máxima | `AtomicLong` + loop CAS (`Double.doubleToLongBits`) | Converte o `double` para representação binária IEEE 754 de 64 bits e atualiza o extremo apenas se a nova leitura for menor (para o mínimo) ou maior (para o máximo), usando `compareAndSet` sem bloqueio de threads. |

### 3.2 Regra de alerta fora de 2–6 °C

Conforme `doc/telemetria.md`, para o monitoramento padrão da cadeia fria de hemácias:

- **Faixa segura:** $2,0\ ^\circ\text{C} \le T \le 6,0\ ^\circ\text{C}$ → status `OK` (`alerta = false`).
- **Excursão térmica:** $T < 2,0\ ^\circ\text{C}$ ou $T > 6,0\ ^\circ\text{C}$ → status `ALERTA` (`alerta = true`), incrementando `totalAlertas` do veículo e `totalAlertasGlobal`.

### 3.3 Evidência dos testes passando com sincronização (TDD — Green)

Após a refatoração para `ConcurrentHashMap` + `LongAdder`, todos os testes de condição de corrida e de fronteira (2–6 °C) em `AgregadorTelemetriaTest` passaram com **0% de perda**:

| Teste em `AgregadorTelemetriaTest` | Threads × Leituras | Esperado | Obtido (`AgregadorTelemetria`) | Status |
|---|---:|---:|---:|---|
| `nThreadsGravandoNoMesmoVeiculoNaoPerdemContagem` | 16 × 10.000 | 160.000 leituras / 80.000 alertas | **160.000 / 80.000** | PASS |
| `nThreadsGravandoEmMultiplosVeiculosNaoPerdemContagem` | 16 × 8.000 | 128.000 leituras / 32.000 alertas | **128.000 / 32.000** | PASS |
| `deveComprovarCondicaoDeCorridaSemSincronizacaoVsZeroPerdaComSincronizacao` | 16 × 20.000 | 320.000 | **320.000** (vs `< 320.000` sem sync) | PASS |
| `deveDetectarAlertaForaDe2a6Graus` (parametrizado, 8 casos) | Fronteiras `2.0`, `6.0`, `1.99`, `6.01`, etc. | Classificação exata `OK` / `ALERTA` | **8/8 corretos** | PASS |

---

## 4. Benchmark: Sequencial vs Concorrente

### 4.1 Metodologia

Implementado em `rotavital/src/test/java/com/hemorede/service/AgregadorTelemetriaBenchmarkTest.java`:

- **Carga sintética determinística:** `500.000` leituras pré-geradas em memória (semente fixa `Random(42)`) distribuídas entre `20` veículos, com temperaturas normais em torno de $4,0\ ^\circ\text{C}$ ($\sigma = 1,5\ ^\circ\text{C}$), gerando leituras dentro e fora da faixa de 2–6 °C. O tempo de geração da massa **não** entra na medição.
- **Aquecimento (Warmup):** 3 rodadas completas (sequencial e concorrente) antes de iniciar o cronômetro para estabilizar a compilação JIT do HotSpot.
- **Medição:** 5 repetições por configuração (`System.nanoTime()`), reportando a **mediana** do tempo (ms), o **throughput** ($\text{leituras}/\text{s}$) e o **speedup** ($T_{\text{seq}} / T_{\text{conc}}$).
- **Verificação de corretude em todas as rodadas:** a cada execução concorrente (2, 4, 8 e 16 threads), o teste confere com `assertEquals` que o total global de leituras, o total global de alertas e o resumo individual de cada um dos 20 veículos (leituras, alertas, mínimo, máximo e média) são idênticos aos da execução sequencial.

### 4.2 Resultados medidos

Ambiente de execução: JVM OpenJDK 64-Bit Server VM (20 processadores lógicos reportados por `Runtime.getRuntime().availableProcessors()`), Windows, Maven Surefire.

| Modo | Threads | Leituras Processadas | Tempo Mediano (ms) | Throughput (leituras/s) | Speedup |
|---|---:|---:|---:|---:|---:|
| **Sequencial** | 1 | 500.000 | **13,61** | **36.733.914** | **1,00×** |
| **Concorrente** | 2 | 500.000 | **11,53** | **43.369.272** | **1,18×** |
| **Concorrente** | 4 | 500.000 | **9,97** | **50.156.991** | **1,37×** |
| **Concorrente** | 8 | 500.000 | **8,42** | **59.404.294** | **1,62×** |
| **Concorrente** | 16 | 500.000 | **6,73** | **74.287.582** | **2,02×** |

### 4.3 Análise de desempenho

1. **Ganho consistente e monotônico de throughput:** O processamento concorrente reduziu o tempo mediano de **13,61 ms** (1 thread) para **6,73 ms** (16 threads), mais que dobrando a vazão de **36,7 milhões** para **74,3 milhões de leituras/s** (**speedup de 2,02×**).
2. **Por que o speedup é sublinear (2,02× com 16 threads — Lei de Amdahl e contenção de memória):**
   - Cada operação `registrarLeitura` realiza trabalho aritmético mínimo em memória (poucos nanossegundos por leitura). Portanto, o custo fixo de criação do pool de threads (`Executors.newFixedThreadPool`), particionamento da lista e barreira de sincronização final (`invokeAll` / `Future.get`) representa uma fração não desprezível dos ~6,7 ms totais.
   - Diferentemente de um *map-reduce* com acumuladores isolados por fatia, aqui todas as $N$ threads escrevem **em tempo real na mesma instância compartilhada** de `AgregadorTelemetria` (simulando ingestão concorrente ao vivo). Ainda que o `LongAdder` e o `DoubleAdder` distribuam a contenção em células (`Cell[]`), as atualizações de `ultimaTemperaturaBits` e os loops CAS de mínimo/máximo nos 20 veículos compartilham linhas de cache entre os núcleos.
3. **Zero perda sob alta vazão:** Mesmo a mais de 74 milhões de operações por segundo em estado compartilhado, o uso combinado de `ConcurrentHashMap` + `LongAdder` + CAS manteve 100% de exatidão nas contagens e nos alertas.

---

## 5. Evidência Completa dos Testes e Como Reproduzir

### 5.1 Saída do Maven (`mvn test`)

```text
=== BENCHMARK TELEMETRIA (PI2-169): SEQUENCIAL vs CONCORRENTE ===
Processadores disponíveis (JVM): 20
| Modo | Threads | Leituras | Tempo Mediano (ms) | Throughput (leituras/s) | Speedup |
|---|---:|---:|---:|---:|---:|
| Sequencial | 1 | 500,000 | 13.61 | 36,733,914 | 1.00x |
| Concorrente | 2 | 500,000 | 11.53 | 43,369,272 | 1.18x |
| Concorrente | 4 | 500,000 | 9.97 | 50,156,991 | 1.37x |
| Concorrente | 8 | 500,000 | 8.42 | 59,404,294 | 1.62x |
| Concorrente | 16 | 500,000 | 6.73 | 74,287,582 | 2.02x |
=================================================================

[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.472 s -- in com.hemorede.service.AgregadorTelemetriaBenchmarkTest
[INFO] Running com.hemorede.service.AgregadorTelemetriaTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.115 s -- in com.hemorede.service.AgregadorTelemetriaTest
[INFO] Running com.hemorede.service.TelemetriaServiceTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.157 s -- in com.hemorede.service.TelemetriaServiceTest
[INFO] Running com.hemorede.controller.TelemetriaControllerIntegrationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.826 s -- in com.hemorede.controller.TelemetriaControllerIntegrationTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 5.2 Como reproduzir localmente

```bash
cd rotavital

# 1. Executar os testes de condição de corrida e alerta 2–6 °C
./mvnw test -Dtest=AgregadorTelemetriaTest

# 2. Executar o benchmark sequencial vs concorrente
./mvnw test -Dtest=AgregadorTelemetriaBenchmarkTest

# 3. Executar toda a suíte de telemetria
./mvnw test -Dtest="AgregadorTelemetriaTest,AgregadorTelemetriaBenchmarkTest,TelemetriaServiceTest,TelemetriaControllerIntegrationTest"
```

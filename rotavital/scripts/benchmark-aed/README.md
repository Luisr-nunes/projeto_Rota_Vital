# Benchmark AED (PI2 · L1): matriz, FEFO, matching e Dijkstra com 1 mil, 10 mil e 100 mil

Medição empírica usada no relatório [`doc/aed-u2-relatorio.md`](../../../doc/aed-u2-relatorio.md). Mede as classes **reais** do projeto (`MatrizCompatibilidade`, `FilaFEFO`, `IndiceEstoque`, `GrafoRotas`) e reproduz, sem banco, as etapas do matching da HU08.

Fica fora de `src/main/java` de propósito (como `scripts/virtual-threads`): não entra no `mvn package` nem no CI.

## O que precisa

- JDK 17 ou mais novo (precisa do `javac`, não só do `java`).
- Python 3 com `matplotlib` (`pip install matplotlib`), só para consolidar os resultados e gerar o gráfico.
- Mínimo de 4 GB livres de memória para a JVM (o programa usa `-Xmx4g`).

## Como rodar (PowerShell, a partir da pasta `rotavital`)

```powershell
# 1. Gera target\classes com o projeto normal. O wrapper dispensa ter o Maven instalado.
.\mvnw.cmd -DskipTests package

# 2. Compila só o benchmark contra as classes já compiladas.
mkdir $env:TEMP\aed-bench -Force
javac --release 17 -cp target\classes -d $env:TEMP\aed-bench scripts\benchmark-aed\src\com\hemorede\algoritmos\BenchmarkAED.java

# 3. Roda (uma execução leva cerca de 1 a 2 minutos). Argumentos: aquecimento mínimo e nº de medições.
mkdir resultados-aed\execucao-1 -Force
java -Xms2g -Xmx4g "-Dsaida=resultados-aed\execucao-1" -cp "target\classes;$env:TEMP\aed-bench" com.hemorede.algoritmos.BenchmarkAED 8 31 > resultados-aed\execucao-1\resultado.md
```

No Linux/macOS, troque `\` por `/` e o `;` do classpath por `:`, e use `./mvnw`.

O `javac` pode imprimir avisos `unknown enum constant ... class file for jakarta.persistence ... not found`. São esperados (as anotações JPA não estão no classpath do benchmark) e não afetam nada.

**Repita o passo 3 três vezes** (`execucao-1`, `execucao-2`, `execucao-3`), cada uma numa JVM nova, sem usar o computador para outra coisa enquanto roda: o programa mede tempo, e outro processo pesado distorce o resultado.

## Consolidar e gerar o gráfico

```powershell
python scripts\benchmark-aed\consolidar_benchmark_aed.py resultados-aed\consolidado `
  resultados-aed\execucao-1\benchmark_aed.csv `
  resultados-aed\execucao-2\benchmark_aed.csv `
  resultados-aed\execucao-3\benchmark_aed.csv
```

Gera `benchmark_aed.csv` (mediana das medianas e variação entre execuções), `benchmark_aed_tabelas.md` e `benchmark_aed.png`.

## Como ler

- **Mediana** de 31 execuções cronometradas, depois de aquecimento (no mínimo 8 execuções **e** 400 ms) para o JIT estabilizar. Também sai o p95 e o mínimo.
- **Variação entre execuções** = maior mediana ÷ menor mediana entre as JVMs. Perto de 1,0 é estável; acima de 1,5 o valor é só ordem de grandeza.
- **ns/op** é o tempo por operação elementar (por chamada, por inserção, por bolsa). Nas linhas "total" e no Dijkstra não faz sentido e aparece como "—" na tabela consolidada.
- Antes de medir, o programa confere o comportamento (27 pares compatíveis na matriz, FEFO em ordem, alocação sem repetir bolsa, custo N0→N4 = 17). Se alguma verificação falhar, ele aborta sem medir.

## Quando mudar o código medido

Se `FilaFEFO`, `IndiceEstoque`, `MatrizCompatibilidade`, `GrafoRotas` ou `AlocacaoService` mudarem, refaça a medição. Em particular, o harness repete as etapas do `AlocacaoService.alocar` (o método `alocarProximaCompativel` é privado no serviço e foi copiado em `BenchmarkAED`): se a lógica do serviço mudar, a cópia precisa mudar junto.

## O que não mede

Banco de dados, Spring, HTTP e concorrência. Os dados são sintéticos (semente fixa 42), com tipos sanguíneos e validades uniformes. Detalhes e limites na seção 6 do relatório.

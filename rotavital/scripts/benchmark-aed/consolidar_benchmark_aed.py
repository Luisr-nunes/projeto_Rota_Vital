#!/usr/bin/env python3
"""Consolida varias execucoes independentes do BenchmarkAED em uma tabela e um grafico.

Uso:
    python consolidar_benchmark_aed.py <saida> <run1/benchmark_aed.csv> [<run2/...> ...]

Gera em <saida>:
    benchmark_aed.csv        mediana das medianas de cada operacao + variacao entre execucoes
    benchmark_aed_tabelas.md as mesmas linhas em tabelas markdown, por grupo
    benchmark_aed.png        escala medida (4 paineis)

"Variacao" = maior mediana / menor mediana entre as execucoes da JVM. Quanto mais perto de 1,
mais estavel foi a medicao; valores altos (>1,5) indicam ruido (JIT, GC, vizinho no host) e a
linha deve ser lida so como ordem de grandeza.
"""
import csv
import statistics
import sys
from collections import OrderedDict
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402


def carregar(caminhos):
    linhas = OrderedDict()
    for caminho in caminhos:
        with open(caminho, encoding="utf-8", newline="") as f:
            for r in csv.DictReader(f):
                chave = (r["grupo"], r["operacao"], int(r["n"]))
                linhas.setdefault(chave, []).append(
                    (float(r["mediana_ms"]), float(r["p95_ms"]), float(r["minimo_ms"]), int(r["ops_por_execucao"]))
                )
    return linhas


def consolidar(linhas):
    saida = []
    for (grupo, operacao, n), amostras in linhas.items():
        medianas = [a[0] for a in amostras]
        mediana = statistics.median(medianas)
        ops = amostras[0][3]
        saida.append({
            "grupo": grupo,
            "operacao": operacao,
            "n": n,
            "ops": ops,
            "mediana_ms": mediana,
            "p95_ms": statistics.median(a[1] for a in amostras),
            "minimo_ms": min(a[2] for a in amostras),
            "ns_por_op": mediana * 1e6 / ops,
            "variacao": (max(medianas) / min(medianas)) if min(medianas) > 0 else float("nan"),
            "execucoes": len(amostras),
        })
    return saida


def escrever_csv(linhas, destino):
    campos = ["grupo", "operacao", "n", "ops_por_execucao", "mediana_ms", "p95_ms", "minimo_ms",
              "ns_por_op", "variacao_entre_execucoes", "execucoes_jvm"]
    with open(destino, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(campos)
        for r in linhas:
            w.writerow([r["grupo"], r["operacao"], r["n"], r["ops"], f"{r['mediana_ms']:.6f}",
                        f"{r['p95_ms']:.6f}", f"{r['minimo_ms']:.6f}", f"{r['ns_por_op']:.2f}",
                        f"{r['variacao']:.2f}", r["execucoes"]])


def escrever_markdown(linhas, destino):
    grupos = OrderedDict()
    for r in linhas:
        grupos.setdefault(r["grupo"], []).append(r)
    partes = []
    for grupo, rs in grupos.items():
        partes.append(f"### {grupo}\n")
        partes.append("| Operação | n | mediana (ms) | p95 (ms) | ns/op | variação entre execuções |")
        partes.append("|---|---:|---:|---:|---:|---:|")
        for r in rs:
            n_fmt = f"{r['n']:,}".replace(",", ".")
            sem_ns = (r["operacao"].startswith("total") or r["operacao"].startswith("construir")
                      or r["grupo"].startswith("Dijkstra"))
            ns_fmt = "—" if sem_ns else f"{r['ns_por_op']:.1f}"
            partes.append(
                f"| {r['operacao']} | {n_fmt} | {r['mediana_ms']:.4f} | {r['p95_ms']:.4f} | "
                f"{ns_fmt} | {r['variacao']:.2f}x |")
        partes.append("")
    Path(destino).write_text("\n".join(partes), encoding="utf-8")


def serie(linhas, grupo, trecho, campo):
    """Pontos (n, valor) do grupo. trecho com "=" na frente exige nome exato; sem ele, basta conter."""
    if trecho.startswith("="):
        casa = lambda op: op == trecho[1:]
    else:
        casa = lambda op: trecho in op
    pts = sorted((r["n"], r[campo]) for r in linhas if r["grupo"] == grupo and casa(r["operacao"]))
    return [p[0] for p in pts], [p[1] for p in pts]


def grafico(linhas, destino):
    cores = ["#1b6ca8", "#d1495b", "#2a9d8f", "#e09f3e", "#6a4c93", "#4d4d4d"]
    fig, eixos = plt.subplots(2, 2, figsize=(12.5, 8.6), dpi=150)
    fig.patch.set_facecolor("white")

    def estilo(ax, titulo, ylabel, xlabel="n (tamanho da entrada)"):
        ax.set_xscale("log")
        ax.set_yscale("log")
        ax.set_title(titulo, loc="left", fontsize=11, fontweight="bold")
        ax.set_ylabel(ylabel, fontsize=9)
        ax.set_xlabel(xlabel, fontsize=9)
        ax.grid(True, which="major", color="#e3e3e3", linewidth=0.8)
        ax.set_axisbelow(True)
        for lado in ("top", "right"):
            ax.spines[lado].set_visible(False)
        ax.tick_params(labelsize=8)

    usadas = {}

    def desenhar(ax, grupo, itens, campo):
        for trecho, rotulo in itens:
            xs, ys = serie(linhas, grupo, trecho, campo)
            if not xs:
                continue
            i = usadas.get(id(ax), 0)
            usadas[id(ax)] = i + 1
            ax.plot(xs, ys, marker="o", markersize=4, linewidth=1.8, color=cores[i % len(cores)], label=rotulo)

    # A: consultas O(1) -> ns por chamada deve ficar plano (escala linear, y a partir de 0)
    ax = eixos[0][0]
    estilo(ax, "Consultas O(1): custo por chamada", "ns por chamada")
    ax.set_yscale("linear")
    desenhar(ax, "Matriz ABO/Rh 8x8", [("(so a matriz)", "compativel (só a matriz)"), ("matching(receptor)", "matching(receptor)")], "ns_por_op")
    desenhar(ax, "IndiceEstoque (8 filas FEFO)", [("totalDisponivelPorTipo", "totalDisponivelPorTipo"), ("consultarEstoque", "consultarEstoque")], "ns_por_op")
    desenhar(ax, "FilaFEFO (min-heap)", [("peek()", "peek() encadeado")], "ns_por_op")
    ax.set_ylim(0, 16)
    ax.legend(fontsize=7.5, frameon=False, loc="upper left", ncol=2)

    # B: heap -> custo por operacao
    ax = eixos[0][1]
    estilo(ax, "Heap FEFO: custo por operação", "ns por operação")
    desenhar(ax, "FilaFEFO (min-heap)", [
        ("inserir n (melhor caso", "inserir, melhor caso (já em ordem)"),
        ("inserir n (ordem aleatoria", "inserir, ordem aleatória"),
        ("inserir n (pior caso", "inserir, pior caso (ordem inversa)"),
        ("alocar n", "alocar (poll)"),
    ], "ns_por_op")
    ax.legend(fontsize=7.5, frameon=False, loc="upper left")

    # C: matching HU08 -> tempo total por execucao, com referencias
    ax = eixos[1][0]
    estilo(ax, "Matching/alocação da HU08 (sem banco)", "tempo por execução (ms)")
    desenhar(ax, "Matching/alocacao HU08 (sem banco)", [
        ("1. filtrar", "1. filtrar validade + compatibilidade"),
        ("2. indexar", "2. indexar no IndiceEstoque"),
        ("=total (1+2+3), q = 10", "total, q = 10"),
        ("=total (1+2+3), q = 10% das candidatas", "total, q = 10% das candidatas"),
    ], "mediana_ms")
    xs, ys = serie(linhas, "Matching/alocacao HU08 (sem banco)", "1. filtrar", "mediana_ms")
    if xs:
        ax.plot(xs, [ys[0] * x / xs[0] for x in xs], linestyle="--", color="#9a9a9a", linewidth=1.1, label="referência O(n)")
    ax.legend(fontsize=7.5, frameon=False, loc="upper left")

    # D: Dijkstra
    ax = eixos[1][1]
    estilo(ax, "Dijkstra em grafo esparso (E ≈ 3V)", "tempo por consulta (ms)", "V (vértices)")
    desenhar(ax, "Dijkstra (GrafoRotas)", [
        ("inalcancavel", "destino inalcançável (explora o grafo todo)"),
    ], "mediana_ms")
    xs, ys = serie(linhas, "Dijkstra (GrafoRotas)", "inalcancavel", "mediana_ms")
    if xs:
        import math
        ref = [ys[0] * (x * math.log2(x)) / (xs[0] * math.log2(xs[0])) for x in xs]
        ax.plot(xs, ref, linestyle="--", color="#9a9a9a", linewidth=1.1, label="referência O(V log V)")
    ax.legend(fontsize=7.5, frameon=False, loc="upper left")

    fig.suptitle("Rota Vital · AED U2 · escala medida de 1 mil a 100 mil (mediana de execuções independentes)",
                 x=0.01, ha="left", fontsize=12, fontweight="bold")
    fig.tight_layout(rect=(0, 0, 1, 0.95))
    fig.savefig(destino, facecolor="white")
    plt.close(fig)


def main():
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    saida = Path(sys.argv[1])
    saida.mkdir(parents=True, exist_ok=True)
    linhas = consolidar(carregar(sys.argv[2:]))
    escrever_csv(linhas, saida / "benchmark_aed.csv")
    escrever_markdown(linhas, saida / "benchmark_aed_tabelas.md")
    grafico(linhas, saida / "benchmark_aed.png")
    print(f"{len(linhas)} linhas consolidadas de {len(sys.argv) - 2} execucoes -> {saida}")


if __name__ == "__main__":
    main()

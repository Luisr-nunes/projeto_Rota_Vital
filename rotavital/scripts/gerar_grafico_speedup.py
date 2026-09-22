#!/usr/bin/env python3
"""Gera o gráfico de speedup a partir do CSV produzido pelo benchmark."""
import csv
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

CSV_PATH = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/tmp/rotavital-bench-final/benchmark_relatorio_historico.csv")
SAIDA = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("/tmp/rotavital-bench-final/grafico_speedup.png")

ROTULOS = {
    ("sequencial", 1): "Sequencial",
    ("paralelo", 2): "2 threads",
    ("paralelo", 4): "4 threads",
    ("paralelo", 8): "8 threads",
}
ORDEM = [("sequencial", 1), ("paralelo", 2), ("paralelo", 4), ("paralelo", 8)]

linhas = list(csv.DictReader(open(CSV_PATH, encoding="utf-8")))
tamanhos = sorted({int(l["tamanho"]) for l in linhas})

fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(13, 5))

cores = {100_000: "#4C72B0", 1_000_000: "#DD8452", 5_000_000: "#55A868"}
marcadores = {100_000: "o", 1_000_000: "s", 5_000_000: "^"}

# --- Gráfico 1: tempo de processamento (ms) por versão, escala log ---
for tamanho in tamanhos:
    pontos = {(l["modo"], int(l["threads"])): float(l["processamento_ms_mediana"])
              for l in linhas if int(l["tamanho"]) == tamanho}
    x = list(range(len(ORDEM)))
    y = [pontos[chave] for chave in ORDEM]
    ax1.plot(x, y, marker=marcadores[tamanho], color=cores[tamanho],
              label=f"{tamanho:,} registros".replace(",", "."))

ax1.set_xticks(range(len(ORDEM)))
ax1.set_xticklabels([ROTULOS[c] for c in ORDEM], rotation=20)
ax1.set_ylabel("Tempo de processamento (ms, mediana, escala log)")
ax1.set_yscale("log")
ax1.set_title("Tempo de processamento por versão")
ax1.grid(True, which="both", linestyle="--", alpha=0.4)
ax1.legend()

# --- Gráfico 2: speedup em relação à versão sequencial ---
for tamanho in tamanhos:
    pontos = {(l["modo"], int(l["threads"])): float(l["speedup_processamento"])
              for l in linhas if int(l["tamanho"]) == tamanho}
    x = list(range(len(ORDEM)))
    y = [pontos[chave] for chave in ORDEM]
    ax2.plot(x, y, marker=marcadores[tamanho], color=cores[tamanho],
              label=f"{tamanho:,} registros".replace(",", "."))

# linha de speedup ideal (linear) até 8x, para referência visual
ax2.axhline(1.0, color="gray", linestyle=":", linewidth=1)
ax2.set_xticks(range(len(ORDEM)))
ax2.set_xticklabels([ROTULOS[c] for c in ORDEM], rotation=20)
ax2.set_ylabel("Speedup vs. sequencial (mediana do tempo de processamento)")
ax2.set_title("Speedup por versão")
ax2.grid(True, linestyle="--", alpha=0.4)
ax2.legend()

fig.suptitle("Rota Vital, Relatório de Histórico de Requisições: sequencial x threads de plataforma (Java 17)", fontsize=12)
fig.tight_layout(rect=[0, 0, 1, 0.94])
fig.savefig(SAIDA, dpi=160)
print(f"Gráfico salvo em {SAIDA}")

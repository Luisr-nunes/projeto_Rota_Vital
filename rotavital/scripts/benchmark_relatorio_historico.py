#!/usr/bin/env python3
"""
Benchmark do endpoint GET /api/v1/relatorios/historico do Rota Vital.

Mede o tempo de resposta real (round-trip HTTP) da operação escolhida para
a entrega de SO/concorrência, relatório de estatísticas do histórico de
requisições, nas versões sequencial e com threads de plataforma (2, 4 e
8), que são as que o endpoint expõe (Java 17). A comparação opcional com
virtual threads (Java 21) fica fora do endpoint, ver
scripts/virtual-threads/README.md.

Para cada combinação (tamanho da entrada x modo/threads), faz algumas
chamadas de aquecimento (para o JIT compilar os hot paths e a JVM estabilizar,
sem contar no resultado) e depois N repetições medidas, guardando a mediana
do tempo de resposta e o tempo de processamento reportado pelo próprio
endpoint (medido internamente com System.nanoTime, sem o overhead de rede/
serialização).

Antes de rodar, garanta que a aplicação está no ar (mvn spring-boot:run,
ou o jar empacotado) na porta informada em --base-url.

Uso:
    python3 benchmark_relatorio_historico.py --base-url http://localhost:8089 \
        --tamanhos 100000 1000000 --repeticoes 5 --saida resultados
"""
import argparse
import csv
import json
import statistics
import time
import urllib.request
from pathlib import Path

MODOS = [
    ("sequencial", 1),
    ("paralelo", 2),
    ("paralelo", 4),
    ("paralelo", 8),
]


def chamar_endpoint(base_url: str, tamanho: int, modo: str, threads: int) -> dict:
    url = f"{base_url}/api/v1/relatorios/historico?tamanho={tamanho}&modo={modo}&threads={threads}"
    inicio = time.perf_counter()
    with urllib.request.urlopen(url, timeout=120) as resposta:
        corpo = json.loads(resposta.read().decode("utf-8"))
    fim = time.perf_counter()
    corpo["_tempoRespostaHttpMs"] = (fim - inicio) * 1000.0
    return corpo


def aquecer(base_url: str, tamanho: int, modo: str, threads: int, repeticoes: int):
    for _ in range(repeticoes):
        chamar_endpoint(base_url, tamanho, modo, threads)


def medir(base_url: str, tamanho: int, modo: str, threads: int, repeticoes: int):
    respostas_http_ms = []
    processamento_ms = []
    resultado_referencia = None
    for _ in range(repeticoes):
        corpo = chamar_endpoint(base_url, tamanho, modo, threads)
        respostas_http_ms.append(corpo["_tempoRespostaHttpMs"])
        processamento_ms.append(corpo["tempoProcessamentoNs"] / 1_000_000.0)
        if resultado_referencia is None:
            resultado_referencia = corpo["resultado"]
        elif corpo["resultado"] != resultado_referencia:
            raise AssertionError(
                f"Resultado divergente em tamanho={tamanho} modo={modo} threads={threads}: "
                "sequencial e paralelo não bateram, há uma race condition."
            )
    return {
        "tamanho": tamanho,
        "modo": modo,
        "threads": threads,
        "resposta_http_ms_mediana": statistics.median(respostas_http_ms),
        "resposta_http_ms_min": min(respostas_http_ms),
        "resposta_http_ms_max": max(respostas_http_ms),
        "processamento_ms_mediana": statistics.median(processamento_ms),
        "resultado_hash": hash(json.dumps(resultado_referencia, sort_keys=True)),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://localhost:8089")
    parser.add_argument("--tamanhos", type=int, nargs="+", default=[100_000, 1_000_000])
    parser.add_argument("--repeticoes", type=int, default=7)
    parser.add_argument("--aquecimento", type=int, default=3)
    parser.add_argument("--saida", default="resultados")
    args = parser.parse_args()

    saida_dir = Path(args.saida)
    saida_dir.mkdir(parents=True, exist_ok=True)

    linhas = []
    resultados_por_tamanho = {}

    for tamanho in args.tamanhos:
        print(f"\n=== tamanho={tamanho} ===")
        referencia_sequencial = None
        for modo, threads in MODOS:
            print(f"  aquecendo {modo} threads={threads} ...", end=" ", flush=True)
            aquecer(args.base_url, tamanho, modo, threads, args.aquecimento)
            print("medindo ...", end=" ", flush=True)
            linha = medir(args.base_url, tamanho, modo, threads, args.repeticoes)
            linhas.append(linha)

            if modo == "sequencial":
                referencia_sequencial = linha["resultado_hash"]
            elif referencia_sequencial is not None and linha["resultado_hash"] != referencia_sequencial:
                raise AssertionError(
                    f"tamanho={tamanho} modo={modo} threads={threads}: resultado difere do sequencial!"
                )

            print(
                f"http_mediana={linha['resposta_http_ms_mediana']:.1f}ms "
                f"processamento_mediana={linha['processamento_ms_mediana']:.1f}ms"
            )

        base = next(l for l in linhas if l["tamanho"] == tamanho and l["modo"] == "sequencial")
        resultados_por_tamanho[tamanho] = base

    # calcula o speedup em cima do tempo de processamento medido no servidor
    # (exclui rede/serialização, que não fazem parte da operação paralelizada)
    for linha in linhas:
        base = resultados_por_tamanho[linha["tamanho"]]
        linha["speedup_processamento"] = round(
            base["processamento_ms_mediana"] / linha["processamento_ms_mediana"], 3
        ) if linha["processamento_ms_mediana"] > 0 else None
        linha["speedup_http"] = round(
            base["resposta_http_ms_mediana"] / linha["resposta_http_ms_mediana"], 3
        )

    campos = [
        "tamanho", "modo", "threads",
        "resposta_http_ms_mediana", "resposta_http_ms_min", "resposta_http_ms_max",
        "processamento_ms_mediana", "speedup_processamento", "speedup_http",
    ]
    csv_path = saida_dir / "benchmark_relatorio_historico.csv"
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=campos)
        writer.writeheader()
        for linha in linhas:
            writer.writerow({k: linha[k] for k in campos})

    json_path = saida_dir / "benchmark_relatorio_historico.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(linhas, f, ensure_ascii=False, indent=2)

    print(f"\nCSV salvo em {csv_path}")
    print(f"JSON salvo em {json_path}")
    print("\nTodas as respostas paralelas/virtuais bateram byte a byte com a sequencial "
          "(mesmo hash do JSON de 'resultado'), sem race condition.")


if __name__ == "__main__":
    main()

### Matriz ABO/Rh 8x8

| Operação | n | mediana (ms) | p95 (ms) | ns/op | variação entre execuções |
|---|---:|---:|---:|---:|---:|
| construir a matriz (uma vez) | 8 | 0.0029 | 0.0042 | — | 2.09x |
| compativel(doador, receptor), n chamadas (so a matriz) | 1.000 | 0.0060 | 0.0061 | 6.0 | 1.00x |
| compativel sobre List<Bolsa> (inclui ler o tipo da bolsa) | 1.000 | 0.0015 | 0.0017 | 1.5 | 1.37x |
| matching(receptor), n chamadas | 1.000 | 0.0086 | 0.0125 | 8.6 | 1.04x |
| compativel(doador, receptor), n chamadas (so a matriz) | 10.000 | 0.0617 | 0.0784 | 6.2 | 1.06x |
| compativel sobre List<Bolsa> (inclui ler o tipo da bolsa) | 10.000 | 0.0142 | 0.0203 | 1.4 | 1.00x |
| matching(receptor), n chamadas | 10.000 | 0.0855 | 0.1006 | 8.5 | 1.00x |
| compativel(doador, receptor), n chamadas (so a matriz) | 100.000 | 0.6090 | 0.6283 | 6.1 | 1.07x |
| compativel sobre List<Bolsa> (inclui ler o tipo da bolsa) | 100.000 | 0.7234 | 0.7845 | 7.2 | 1.08x |
| matching(receptor), n chamadas | 100.000 | 0.8743 | 0.9068 | 8.7 | 1.01x |

### FilaFEFO (min-heap)

| Operação | n | mediana (ms) | p95 (ms) | ns/op | variação entre execuções |
|---|---:|---:|---:|---:|---:|
| inserir n (ordem aleatoria) | 1.000 | 0.0571 | 0.0854 | 57.1 | 2.59x |
| inserir n (melhor caso: ja em ordem FEFO) | 1.000 | 0.0205 | 0.0320 | 20.5 | 1.24x |
| inserir n (pior caso: ordem FEFO inversa) | 1.000 | 0.2477 | 0.2833 | 247.7 | 1.35x |
| alocar n (esvaziar a fila) | 1.000 | 0.3440 | 0.3671 | 344.0 | 1.62x |
| peek() encadeado em 8 filas, n chamadas | 1.000 | 0.0106 | 0.0176 | 10.6 | 1.02x |
| disponiveisOrdenadas() | 1.000 | 0.1220 | 0.1451 | 122.0 | 1.12x |
| proximasAoVencimento(7) | 1.000 | 0.0152 | 0.0386 | 15.2 | 1.03x |
| inserir n (ordem aleatoria) | 10.000 | 0.6551 | 0.7482 | 65.5 | 1.19x |
| inserir n (melhor caso: ja em ordem FEFO) | 10.000 | 0.4487 | 0.5168 | 44.9 | 3.17x |
| inserir n (pior caso: ordem FEFO inversa) | 10.000 | 4.0515 | 4.2732 | 405.2 | 1.09x |
| alocar n (esvaziar a fila) | 10.000 | 5.8157 | 8.2592 | 581.6 | 1.66x |
| peek() encadeado em 8 filas, n chamadas | 10.000 | 0.1092 | 0.1390 | 10.9 | 1.08x |
| disponiveisOrdenadas() | 10.000 | 1.9807 | 2.2938 | 198.1 | 1.17x |
| proximasAoVencimento(7) | 10.000 | 0.3262 | 0.3693 | 32.6 | 1.19x |
| inserir n (ordem aleatoria) | 100.000 | 6.9827 | 7.6441 | 69.8 | 1.02x |
| inserir n (melhor caso: ja em ordem FEFO) | 100.000 | 7.5721 | 8.0336 | 75.7 | 1.36x |
| inserir n (pior caso: ordem FEFO inversa) | 100.000 | 62.2386 | 83.7495 | 622.4 | 1.16x |
| alocar n (esvaziar a fila) | 100.000 | 138.7423 | 174.9071 | 1387.4 | 1.03x |
| peek() encadeado em 8 filas, n chamadas | 100.000 | 1.1165 | 1.1794 | 11.2 | 1.04x |
| disponiveisOrdenadas() | 100.000 | 26.7852 | 37.5581 | 267.9 | 1.09x |
| proximasAoVencimento(7) | 100.000 | 6.1962 | 7.3406 | 62.0 | 1.22x |

### IndiceEstoque (8 filas FEFO)

| Operação | n | mediana (ms) | p95 (ms) | ns/op | variação entre execuções |
|---|---:|---:|---:|---:|---:|
| adicionarBolsa, n bolsas | 1.000 | 0.0619 | 0.0940 | 61.9 | 1.67x |
| totalDisponivelPorTipo(tipo), n chamadas | 1.000 | 0.0089 | 0.0089 | — | 1.00x |
| consultarEstoque(tipo), n chamadas | 1.000 | 0.0089 | 0.0091 | 8.9 | 1.01x |
| adicionarBolsa, n bolsas | 10.000 | 0.7016 | 0.7815 | 70.2 | 1.27x |
| totalDisponivelPorTipo(tipo), n chamadas | 10.000 | 0.0888 | 0.1137 | — | 1.02x |
| consultarEstoque(tipo), n chamadas | 10.000 | 0.0888 | 0.1045 | 8.9 | 1.02x |
| adicionarBolsa, n bolsas | 100.000 | 11.8226 | 12.2268 | 118.2 | 1.19x |
| totalDisponivelPorTipo(tipo), n chamadas | 100.000 | 0.9076 | 0.9290 | — | 1.03x |
| consultarEstoque(tipo), n chamadas | 100.000 | 0.9056 | 0.9680 | 9.1 | 1.02x |

### Matching/alocacao HU08 (sem banco)

| Operação | n | mediana (ms) | p95 (ms) | ns/op | variação entre execuções |
|---|---:|---:|---:|---:|---:|
| 1. filtrar validade + compatibilidade (N bolsas) | 1.000 | 0.0224 | 0.0297 | 22.4 | 1.33x |
| 2. indexar as candidatas no IndiceEstoque | 1.000 | 0.0207 | 0.0326 | 42.1 | 1.10x |
| 3. alocar q = 10 | 1.000 | 0.0024 | 0.0043 | 240.0 | 1.07x |
| total (1+2+3), q = 10 | 1.000 | 0.0466 | 0.0810 | — | 1.07x |
| 3. alocar q = 10% das candidatas | 1.000 | 0.0116 | 0.0304 | 236.6 | 1.35x |
| total (1+2+3), q = 10% das candidatas | 1.000 | 0.0604 | 0.0902 | — | 1.23x |
| 1. filtrar validade + compatibilidade (N bolsas) | 10.000 | 0.3145 | 0.3817 | 31.4 | 1.46x |
| 2. indexar as candidatas no IndiceEstoque | 10.000 | 0.3338 | 0.3861 | 66.2 | 1.10x |
| 3. alocar q = 10 | 10.000 | 0.0051 | 0.0085 | 513.9 | 1.30x |
| total (1+2+3), q = 10 | 10.000 | 0.6707 | 0.7032 | — | 1.37x |
| 3. alocar q = 10% das candidatas | 10.000 | 0.2280 | 0.2565 | 452.4 | 1.07x |
| total (1+2+3), q = 10% das candidatas | 10.000 | 0.8787 | 0.9360 | — | 1.16x |
| 1. filtrar validade + compatibilidade (N bolsas) | 100.000 | 3.4188 | 4.0142 | 34.2 | 1.04x |
| 2. indexar as candidatas no IndiceEstoque | 100.000 | 7.5813 | 9.1540 | 151.3 | 1.15x |
| 3. alocar q = 10 | 100.000 | 0.0264 | 0.0343 | 2637.6 | 1.03x |
| total (1+2+3), q = 10 | 100.000 | 10.3518 | 10.7214 | — | 1.11x |
| 3. alocar q = 10% das candidatas | 100.000 | 6.6494 | 6.8982 | 1327.2 | 1.10x |
| total (1+2+3), q = 10% das candidatas | 100.000 | 17.1772 | 20.5781 | — | 1.16x |

### Dijkstra (GrafoRotas)

| Operação | n | mediana (ms) | p95 (ms) | ns/op | variação entre execuções |
|---|---:|---:|---:|---:|---:|
| rede real V=6, E=15: N0 -> N4 | 6 | 0.0005 | 0.0012 | — | 1.03x |
| grafo esparso (E ~ 3V): destino alcancavel | 1.000 | 0.1107 | 0.1191 | — | 1.11x |
| grafo esparso (E ~ 3V): destino inalcancavel (explora tudo) | 1.000 | 0.7745 | 0.8672 | — | 1.04x |
| grafo esparso (E ~ 3V): destino alcancavel | 10.000 | 8.5216 | 9.1854 | — | 1.18x |
| grafo esparso (E ~ 3V): destino inalcancavel (explora tudo) | 10.000 | 19.4859 | 23.9772 | — | 1.14x |
| grafo esparso (E ~ 3V): destino alcancavel | 100.000 | 218.8631 | 351.8414 | — | 1.03x |
| grafo esparso (E ~ 3V): destino inalcancavel (explora tudo) | 100.000 | 422.2355 | 622.0308 | — | 1.07x |

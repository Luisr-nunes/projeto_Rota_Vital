# AED — Unidade 2 (matriz de compatibilidade ABO/Rh 8×8)

Matriz doador × receptor com consulta O(1) e *matching* ordenado (tipo idêntico primeiro, do mais
específico ao mais universal, O- por último), em C e em Java.

- Código C: [`c/`](c) — `make test` (AddressSanitizer), `make run`, `make valgrind`
- Código Java: [`MatrizCompatibilidade`](../../rotavital/src/main/java/com/hemorede/algoritmos/MatrizCompatibilidade.java) — `mvn test -Dtest=MatrizCompatibilidadeTest`
- Usada na alocação por [`CompatibilidadeSanguineaService`](../../rotavital/src/main/java/com/hemorede/service/CompatibilidadeSanguineaService.java)
- Regras, fontes e análise de complexidade: [`doc/aed-u2-compatibilidade.md`](../../doc/aed-u2-compatibilidade.md)

Regra: cada tipo é o conjunto de antígenos {A, B, Rh}; o doador é compatível quando seus antígenos
estão contidos nos do receptor. A tabela é montada uma vez (O(k²), k = 8) e depois `compativel` e
`matching` são O(1). Ordem do matching: (1) tipo idêntico; (2) menos receptores atendidos primeiro;
(3) desempate pela ordem do enum.

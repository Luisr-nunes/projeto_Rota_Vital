# 📋 Resumo do Sprint — Week 04e05

**Sprint:** Week 04e05 (ID: 4)
**Período:** 24/08/2026 – 04/09/2026 (encerrado em 09/09/2026)
**Meta do Sprint:** Não definida formalmente
**Status:** ✅ Concluído — 3/3 histórias entregues

---

## 🏆 O que foi feito

### [PI2-15 — W04-Pipeline/Deploy Inicial (SO)](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-15)
**Responsável:** Lucas Henrique Gomes Medeiros
**Entregue em:** 09/09/2026

- Pipeline de **CI/CD configurado via GitHub Actions** (`ci.yml`) com trigger automático a cada `push` na branch `main`
- Build com **Java 17 + Maven** e execução de testes automatizados (`mvn test`) no pipeline
- **Health Check** implementado via `spring-boot-starter-actuator` no endpoint `/actuator/health` → retorno `{"status":"UP"}` validado
- Pipeline executado com **status verde (Success)** no GitHub Actions
- `.gitignore` configurado para ignorar artefatos Maven (`target/`)

**Arquivos entregues:** `.github/workflows/ci.yml`, `pom.xml`, `.gitignore`

---

### [PI2-16 — W04-Topologia (RSD)](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-16)
**Responsável:** Micaella Maria Barbosa Cabral
**Entregue em:** 08/09/2026

- Criado arquivo `topologia.md` consolidando a topologia de rede do sistema **Rota Vital**
- Incluídos: diagrama de arquitetura, tabela de ligações/protocolos e mapeamento de requisitos de negócio → requisitos de rede
- Documentação dos componentes, zonas de segurança e medidas de infraestrutura
- Notas sobre ambientes e ações futuras para deploy em produção

---

### [PI2-17 — W04-Estruturas-base (AED)](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-17)
**Responsável:** Matheus Rodrigues Larre
**Entregue em:** 09/09/2026

- **`FilaFEFO`** (Min-Heap / PriorityQueue): alocação de hemocomponentes priorizando os de validade mais próxima (política FEFO)
- **`GrafoRotas`** (Grafo ponderado + Dijkstra): cálculo da rota mais rápida entre o Hemocentro e os hospitais da rede
- **`IndiceEstoque`** (Tabela Hash): mapeamento por tipo sanguíneo integrando o estoque às filas FEFO
- **`AEDTesteManualTest`** (JUnit 5): suíte de testes com 100% de aprovação, cobrindo menor caminho, alocações e exceções

**Arquivos entregues:** `FilaFEFO.java`, `GrafoRotas.java`, `IndiceEstoque.java`, `AEDTesteManualTest.java`

---

## 🚧 Bloqueadores e Riscos

- **Nenhum bloqueador registrado** nas issues do sprint
- ⚠️ **Risco observado:** sprint encerrado com **5 dias de atraso** (previsto: 04/09, concluído: 09/09) — pode indicar gargalo no processo de revisão ou validação final

---

## 📊 Resumo de Entregas

| Issue | Título | Responsável | Status | Entregue em |
|-------|--------|-------------|--------|-------------|
| [PI2-15](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-15) | W04-Pipeline/Deploy Inicial (SO) | Lucas Henrique | ✅ Concluído | 09/09/2026 |
| [PI2-16](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-16) | W04-Topologia (RSD) | Micaella Cabral | ✅ Concluído | 08/09/2026 |
| [PI2-17](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-17) | W04-Estruturas-base (AED) | Matheus Larre | ✅ Concluído | 09/09/2026 |

**Total:** 3 histórias planejadas | 3 concluídas | 0 transferidas | 0 removidas

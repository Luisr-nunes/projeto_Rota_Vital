# Roteiro da apresentação final de POO

Resumo de até **8 minutos** do produto, do processo de trabalho e do time, na aula entre 09/11 e 13/11/2026. **Os sete membros precisam estar presentes**, e cada um tem uma fala.

Ticket: PI2-201 (K1) · Issue #63 · Responsável: Luis Felipe.

A ordem segue o enunciado: problema, solução, fluxo de trabalho, ferramentas, lições aprendidas e demonstração.

---

## Tempo por bloco

| Bloco | Tempo | Quem fala | Slide |
|---|---|---|---|
| 1. O problema | 0:00–0:45 | Luis Felipe | 1–2 |
| 2. A solução | 0:45–2:00 | Mariana | 3–4 |
| 3. Fluxo de trabalho | 2:00–4:15 | Luis Felipe, João Pedro, Lucas, Micaella | 5–8 |
| 4. Ferramentas | 4:15–5:00 | Luis Lucena | 9 |
| 5. Lições aprendidas | 5:00–5:45 | Luis Lucena e João Pedro | 10 |
| 6. Demonstração | 5:45–7:45 | Matheus (vídeo curto) | 11 |
| Fechamento | 7:45–8:00 | Luis Felipe | 12 |

Folga de 15 s para troca de quem fala. Se passar de 8 min, encurte o vídeo, não as falas.

---

## 1. O problema (0:00–0:45) — Luis Felipe

**Slide 1 — Capa:** Rota Vital · equipe 05 · nomes dos sete integrantes · CESAR School 2026.2.

**Slide 2 — O problema**

> "A rede de sangue precisa entregar o componente certo, compatível e dentro da validade, no lugar certo, no tempo certo e na temperatura certa. Quando isso falha, falta sangue no hospital, bolsas vencem no estoque e o paciente corre risco. Hoje estoque, compatibilidade, rota e monitoramento da cadeia fria ficam em processos separados. O Rota Vital junta tudo isso numa aplicação só."

---

## 2. A solução (0:45–2:00) — Mariana

**Slide 3 — O que o produto faz:** a aplicação web em Java/Spring Boot cobre o caminho da bolsa, da doação até a entrega.

| Etapa | História |
|---|---|
| Registrar doação e cadastrar a bolsa | HU01 |
| Consultar estoque por tipo, componente e validade | HU02 |
| Registrar requisição do hospital | HU03 |
| Alocar bolsa compatível (ABO/Rh) que vence primeiro (FEFO) | HU04 / HU08 |
| Calcular a rota de menor custo (Dijkstra) | HU05 |
| Confirmar a entrega ao hospital | HU06 |
| Painel gerencial com indicadores | HU07 |

**Slide 4 — Para quem e diferencial**

> "Atende três perfis: o hospital, que pede; o hemocentro, que aloca e controla o estoque; e o gestor da rede, que acompanha os indicadores. O diferencial é a alocação: o sistema só sugere bolsas compatíveis com o paciente e, entre elas, a que vence primeiro, mesmo que seja de outro tipo compatível. Isso reduz o descarte por vencimento sem colocar o paciente em risco. Todos os dados são sintéticos."

---

## 3. Fluxo de trabalho (2:00–4:15)

**Slide 5 — Planejamento (Luis Felipe, 0:35)**

> "Trabalhamos em sprints de duas semanas no Jira, com um épico por disciplina e as entregas de cada professor como histórias da sprint. Cada pessoa recebia de 5 a 6 pontos por sprint, e no fim de cada sprint publicávamos um resumo com o que foi entregue, o burndown e as pendências."

Mostrar: print do quadro do Jira e um resumo de sprint (`doc/resumo-sprint-W12e13.md`).

**Slide 6 — Requisitos (João Pedro, 0:30)**

> "As histórias foram escritas com critérios de aceite em BDD, no formato Dado / Quando / Então. Antes do código, prototipamos as telas no Figma. Nas histórias de maior risco, a alocação e a entrega, o cenário em Gherkin foi escrito antes da implementação e virou teste automatizado."

Mostrar: um cenário de `alocar_bolsa.feature`.

**Slide 7 — Desenvolvimento e gerência de configuração (Lucas, 0:35)**

> "O código fica no GitHub. Cada tarefa tem uma branch curta e entra na main por pull request. A main é protegida: o merge só acontece com o pipeline verde no GitHub Actions. Depois do merge, o Render publica a nova versão automaticamente, e se algum teste falhar a versão anterior continua no ar. Bugs são registrados nas Issues do GitHub."

Mostrar: o diagrama do pipeline do README (PR → Actions → Render).

**Slide 8 — Qualidade e testes (Micaella, 0:35)**

> "Escrevemos o teste antes do código: o commit `test:` vem antes do `feat:`. Hoje a main tem 295 testes passando: testes unitários das estruturas de dados, testes de integração dos endpoints, testes com PostgreSQL real via Testcontainers e os cenários BDD em Gherkin. Nenhum código entra sem a suíte verde."

Conferir o número de testes no dia (`mvn test`). Em 07/11: 295 testes, 0 falhas.

---

## 4. Ferramentas (4:15–5:00) — Luis Lucena

**Slide 9 — Ferramentas** (ícones em preto e branco, com o link de cada uma no rodapé do slide)

| Uso | Ferramenta |
|---|---|
| Linguagem e framework | Java 17 + Spring Boot |
| Banco | PostgreSQL (Supabase em produção, Docker local) e H2 nos testes rápidos |
| Interface | HTML, CSS e JavaScript consumindo a API REST |
| Testes | JUnit, Cucumber (Gherkin) e Testcontainers |
| CI/CD e deploy | GitHub Actions + Render — https://rota-vital-jnmx.onrender.com |
| Gestão | Jira — https://csprj-adsr-3p-e5.atlassian.net/browse/PI2 |
| Código e bugs | GitHub — https://github.com/Luisr-nunes/projeto_Rota_Vital |
| Protótipo | Figma |
| Vídeos | YouTube |

> "Lombok e outros geradores de código não foram usados, como pede a disciplina."

---

## 5. Lições aprendidas (5:00–5:45)

**Slide 10 — O que aprendemos**

**Luis Lucena (0:25):**
> "Integrar cinco disciplinas num produto só exige combinar os contratos cedo. Quando a API e as estruturas de dados foram definidas antes do código, as entregas encaixaram. Quando não foram, houve retrabalho."

**João Pedro (0:20):**
> "Testar primeiro a regra crítica, a compatibilidade sanguínea, salvou o projeto de erros que só apareceriam na demonstração. E deixar o prazo de cada tarefa visível no Jira ajudou o time a perceber os atrasos antes da data de entrega."

---

## 6. Demonstração (5:45–7:45) — Matheus

**Slide 11 — Vídeo:** versão curta (até 2 min) do vídeo de [`roteiro-demo.md`](roteiro-demo.md). O vídeo completo tem cerca de 4min30s. Para a apresentação, mantenha só:

1. Hospital cria a requisição urgente O+ (HU03).
2. Hemocentro consulta o estoque e vê a bolsa que vence em 2 dias (HU02).
3. Alocação sugere O+ e O−, por compatibilidade e validade (HU08).
4. Rota Dijkstra até o hospital (HU05).
5. Confirmação de entrega (HU06).

Levar o vídeo baixado no notebook e também o link do YouTube. Não depender da internet da sala.

---

## Fechamento (7:45–8:00) — Luis Felipe

**Slide 12 — Obrigado:** link do repositório e QR code.

> "O Rota Vital está no ar e o código está público no GitHub. Obrigado."

---

## Checklist antes do dia

| # | Item | Responsável |
|---|---|---|
| 1 | Slides prontos e revisados pelo grupo | Luis Felipe |
| 2 | Vídeo curto (até 2 min) exportado e baixado no notebook | Matheus |
| 3 | Ensaio cronometrado com os sete, no máximo 8 min | Todos |
| 4 | Número de testes atualizado no slide 8 | Micaella |
| 5 | Aplicação no ar no Render (abrir a URL 5 min antes para acordar o serviço gratuito) | Lucas |
| 6 | Todos presentes no dia da aula | Todos |

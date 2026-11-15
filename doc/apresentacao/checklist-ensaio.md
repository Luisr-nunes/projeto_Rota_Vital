# Checklist do ensaio da apresentação final

Ticket: PI2-202 (K2) · História: PI2-160 — P3 · Responsável: Matheus.

Lista usada no ensaio cronometrado e no dia da apresentação. Base: [roteiro da apresentação](roteiro-apresentacao-final.md) (8 min, sete falas), [roteiro da demo](roteiro-demo.md), [slides](slides-apresentacao-final.pdf) e a [Definition of Done da U2](../definition-of-done-u2.md).

---

## 1. Antes do ensaio (D-2)

| # | Item | Responsável | Como conferir |
|---|---|---|---|
| 1 | Slides na versão final, com as notas de fala de cada slide | Luis Felipe | PDF e PPTX da `main` iguais à versão projetada |
| 2 | Número de testes do slide 8 atualizado | Micaella | `cd rotavital && mvn test` no dia; mesmo número no slide |
| 3 | Vídeo curto da demo (até 2 min) exportado | Matheus | Duração ≤ 2:00; mostra requisição → alocação → rota → entrega |
| 4 | Vídeo baixado no notebook da apresentação **e** link do YouTube no README | Matheus | Abre sem internet; o link do README abre o vídeo certo |
| 5 | Aplicação no ar no Render, na versão da `main` | Lucas | Badge do CI verde e `GET /actuator/health` → `{"status":"UP"}` |
| 6 | Cada integrante leu a própria fala no roteiro | Todos | Confirmação no grupo da equipe |

## 2. Ensaio cronometrado (D-1)

Um integrante cronometra e anota o tempo de cada bloco. Meta: **8:00 no total**, com folga de 15 s para as trocas de quem fala.

| Bloco | Quem fala | Tempo previsto | Tempo no ensaio | OK? |
|---|---|---|---|---|
| 1. O problema (slides 1–2) | Luis Felipe | 0:45 | | ☐ |
| 2. A solução (slides 3–4) | Mariana | 1:15 | | ☐ |
| 3. Planejamento (slide 5) | Luis Felipe | 0:35 | | ☐ |
| 3. Requisitos (slide 6) | João Pedro | 0:30 | | ☐ |
| 3. Desenvolvimento e configuração (slide 7) | Lucas | 0:35 | | ☐ |
| 3. Qualidade e testes (slide 8) | Micaella | 0:35 | | ☐ |
| 4. Ferramentas (slide 9) | Luis Lucena | 0:45 | | ☐ |
| 5. Lições aprendidas (slide 10) | Luis Lucena e João Pedro | 0:45 | | ☐ |
| 6. Demonstração em vídeo (slide 11) | Matheus | 2:00 | | ☐ |
| Fechamento (slide 12) | Luis Felipe | 0:15 | | ☐ |
| **Total** | | **8:00** | | ☐ |

Durante o ensaio, conferir:

- [ ] Os sete integrantes falaram pelo menos uma vez.
- [ ] A ordem seguiu o enunciado: problema, solução, fluxo de trabalho, ferramentas, lições aprendidas e demonstração.
- [ ] Nenhuma fala passou mais de 10 s do tempo previsto.
- [ ] O vídeo rodou do notebook, em tela cheia, com legenda legível do fundo da sala.
- [ ] A troca de slide para o vídeo e de volta não levou mais de 5 s.
- [ ] Ninguém leu o slide palavra por palavra.

**Se passar de 8 min:** encurtar o vídeo, não as falas (regra do roteiro). Cortar primeiro as partes opcionais da demo (estoque depois da reserva e requisição sem botão de entrega).

## 3. No dia da apresentação

| # | Item | Responsável | Quando |
|---|---|---|---|
| 1 | Todos os sete presentes | Todos | Início da aula |
| 2 | Abrir `https://rota-vital-jnmx.onrender.com` para acordar o serviço gratuito | Lucas | 5 min antes |
| 3 | Notebook carregado, com adaptador de vídeo, slides e vídeo abertos | Luis Felipe e Matheus | 10 min antes |
| 4 | Notificações do sistema desligadas e navegador sem abas pessoais | Quem projeta | 10 min antes |
| 5 | Áudio do vídeo testado no som da sala (ou legenda conferida, se for sem som) | Matheus | 5 min antes |
| 6 | Plano B pronto: link do YouTube aberto em uma aba, caso o arquivo local falhe | Matheus | 5 min antes |
| 7 | Cronômetro visível para quem está falando | Um integrante | Durante |

## 4. Depois da apresentação

- [ ] Registrar no Jira (comentário da PI2-160) o tempo final e o que precisou ser cortado.
- [ ] Anotar as perguntas do professor para a retrospectiva da sprint.
- [ ] Atualizar este checklist se algum item faltou no dia.

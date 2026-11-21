# Auditoria de produto, UI/UX e código — Rota Vital

**Data:** 21/11/2026 · **Escopo:** revisão do sistema publicado, sem ampliar funcionalidades  
**Base:** `main` após o PR #94 · **Ambiente:** https://rota-vital-jnmx.onrender.com

Este documento lista as melhorias encontradas na revisão, na ordem de impacto para quem usa o sistema. Cada item traz o que mudar, por quê e a situação atual. As decisões de produto foram tomadas pelo responsável do projeto; as que ainda estão em aberto aparecem com as opções e as consequências.

---

## 1. Decisões de produto registradas

| Tema | Decisão | Consequência |
|---|---|---|
| Direção visual | **Manter o Figma da equipe** (vidro fosco, gradientes, sombras) | Só os tokens foram consolidados; o visual não muda. Diverge do padrão global (estilo Notion), por escolha consciente |
| Páginas legadas (`operacao.html`, `indicadores.html`) | **Área técnica fora da navegação** | Continuam no ar como evidência das entregas acadêmicas, acessíveis pelo endereço, sem link na home |
| Ícones | **Manter o conjunto próprio** (SVG inline do Figma) | Traço padronizado; sem nova dependência |
| Tom de voz | **Dois registros** | Hemocentro e hospital: direto e técnico. Doador: direto e acolhedor. Erros sempre no formato "o que houve + o que fazer" |

### Guia rápido do tom de voz

| Situação | Hemocentro / hospital | Doador |
|---|---|---|
| Ação principal | "Despachar", "Confirmar recebimento", "Corrigir tipo" | "Atualizar meus dados" |
| Estado vazio | "Nada aguardando despacho. Aloque as bolsas de uma requisição pendente para despachá-la." | "Você ainda não tem doações registradas. Depois da sua primeira coleta, ela aparece aqui." |
| Erro | "A requisição precisa estar pendente para ser alocada." | "Você pode doar de novo a partir de 20/01." |
| Termos do domínio | FEFO, ABO/Rh, hemocomponente, sem explicação | Explicados ou evitados |

---

## 2. Melhorias por impacto

### 2.1 Mensagens de erro (maior impacto)

| O que mudar | Por quê | Situação |
|---|---|---|
| Tratar JSON malformado, valor inválido em campo (data, enum), método e Content-Type não aceitos e violação de unicidade no banco, no formato `{erro, mensagem}` | O usuário via "Erro 400" ou o texto padrão do Spring em inglês, sem saber o que corrigir | Feito — PR `fix/erros-padronizados` |
| Acentuar as mensagens do backend (alocação, indicadores, previsão) | Textos como "Requisicao nao encontrada" apareciam na tela, destoando do resto | Feito — PR `fix/erros-padronizados` |
| Montar a mensagem na tela a partir de `mensagem` + erros por campo, e explicar 5xx | No Render, o servidor dormindo devolvia só "Erro 502"; agora diz que está iniciando e para tentar de novo | Feito — PR `fix/erros-padronizados` |

### 2.2 Validação de entrada

| O que mudar | Por quê | Situação |
|---|---|---|
| `POST /api/hospitais` recebe um DTO validado (nome, CNPJ, endereço, coordenadas) em vez da entidade | A entidade aceitava qualquer campo e tamanho, sem validação | Feito — PR `fix/validacao-entradas` |
| Mensagens de validação em português em todos os DTOs, com limites de tamanho | Sem mensagem própria, o Bean Validation responde no idioma do servidor (inglês no Render) | Feito — PR `fix/validacao-entradas` |
| Quantidade por item até 50 bolsas e até 20 itens por requisição, iguais na tela e no backend | A tela limitava a 50, mas a API aceitava qualquer valor | Feito — PR `fix/validacao-entradas` |
| Bloqueio de 15 min após 5 falhas seguidas de login no mesmo e-mail (429) | Sem limite, a senha podia ser testada por força bruta | Feito — PR `fix/validacao-entradas` |

### 2.3 Estados vazios, textos e jargão

| O que mudar | Por quê | Situação |
|---|---|---|
| Estados vazios dizem o motivo e oferecem a ação seguinte (ver pendentes, limpar prioridade, ver entregas) | Alguns vazios só informavam, sem caminho para o usuário seguir | Feito — PR `fix/textos-e-estados-vazios` |
| Remover jargão interno visível: "RN-04", "Dijkstra" no mapa do hospital, IDs de Figma e Jira no HTML | São termos da equipe, não do usuário | Feito — PR `fix/textos-e-estados-vazios` |
| Pilares da home escritos pelo benefício ("Menos bolsas vencidas", "Entrega mais rápida") | "Algoritmo de Dijkstra" descreve a técnica, não o ganho | Feito — PR `fix/textos-e-estados-vazios` |
| Recuperar senha com um único `h1`; remover o código morto do botão "em breve" | Semântica e código sem uso | Feito — PR `fix/textos-e-estados-vazios` |

### 2.4 Páginas legadas

| O que mudar | Por quê | Situação |
|---|---|---|
| Tirar os links da home para `operacao.html` e `indicadores.html` | O usuário mudava de identidade visual ao clicar | Feito — PR `fix/paginas-legadas-area-tecnica` |
| Aviso de área técnica com link de volta ao sistema, fonte Inter e erros em português | Mantém a evidência acadêmica com o mínimo de coerência | Feito — PR `fix/paginas-legadas-area-tecnica` |

### 2.5 Design system (mantendo o Figma)

| O que mudar | Por quê | Situação |
|---|---|---|
| Escala tipográfica em variáveis: 11, 12, 13, 14, 16, 18, 24, 28 px, mais 44/60 no título da home (antes 18 tamanhos soltos) | Hierarquia previsível e manutenção num lugar só | Feito — PR `refactor/tokens-design` |
| `font-size` inline trocado por classes `.fs-*` | Estilo fora do CSS não segue a escala | Feito — PR `refactor/tokens-design` |
| Cores semânticas (`--success`, `--info`, `--warning`, `--danger` e fundos); neutros quase iguais viram apelidos | Havia 4 tons de texto secundário e 3 de borda praticamente iguais | Feito — PR `refactor/tokens-design` |
| Traço 2 em todos os ícones (o menu lateral mantém 1,8/2) | Os traços variavam de 1,8 a 3,2 | Feito — PR `refactor/tokens-design` |

### 2.6 SEO e metadados

| O que mudar | Por quê | Situação |
|---|---|---|
| Favicon com a gota da marca | A aba do navegador não tinha ícone | Feito — PR `feat/metadados-seo` |
| Descrição e Open Graph (imagem 1200x630) na home, login, criar conta e recuperar senha | O link compartilhado no WhatsApp ou Teams aparecia sem imagem nem descrição | Feito — PR `feat/metadados-seo` |
| `noindex` nas áreas logadas e `robots.txt` liberando só as páginas públicas | Telas internas não devem aparecer em buscadores | Feito — PR `feat/metadados-seo` |

---

## 3. Pendências que dependem de decisão

| Tema | Opções | Trade-off |
|---|---|---|
| "Alocada" e "Aprovada" têm o mesmo significado para o usuário | (a) Unificar só o rótulo na tela, sem mexer na API · (b) Unificar também o enum no backend | (a) é rápido e reversível · (b) deixa o modelo coerente, mas exige migrar os dados existentes e ajustar testes BDD |
| Indexação | (a) Manter como está: só home, login e criar conta indexados · (b) `noindex` em tudo | (a) faz sentido se o projeto for divulgado · (b) se o acesso for só da banca e da equipe |
| Hierarquia de densidade | (a) Em Entregas, destacar o que está em trânsito com temperatura fora da faixa e compactar "Entregues" · (b) No painel do hemocentro, urgentes antes dos KPIs | Mudam o layout das telas; convém validar com a equipe antes |

## 4. Pendências técnicas (sem decisão de produto)

| O que mudar | Por quê | Esforço |
|---|---|---|
| Extrair os scripts inline das páginas para `js/paginas/*.js` | Cada tela tem 100 a 200 linhas misturando dados, regra e HTML; separado fica testável e revisável | Médio |
| Um `js/validacao.js` com as regras de tela (CPF, peso, quantidade) | Evita que tela e backend divirjam | Baixo |
| Cores das chamadas `ui.kpi(...)` e `ui.icon(...)` nas páginas | Ainda usam hexadecimal; o SVG não aceita `var()` em atributo, então exige mudar o helper para `style` | Baixo |

---

## 5. PRs e ordem de merge

| Ordem | Branch | Conteúdo |
|---|---|---|
| 1 | `fix/erros-padronizados` | Erros padronizados e mensagens acentuadas |
| 2 | `fix/validacao-entradas` | DTO de hospital, limites, bloqueio de login |
| 3 | `fix/textos-e-estados-vazios` | Microcopy, estados vazios, jargão |
| 4 | `feat/metadados-seo` | Favicon, Open Graph, robots |
| 5 | `fix/paginas-legadas-area-tecnica` | Área técnica (depende do 4) |
| 6 | `refactor/tokens-design` | Escala tipográfica, cores, ícones |
| 7 | `docs/auditoria-produto-ux` | Este documento |

Todos os branches foram combinados num merge de teste sobre a `main` sem conflitos, com **467 testes verdes**.

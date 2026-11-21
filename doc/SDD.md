# SDD — Especificação do Sistema (Spec-Driven Development)

**Sistema:** Rota Vital — gestão e distribuição de hemocomponentes
**Versão da especificação:** 2.0 (perfis, autenticação e rastreamento da entrega)
**Fonte da verdade das regras:** [`regras-de-negocio.md`](regras-de-negocio.md) e o pacote `com.hemorede.domain.regras`
**Estratégia de testes:** [`TDD.md`](TDD.md) · **Complexidade:** [`big-o.md`](big-o.md)
**Documentos anteriores consolidados aqui:** [`historias_de_usuario.md`](historias_de_usuario.md), [`contratos-api.md`](contratos-api.md), [`modelo-dominio.md`](modelo-dominio.md)

Cada requisito tem um identificador (`RF-xx`, `RNF-xx`) e aponta para a regra (`RN-xx`) e para o teste que o garante. Mudança de comportamento começa aqui: primeiro a especificação, depois o teste vermelho, depois o código.

---

## 1. Atores e perfis

| Perfil | Quem é | O que pode fazer | Home |
|---|---|---|---|
| **DOADOR** | Pessoa que doa sangue (autocadastro) | Informar tipo sanguíneo e dados básicos para doar; ver se pode doar e quando; ver o histórico das próprias doações | `/doador/painel.html` |
| **HOSPITAL** | Banco de sangue de um hospital (conta criada pelo hemocentro) | Solicitar bolsas; ver só as próprias requisições; acompanhar a entrega no mapa; confirmar o recebimento | `/hospital/painel.html` |
| **HEMOCENTRO** | Equipe do hemocentro (o sistema) | Tudo: estoque, doações, alocação FEFO, despacho, rastreamento, indicadores, rede e contas de acesso | `/hemocentro/painel.html` |

Regra de isolamento: o hospital e o doador são identificados **só pelo JWT** (`hospitalId`/`doadorId` gravados no token no login). Um recurso de outro hospital responde **404**, igual a inexistente.

---

## 2. Requisitos funcionais

| ID | Requisito | Perfil | Endpoint / tela | Regra | Teste |
|---|---|---|---|---|---|
| RF-01 | Entrar com e-mail e senha e receber um token de 8 h | Todos | `POST /api/auth/login` · `login.html` | — | `PerfisEFluxoEntregaIntegrationTest.loginInvalido` |
| RF-02 | Autocadastro do doador com tipo sanguíneo, sexo, nascimento e peso | Público | `POST /api/auth/cadastro-doador` · `criar-conta.html` | RN-04 | `...doador`, `...doadorInvalido` |
| RF-03 | Ver dados, elegibilidade (apto, motivos, próxima data) e histórico | DOADOR | `GET/PUT /api/doador/me` · `doador/painel.html` | RN-04 | `...doador`, `RegrasAplicadasNosServicosTest.Doacao` |
| RF-04 | Registrar doação e gerar a bolsa | HEMOCENTRO | `POST /api/doacoes` · `operacao.html` | RN-03, RN-04 | `DoacaoControllerIntegrationTest`, `RegrasAplicadasNosServicosTest` |
| RF-05 | Consultar estoque disponível por tipo/componente em ordem FEFO | HEMOCENTRO | `GET /api/estoques/bolsas` · `hemocentro/estoque.html` | RN-02, RN-03 | `EstoqueBolsasValidadeIntegrationTest` |
| RF-06 | Solicitar bolsas (prioridade + itens); o hospital vem do token | HOSPITAL | `POST /api/hospital/requisicoes` · `hospital/painel.html` | RN-09 | `...fluxoCompleto` |
| RF-07 | Listar e abrir só as próprias requisições | HOSPITAL | `GET /api/hospital/requisicoes[/{id}]` | isolamento | `...fluxoCompleto` (404 entre hospitais) |
| RF-08 | Alocar bolsas compatíveis por FEFO (tudo ou nada) | HEMOCENTRO | `POST /api/v1/requisicoes/{id}/alocacao` e `/aprovar` | RN-01, RN-02, RN-03, RN-08, RN-09 | `alocar_bolsa.feature`, `RegrasAplicadasNosServicosTest.Fefo` |
| RF-09 | Calcular a menor rota hemocentro → hospital (Dijkstra) | HEMOCENTRO | `POST /api/rotas/calcular` | RN-05 | `RotaControllerIntegrationTest`, `RoteirizacaoServiceTest` |
| RF-10 | Despachar: veículo compatível, bolsas EM_TRANSITO, previsão de chegada | HEMOCENTRO | `POST /api/v1/requisicoes/{id}/despacho` · `hemocentro/requisicao.html` | RN-06, RN-09 | `...fluxoCompleto` |
| RF-11 | Acompanhar a entrega: mapa, trecho atual, % concluído, previsão, temperatura e link do Google Maps | HOSPITAL, HEMOCENTRO | `GET .../rastreamento` · `rastreamento.html` | RN-10 | `...fluxoCompleto`, `RegraProgressoEntregaTest` |
| RF-12 | Confirmar entrega/recebimento: ENTREGUE, bolsas UTILIZADA, veículo liberado | HOSPITAL, HEMOCENTRO | `PATCH /api/hospital/requisicoes/{id}/recebimento`, `PATCH /api/v1/requisicoes/{id}/entrega` | RN-09 | `ConfirmarEntregaIntegrationTest`, `confirmar_entrega.feature` |
| RF-13 | Telemetria de temperatura da carga | HEMOCENTRO | `/api/v1/telemetria/*` | RN-06 | `Telemetria*Test` |
| RF-14 | Indicadores, risco de desabastecimento, descarte e rede | HEMOCENTRO | `/api/indicadores/*`, `gestor/*.html` | — | `Indicadores*Test`, `MetricasRede*Test` |
| RF-15 | Criar contas de hospital e de hemocentro | HEMOCENTRO | `GET/POST /api/usuarios` · `hemocentro/usuarios.html` | — | `...gestaoDeContas` |
| RF-16 | Descartar automaticamente bolsas vencidas (job diário às 03h) | sistema | `ValidadeService` | RN-03 | `ValidadeServiceTest` |

## 3. Requisitos não funcionais

| ID | Requisito | Como é atendido |
|---|---|---|
| RNF-01 | Autenticação stateless | Spring Security + JWT HS256 (Nimbus), sem sessão no servidor e sem CSRF (token no cabeçalho) |
| RNF-02 | Sem segredo padrão | `JWT_SECRET` obrigatório no perfil `prod` (mínimo de 32 caracteres); fora de prod é aleatório por execução. Contas demo só com `DEMO_SENHA` |
| RNF-03 | Senha protegida | BCrypt; e-mail inexistente e senha errada têm a mesma resposta e custo parecido (hash fictício) |
| RNF-04 | Isolamento entre hospitais | Tenant só do JWT; recurso de outro hospital = 404 |
| RNF-05 | Erros padronizados | `GlobalExceptionHandler`: 400 dados inválidos, 401 não autenticado, 403 perfil sem acesso, 404 inexistente, 409 transição inválida ou duplicado, 422 regra de negócio |
| RNF-06 | Testes no pipeline | CI roda `mvn verify` em todo PR e push na `main`; o Dockerfile roda os testes antes de gerar a imagem |
| RNF-07 | Disponibilidade | Render com health check em `/actuator/health`; o deploy só troca o tráfego com health 200 |
| RNF-08 | Acessibilidade | Rótulo em todos os campos, `aria-live` nos estados, `prefers-reduced-motion` nas animações |
| RNF-09 | Dados sintéticos | Nenhum dado pessoal real; CPF mascarado no painel do doador |

## 4. Arquitetura

```text
navegador (HTML + JS, token no sessionStorage/localStorage)
   │  Authorization: Bearer <jwt>
   ▼
SegurancaConfig (filtro JWT + regra por perfil)
   ▼
controller ──► service ──► domain.regras (Java puro, sem Spring/JPA)
                  │
                  ├──► algoritmos (FilaFEFO, IndiceEstoque, GrafoRotas/Dijkstra)
                  └──► repository (Spring Data JPA) ──► H2 (dev/teste) · PostgreSQL (prod)
```

- **`domain.regras`**: regras de negócio sem dependência de framework, reutilizáveis em outro serviço, num job ou num app. Os services só orquestram: buscam os dados, chamam a regra e gravam.
- **`algoritmos`**: estruturas de dados da disciplina de AED (heap FEFO, índice por tipo, Dijkstra).
- **`seguranca`**: configuração do Spring Security, emissão do token e leitura do usuário atual.

## 5. Máquina de estados da requisição (RN-09)

```text
PENDENTE ──alocar/aprovar──► ALOCADA | APROVADA ──despachar──► EM_ROTA ──confirmar──► ENTREGUE
    │                              │      └──────confirmar (retirada no balcão)──────────►┘
    └──────────cancelar────────────┴──► CANCELADA
```

ENTREGUE e CANCELADA são finais. Qualquer outra transição responde 409.

## 6. Contrato do rastreamento (RF-11)

`GET /api/v1/requisicoes/{id}/rastreamento` (HEMOCENTRO) e `GET /api/hospital/requisicoes/{id}/rastreamento` (HOSPITAL, só as próprias):

```json
{
  "requisicaoId": 4, "status": "EM_ROTA", "hospital": "Hospital Regional Norte",
  "trajeto": [{"no": "N0", "nome": "Hemocentro", "lat": -8.0476, "lng": -34.899},
              {"no": "N1", "nome": "Hospital Regional Norte", "lat": -8.0123, "lng": -34.8912}],
  "distanciaKm": 18.0, "posicaoAtual": {"lat": -8.04, "lng": -34.897}, "trechoAtual": 0, "percentual": 14.0,
  "saida": "2026-11-20T21:16:00", "previsaoChegada": "2026-11-20T21:34:00", "minutosRestantes": 15,
  "veiculoPlaca": "ROTA-1001", "temperaturaC": 5.2, "statusTemperatura": "OK",
  "googleMapsUrl": "https://www.google.com/maps/dir/?api=1&travelmode=driving&origin=...&destination=..."
}
```

Sem GPS real no escopo, a posição é estimada pelo tempo decorrido sobre o trajeto de Dijkstra (RN-10). A variável `RASTREAMENTO_FATOR_TEMPO` acelera o relógio na demonstração.

## 7. Fora do escopo (decisões registradas)

- Envio de e-mail: a recuperação de senha é feita pela equipe do hemocentro.
- GPS real do veículo e trânsito em tempo real dentro do app: o botão abre a rota no Google Maps.
- Limite de tentativas de login: recomendado antes de usar dados reais (ver [`big-o.md`](big-o.md), seção 4).

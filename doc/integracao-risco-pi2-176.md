# PI2-176 — Integração do risco de desabastecimento

## O que esta implementação entrega

- Integra a base da branch `luis/pi2-148-risco`, preservando os indicadores existentes.
- Publica `GET /api/v1/indicadores/risco-desabastecimento?dias=7` e mantém o alias `/api/indicadores/risco-desabastecimento`.
- Usa Poisson para calcular P(demanda > estoque atual), com média do período = lambda diário × dias.
- Conta bolsas disponíveis e não vencidas por tipo sanguíneo. O estoque é uma fotografia atual: não simula reposição, vencimentos futuros ou substituição entre tipos compatíveis.
- Retorna probabilidades entre 0 e 1, arredondadas a seis casas, ordenadas do maior risco para o menor.
- Retorna 400 para dias não positivos ou não inteiros e 503 quando o modelo não está configurado.
- Centraliza os erros no GlobalExceptionHandler.
- Corrige a validação de dias antes da consulta ao modelo e o cálculo com médias altas (underflow).

## Dependência da PI2-177 (Luis Felipe)

A implementação original recebe taxas diárias configuradas por tipo; ainda não estima essas taxas a partir do histórico de requisições. Sem os oito lambdas válidos, responde 503. Isso indica uma dependência real, e não uma probabilidade de risco igual a zero.

Propriedades previstas: `hemorede.indicadores.risco.lambda-diario.<TIPO>`, com os tipos A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS e O_NEG. Valores finitos, não negativos, em bolsas por dia. Não há valores demonstrativos configurados em produção.

Antes de concluir a tarefa, alinhar e integrar:

1. Janela de histórico e tratamento dos dias sem requisições.
2. Se demanda significa quantidade de bolsas solicitadas e quais status entram na contagem.
3. Se o modelo agrega hemocomponentes por tipo (como a base atual) ou precisa separá-los; estoque e demanda devem usar a mesma unidade e recorte.
4. Tratamento de ausência de histórico: não confundir ausência de dados com demanda comprovadamente zero.
5. Origem, atualização e validação dos lambdas definidos na metodologia.

Os valores usados nos testes são fixtures matemáticas, não estimativas da rede. A integração técnica pode ser revisada, mas PI2-176 e PI2-32 não devem ser consideradas integralmente concluídas antes da integração com a metodologia e da validação do resultado com histórico.

## PI2-178 — Taxa de descarte

Foi implementado `GET /api/v1/indicadores/descarte?dias=7`, agrupado por tipo sanguíneo e hemocomponente. A definição operacional usa a proporção entre bolsas com `status=DESCARTADA` e todas as bolsas cuja validade caiu no intervalo. A janela inclui hoje e os seis dias anteriores; sem dados no grupo, ele não é exibido e uma base vazia retorna `taxas: []`.

A entidade Bolsa não registra data efetiva nem motivo do descarte. Portanto, `dataValidade` é a referência temporal e o cálculo pressupõe que `DESCARTADA` corresponda ao vencimento; essa limitação está documentada em `doc/indicador-descarte-pi2-178.md` e deve ser confirmada na revisão da metodologia da PI2-177.

## Limites de responsabilidade

O painel (PI2-179) continua com Mariana. A metodologia (PI2-177) continua com Luis Felipe. Não foram alterados DataInitializer, entidades ou frontend.

## Validação

Executar com Java 17, na pasta rotavital:

```bash
mvn clean test
```

Cobertura adicionada: Poisson e horizonte, estoque reservado/vencido excluído, modelo incompleto, dias inválidos, média alta e overflow, contrato HTTP 200/400/503, alias de URL e preservação dos indicadores existentes.

Os dois cenários HU06 já excluídos por dependerem da PI2-181 continuam excluídos. A aprovação local não substitui conferir a execução no CI após o push.

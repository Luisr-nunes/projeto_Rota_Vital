# language: pt
# Contrato do Jira PI2-127/PI2-181: PATCH /api/v1/requisicoes/{id}/entrega.
@HU06
Funcionalidade: Confirmar recebimento das bolsas pelo hospital
  Como profissional responsavel pela entrega
  Quero confirmar o recebimento das bolsas
  Para concluir a requisicao e atualizar o estoque

  Cenario: Confirmar entrega de requisicao alocada
    Dado uma requisicao alocada com uma bolsa reservada
    Quando confirmo a entrega da requisicao
    Entao a API responde 200
    E a requisicao fica "ENTREGUE"
    E a bolsa "entrega" fica "UTILIZADA"
    E a requisicao possui a bolsa "entrega" associada

  Cenario: Impedir entrega de requisicao ainda pendente
    Dado uma requisicao pendente de 1 bolsa para receptor "A_POS"
    Quando confirmo a entrega da requisicao
    Entao a API responde 409
    E a requisicao fica "PENDENTE"
    E a requisicao nao possui bolsas associadas

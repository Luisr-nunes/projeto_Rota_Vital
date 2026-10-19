# language: pt
@HU08
Funcionalidade: Alocar bolsas compativeis em uma requisicao
  Como profissional do hemocentro
  Quero reservar bolsas compativeis seguindo FEFO e calcular a rota
  Para atender o hospital com menor desperdicio

  Cenario: Reservar a bolsa compativel que vence primeiro
    Dado uma requisicao pendente de 1 bolsa para receptor "A_POS"
    E uma bolsa "mais_nova" do tipo "A_POS" com validade em 10 dias
    E uma bolsa "vence_primeiro" do tipo "O_NEG" com validade em 2 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 200
    E a requisicao fica "ALOCADA"
    E a bolsa "vence_primeiro" fica "RESERVADA"
    E a bolsa "mais_nova" fica "DISPONIVEL"
    E a requisicao possui a bolsa "vence_primeiro" associada
    E a resposta inclui rota e custo

  Cenario: Rejeitar estoque incompativel
    Dado uma requisicao pendente de 1 bolsa para receptor "O_NEG"
    E uma bolsa "incompativel" do tipo "AB_POS" com validade em 10 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 422
    E a requisicao fica "PENDENTE"
    E a bolsa "incompativel" fica "DISPONIVEL"
    E a requisicao nao possui bolsas associadas

  Cenario: Rejeitar quantidade insuficiente sem reservar parcialmente
    Dado uma requisicao pendente de 2 bolsas para receptor "A_POS"
    E uma bolsa "unica" do tipo "O_NEG" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 409
    E a resposta informa 2 bolsas necessarias e 1 disponivel
    E a requisicao fica "PENDENTE"
    E a bolsa "unica" fica "DISPONIVEL"
    E a requisicao nao possui bolsas associadas

  Cenario: Ignorar bolsa vencida mesmo que seja compativel
    Dado uma requisicao pendente de 1 bolsa para receptor "A_POS"
    E uma bolsa "vencida" do tipo "O_NEG" com validade em -1 dias
    E uma bolsa "valida" do tipo "A_POS" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 200
    E a bolsa "vencida" fica "DISPONIVEL"
    E a bolsa "valida" fica "RESERVADA"
    E a requisicao possui a bolsa "valida" associada

  Cenario: Nao reservar bolsas quando o hospital nao possui rota
    Dado uma requisicao pendente de 1 bolsa para receptor "A_POS"
    E uma bolsa "disponivel" do tipo "A_POS" com validade em 5 dias
    E o hospital nao possui no no grafo
    Quando solicito a alocacao da requisicao
    Entao a API responde 422
    E a requisicao fica "PENDENTE"
    E a bolsa "disponivel" fica "DISPONIVEL"

  Cenario: Reverter o primeiro item quando faltar estoque para o segundo
    Dado uma requisicao pendente de 1 bolsa para receptor "A_POS"
    E a requisicao tambem solicita 1 bolsa para receptor "O_NEG"
    E uma bolsa "primeiro_item" do tipo "A_POS" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 422
    E a requisicao fica "PENDENTE"
    E a bolsa "primeiro_item" fica "DISPONIVEL"
    E a requisicao nao possui bolsas associadas

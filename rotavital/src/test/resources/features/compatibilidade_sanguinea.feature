# language: pt
# L2 (PI2): cenarios de aceitacao e de borda da compatibilidade ABO/Rh na alocacao (HU08).
# Reusa os steps de RequisicaoSteps; nao ha step novo. Escopo: concentrado de hemacias.
@HU08 @compatibilidade
Funcionalidade: Compatibilidade sanguinea na alocacao de bolsas
  Como profissional do hemocentro
  Quero que a alocacao respeite o grupo ABO, o fator Rh e a validade da bolsa
  Para nunca reservar uma bolsa que o paciente nao pode receber

  # ---------------------------------------------------------------------------
  # Rh negativo: receptor Rh- nunca recebe bolsa Rh+
  # ---------------------------------------------------------------------------

  @rh_negativo
  Esquema do Cenario: Receptor Rh negativo recusa bolsa Rh positivo
    Dado uma requisicao pendente de 1 bolsa para receptor "<receptor>"
    E uma bolsa "rh_positivo" do tipo "<doador>" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 422
    E a requisicao fica "PENDENTE"
    E a bolsa "rh_positivo" fica "DISPONIVEL"
    E a requisicao nao possui bolsas associadas

    Exemplos: mesmo grupo e doador universal Rh+ contra receptor Rh-
      | receptor | doador |
      | A_NEG    | A_POS  |
      | B_NEG    | B_POS  |
      | AB_NEG   | AB_POS |
      | O_NEG    | O_POS  |
      | AB_NEG   | O_POS  |

  @rh_negativo
  Esquema do Cenario: Receptor Rh positivo aceita bolsa Rh negativo do mesmo grupo
    Dado uma requisicao pendente de 1 bolsa para receptor "<receptor>"
    E uma bolsa "rh_negativo" do tipo "<doador>" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 200
    E a requisicao fica "ALOCADA"
    E a bolsa "rh_negativo" fica "RESERVADA"
    E a requisicao possui a bolsa "rh_negativo" associada

    Exemplos:
      | receptor | doador |
      | A_POS    | A_NEG  |
      | B_POS    | B_NEG  |
      | AB_POS   | AB_NEG |
      | O_POS    | O_NEG  |

  @rh_negativo
  Cenario: Receptor AB negativo ignora a bolsa AB positivo mesmo que ela venca antes
    Dado uma requisicao pendente de 2 bolsas para receptor "AB_NEG"
    E uma bolsa "ab_positivo" do tipo "AB_POS" com validade em 1 dias
    E uma bolsa "a_negativo" do tipo "A_NEG" com validade em 6 dias
    E uma bolsa "o_negativo" do tipo "O_NEG" com validade em 3 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 200
    E a requisicao fica "ALOCADA"
    E a bolsa "o_negativo" fica "RESERVADA"
    E a bolsa "a_negativo" fica "RESERVADA"
    E a bolsa "ab_positivo" fica "DISPONIVEL"

  # ---------------------------------------------------------------------------
  # Desempate e FEFO entre tipos compativeis
  # ---------------------------------------------------------------------------

  @rh_negativo
  Cenario: Com a mesma validade, o tipo identico sai antes do O negativo
    Dado uma requisicao pendente de 1 bolsa para receptor "A_POS"
    E uma bolsa "o_negativo" do tipo "O_NEG" com validade em 5 dias
    E uma bolsa "identica" do tipo "A_POS" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 200
    E a bolsa "identica" fica "RESERVADA"
    E a bolsa "o_negativo" fica "DISPONIVEL"

  @rh_negativo
  Cenario: A validade mais proxima vence o tipo identico
    Dado uma requisicao pendente de 1 bolsa para receptor "B_NEG"
    E uma bolsa "identica" do tipo "B_NEG" com validade em 8 dias
    E uma bolsa "o_negativo" do tipo "O_NEG" com validade em 2 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 200
    E a bolsa "o_negativo" fica "RESERVADA"
    E a bolsa "identica" fica "DISPONIVEL"

  # ---------------------------------------------------------------------------
  # Estoque com apenas O negativo
  # ---------------------------------------------------------------------------

  @so_o_negativo
  Esquema do Cenario: O negativo sozinho no estoque atende qualquer receptor
    Dado uma requisicao pendente de 1 bolsa para receptor "<receptor>"
    E uma bolsa "universal" do tipo "O_NEG" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 200
    E a requisicao fica "ALOCADA"
    E a bolsa "universal" fica "RESERVADA"
    E a requisicao possui a bolsa "universal" associada
    E a resposta inclui rota e custo

    Exemplos: os oito receptores
      | receptor |
      | O_NEG    |
      | O_POS    |
      | A_NEG    |
      | A_POS    |
      | B_NEG    |
      | B_POS    |
      | AB_NEG   |
      | AB_POS   |

  @so_o_negativo
  Cenario: Doador universal nao e receptor universal
    Dado uma requisicao pendente de 1 bolsa para receptor "O_NEG"
    E uma bolsa "o_positivo" do tipo "O_POS" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 422
    E a requisicao fica "PENDENTE"
    E a bolsa "o_positivo" fica "DISPONIVEL"
    E a requisicao nao possui bolsas associadas

  @so_o_negativo
  Cenario: Uma unica bolsa O negativo nao atende dois pacientes
    Dado uma requisicao pendente de 2 bolsas para receptor "A_POS"
    E uma bolsa "unica" do tipo "O_NEG" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 409
    E a resposta informa 2 bolsas necessarias e 1 disponivel
    E a requisicao fica "PENDENTE"
    E a bolsa "unica" fica "DISPONIVEL"
    E a requisicao nao possui bolsas associadas

  @so_o_negativo
  Cenario: A mesma bolsa O negativo nao e reservada para dois itens da requisicao
    Dado uma requisicao pendente de 1 bolsa para receptor "A_POS"
    E a requisicao tambem solicita 1 bolsa para receptor "B_NEG"
    E uma bolsa "unica" do tipo "O_NEG" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 422
    E a requisicao fica "PENDENTE"
    E a bolsa "unica" fica "DISPONIVEL"
    E a requisicao nao possui bolsas associadas

  # ---------------------------------------------------------------------------
  # Bolsa vencida
  # ---------------------------------------------------------------------------

  @bolsa_vencida
  Esquema do Cenario: Bolsa vencida nunca e reservada, mesmo sendo a unica compativel
    Dado uma requisicao pendente de 1 bolsa para receptor "A_POS"
    E uma bolsa "vencida" do tipo "O_NEG" com validade em <dias> dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 422
    E a requisicao fica "PENDENTE"
    E a bolsa "vencida" fica "DISPONIVEL"
    E a requisicao nao possui bolsas associadas

    Exemplos: vencida ontem e vencida ha um mes
      | dias |
      | -1   |
      | -30  |

  @bolsa_vencida
  Cenario: Bolsa que vence hoje ainda pode ser reservada
    Dado uma requisicao pendente de 1 bolsa para receptor "A_POS"
    E uma bolsa "vence_hoje" do tipo "O_NEG" com validade em 0 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 200
    E a requisicao fica "ALOCADA"
    E a bolsa "vence_hoje" fica "RESERVADA"

  @bolsa_vencida
  Cenario: Bolsa vencida nao conta na quantidade disponivel
    Dado uma requisicao pendente de 2 bolsas para receptor "A_POS"
    E uma bolsa "vencida" do tipo "O_NEG" com validade em -1 dias
    E uma bolsa "valida" do tipo "A_POS" com validade em 3 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 409
    E a resposta informa 2 bolsas necessarias e 1 disponivel
    E a requisicao fica "PENDENTE"
    E a bolsa "vencida" fica "DISPONIVEL"
    E a bolsa "valida" fica "DISPONIVEL"
    E a requisicao nao possui bolsas associadas

  @bolsa_vencida
  Cenario: Entre duas bolsas do mesmo tipo, a vencida e ignorada e a valida e reservada
    Dado uma requisicao pendente de 1 bolsa para receptor "O_NEG"
    E uma bolsa "vencida" do tipo "O_NEG" com validade em -3 dias
    E uma bolsa "valida" do tipo "O_NEG" com validade em 9 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 200
    E a bolsa "valida" fica "RESERVADA"
    E a bolsa "vencida" fica "DISPONIVEL"
    E a requisicao possui a bolsa "valida" associada

  @bolsa_vencida @rh_negativo
  Cenario: Receptor Rh negativo sem bolsa Rh negativo valida nao recebe Rh positivo
    Dado uma requisicao pendente de 1 bolsa para receptor "A_NEG"
    E uma bolsa "o_negativo_vencida" do tipo "O_NEG" com validade em -1 dias
    E uma bolsa "a_positivo" do tipo "A_POS" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 422
    E a requisicao fica "PENDENTE"
    E a bolsa "o_negativo_vencida" fica "DISPONIVEL"
    E a bolsa "a_positivo" fica "DISPONIVEL"
    E a requisicao nao possui bolsas associadas

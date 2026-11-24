# Seção B — Modelo tático do contexto Estoque

O Estoque é core (A.1) e guarda a regra mais difícil do produto: reservar a bolsa certa sem nunca entregar uma vencida. Base: [`doc/modelo-dominio.md`](../../doc/modelo-dominio.md) e [`alocar_bolsa.feature`](../../rotavital/src/test/resources/features/alocar_bolsa.feature).

## B.1 O agregado Estoque com Bolsa

| Elemento | Papel |
|---|---|
| **Estoque** | **Raiz.** Único ponto de entrada para mudar uma bolsa: quem quer reservar pede ao Estoque. |
| **Bolsa** | Entidade interna, com identidade própria, que só existe dentro de um Estoque. Ninguém fora guarda referência direta a ela. |
| TipoSanguineo, HemoComponente | Objetos de valor (kernel compartilhado), imutáveis. O tipo sabe dizer se doa para outro; o hemocomponente define validade e refrigeração. |
| Validade | Objeto de valor: data limite de uso (coleta mais dias do hemocomponente); responde `vencidaEm(data)`. |
| StatusBolsa, Quantidade | Objetos de valor: `DISPONIVEL`, `RESERVADA`, `EM_TRANSITO`, `UTILIZADA`, `DESCARTADA`; quantidade inteira maior que zero. |

Ficam **fora** do agregado: o Hospital (o Estoque guarda só o identificador), o Doador (o Estoque recebe `BolsaColetada` com tipo, componente e data da coleta) e a Requisição (o Estoque recebe um pedido de reserva). A fronteira do agregado é a fronteira da transação: uma reserva muda várias bolsas do mesmo estoque numa única operação, e as invariantes abaixo olham para o conjunto, não para uma bolsa sozinha.

## B.2 Invariantes que a raiz protege

| # | Invariante | Quem garante | Prova |
|---|---|---|---|
| I1 | Bolsa vencida nunca é reservada | `Estoque.reservar` descarta bolsa com `Validade.vencidaEm(hoje)` | Cenário "Ignorar bolsa vencida" (B.4) |
| I2 | Só bolsa compatível ABO/Rh atende o receptor | `TipoSanguineo.doaPara(receptor)` dentro de `reservar` | `compatibilidade_sanguinea.feature` |
| I3 | FEFO: entre as compatíveis, sai primeiro a que vence antes | Ordenação por validade crescente em `reservar` | Cenário "Reservar a bolsa compativel que vence primeiro" |
| I4 | Tudo ou nada: sem bolsas suficientes, nenhuma muda de status | `reservar` verifica a quantidade antes de alterar qualquer bolsa e lança `EstoqueInsuficiente` | Cenários "Rejeitar quantidade insuficiente sem reservar parcialmente" e "Reverter o primeiro item..." |
| I5 | Só bolsa `DISPONIVEL` pode ser reservada | `Bolsa.reservar()` recusa outro status | Teste dedicado a escrever na Parte 3 |

Hoje o "tudo ou nada" vem do `@Transactional` do banco; no agregado ele passa a ser do próprio objeto de domínio, o que importa quando o Estoque virar um serviço separado (Seção D).

## B.3 História âncora

> **Como** Marcos, técnico do banco de sangue de um hemocentro, de plantão,
> **quero** reservar, para o pedido de um hospital, as bolsas compatíveis que vencem primeiro,
> **para** atender o pedido sem usar bolsa vencida e sem deixar uma bolsa mais antiga perder a validade.

Corresponde à HU08 e está ligada às invariantes I1 a I4. Critérios de aceite:

1. **FEFO.** *Dado* um pedido de 1 bolsa para receptor A+, uma bolsa A+ que vence em 10 dias e uma O− que vence em 2, *quando* o Marcos solicita a reserva, *então* a O− é reservada, a A+ continua disponível e a resposta traz rota e custo.
2. **Vencida.** *Dado* uma bolsa O− vencida ontem e uma A+ válida por 5 dias, *quando* ele solicita a reserva para A+, *então* só a válida é reservada e a vencida não muda.
3. **Insuficiente.** *Dado* um pedido de 2 bolsas e só 1 compatível, *quando* ele solicita a reserva, *então* o sistema informa que são necessárias 2 e há 1, e nenhuma bolsa é reservada.
4. **Incompatível.** *Dado* um pedido para receptor O− e só uma bolsa AB+, *quando* ele solicita a reserva, *então* o pedido é rejeitado e a AB+ continua disponível.

## B.4 Cenário Gherkin (invariante I1)

O cenário está em `alocar_bolsa.feature` e já roda no Cucumber dentro do `mvn test`. Na Parte 3 o mesmo texto passa a rodar também contra o domínio puro (`Estoque.reservar`), sem Spring nem banco.

```gherkin
# language: pt
@HU08
Funcionalidade: Alocar bolsas compativeis em uma requisicao
  Como profissional do hemocentro
  Quero reservar bolsas compativeis seguindo FEFO e calcular a rota
  Para atender o hospital com menor desperdicio

  Cenario: Ignorar bolsa vencida mesmo que seja compativel
    Dado uma requisicao pendente de 1 bolsa para receptor "A_POS"
    E uma bolsa "vencida" do tipo "O_NEG" com validade em -1 dias
    E uma bolsa "valida" do tipo "A_POS" com validade em 5 dias
    Quando solicito a alocacao da requisicao
    Entao a API responde 200
    E a bolsa "vencida" fica "DISPONIVEL"
    E a bolsa "valida" fica "RESERVADA"
    E a requisicao possui a bolsa "valida" associada
```

## B.5 Esboço do agregado (Java puro, sem Spring nem JPA)

O agregado tem comportamento, não só `get` e `set`: as regras vivem na raiz.

```java
public class Estoque {                       // raiz do agregado
    private final List<Bolsa> bolsas;

    public List<Bolsa> reservar(PedidoDeReserva pedido, LocalDate hoje) {
        List<Bolsa> candidatas = bolsas.stream()
            .filter(Bolsa::estaDisponivel)                                // I5
            .filter(b -> b.componente() == pedido.componente())
            .filter(b -> !b.validade().vencidaEm(hoje))                   // I1
            .filter(b -> b.tipo().doaPara(pedido.tipoDoReceptor()))       // I2
            .sorted(Comparator.comparing(b -> b.validade().data()))       // I3
            .limit(pedido.quantidade().valor()).toList();
        if (candidatas.size() < pedido.quantidade().valor())              // I4
            throw new EstoqueInsuficiente(pedido.quantidade().valor(), candidatas.size());
        candidatas.forEach(Bolsa::reservar);
        return candidatas;
    }
}
```

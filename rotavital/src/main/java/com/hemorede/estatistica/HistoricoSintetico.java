package com.hemorede.estatistica;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Histórico sintético para as telas da disciplina de Estatística.
 *
 * <p>O banco real tem poucas dezenas de registros, pouco para correlação e regressão.
 * Estes dois conjuntos simulam 12 meses de operação com relações plausíveis entre as
 * variáveis. A semente é fixa: o mesmo dado sai sempre igual, então qualquer número da
 * tela pode ser conferido à mão ou no Colab.</p>
 *
 * <ul>
 *   <li><b>diário</b> (365 dias): requisições de bolsas por dia (target), acidentes de
 *       trânsito, cirurgias eletivas, doações, temperatura, feriado, fim de semana e dia do
 *       mês. Requisições dependem de acidentes e cirurgias; temperatura e dia do mês não
 *       influem nelas, de propósito, para a seleção de features ter o que descartar.</li>
 *   <li><b>entregas</b> (200 entregas): tempo de entrega (target), distância, trânsito e
 *       temperatura da caixa térmica na chegada.</li>
 * </ul>
 */
public final class HistoricoSintetico {

    public static final long SEMENTE = 20261121L;
    public static final LocalDate INICIO = LocalDate.of(2025, 11, 1);
    public static final int DIAS = 365;
    public static final int ENTREGAS = 200;

    private static final Set<LocalDate> FERIADOS = Set.of(
            LocalDate.of(2025, 11, 15), LocalDate.of(2025, 11, 20), LocalDate.of(2025, 12, 25),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17),
            LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 21), LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 6, 4), LocalDate.of(2026, 9, 7), LocalDate.of(2026, 10, 12));

    private static final List<Conjunto> CONJUNTOS = List.of(gerarDiario(), gerarEntregas());

    private HistoricoSintetico() {
    }

    public static List<Conjunto> conjuntos() {
        return CONJUNTOS;
    }

    private static Conjunto gerarDiario() {
        Random rnd = new Random(SEMENTE);
        double[] req = new double[DIAS];
        double[] acid = new double[DIAS];
        double[] cir = new double[DIAS];
        double[] doa = new double[DIAS];
        double[] temp = new double[DIAS];
        double[] fer = new double[DIAS];
        double[] fds = new double[DIAS];
        double[] diaMes = new double[DIAS];
        for (int i = 0; i < DIAS; i++) {
            LocalDate d = INICIO.plusDays(i);
            DayOfWeek dow = d.getDayOfWeek();
            boolean fimDeSemana = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
            boolean feriado = FERIADOS.contains(d);
            fds[i] = fimDeSemana ? 1 : 0;
            fer[i] = feriado ? 1 : 0;
            diaMes[i] = d.getDayOfMonth();
            // Recife: verão mais quente por volta de janeiro/fevereiro.
            temp[i] = umaCasa(27 + 2.5 * Math.cos(2 * Math.PI * (i - 90) / 365.0) + 1.2 * rnd.nextGaussian());
            acid[i] = naoNegativo(5 + 3 * fds[i] + 4 * fer[i] + 1.5 * rnd.nextGaussian());
            cir[i] = naoNegativo(fimDeSemana || feriado ? 5 + 2 * rnd.nextGaussian() : 20 + 4 * rnd.nextGaussian());
            double efeitoDia = dow == DayOfWeek.SATURDAY ? 8 : dow == DayOfWeek.SUNDAY ? -20 : 0;
            doa[i] = naoNegativo(48 - 1.5 * (temp[i] - 27) - 18 * fer[i] + efeitoDia + 6 * rnd.nextGaussian());
            req[i] = naoNegativo(8 + 1.8 * acid[i] + 0.45 * cir[i] + 2.5 * rnd.nextGaussian());
        }
        return new Conjunto("diario", "Histórico diário", DIAS + " dias de operação simulada, de "
                + INICIO + " a " + INICIO.plusDays(DIAS - 1), List.of(
                new Coluna("diario.requisicoes", "Bolsas requisitadas no dia", "bolsas", "quantitativa discreta", req),
                new Coluna("diario.acidentes", "Acidentes de trânsito na região", "ocorrências", "quantitativa discreta", acid),
                new Coluna("diario.cirurgias", "Cirurgias eletivas agendadas", "cirurgias", "quantitativa discreta", cir),
                new Coluna("diario.doacoes", "Doações recebidas", "doações", "quantitativa discreta", doa),
                new Coluna("diario.temperatura", "Temperatura média", "°C", "quantitativa contínua", temp),
                new Coluna("diario.feriado", "Feriado", "0 = não, 1 = sim", "qualitativa codificada (0/1)", fer),
                new Coluna("diario.fimDeSemana", "Fim de semana", "0 = não, 1 = sim", "qualitativa codificada (0/1)", fds),
                new Coluna("diario.diaDoMes", "Dia do mês", "dia", "quantitativa discreta", diaMes)));
    }

    private static Conjunto gerarEntregas() {
        Random rnd = new Random(SEMENTE + 1);
        double[] tempo = new double[ENTREGAS];
        double[] dist = new double[ENTREGAS];
        double[] transito = new double[ENTREGAS];
        double[] caixa = new double[ENTREGAS];
        for (int i = 0; i < ENTREGAS; i++) {
            dist[i] = umaCasa(2 + 33 * rnd.nextDouble());
            transito[i] = 1 + rnd.nextInt(10);
            // Atrasos ocasionais longos (cauda à direita), como em qualquer entrega urbana.
            tempo[i] = umaCasa(6 + 1.9 * dist[i] + 1.4 * transito[i] + 3 * Math.exp(0.7 * rnd.nextGaussian()));
            caixa[i] = Math.round((3.2 + 0.035 * tempo[i] + 0.35 * rnd.nextGaussian()) * 100) / 100.0;
        }
        return new Conjunto("entregas", "Entregas", ENTREGAS + " entregas simuladas do hemocentro aos hospitais", List.of(
                new Coluna("entregas.tempo", "Tempo de entrega", "min", "quantitativa contínua", tempo),
                new Coluna("entregas.distancia", "Distância percorrida", "km", "quantitativa contínua", dist),
                new Coluna("entregas.transito", "Índice de trânsito", "1 a 10", "quantitativa discreta", transito),
                new Coluna("entregas.temperaturaCaixa", "Temperatura da caixa na chegada", "°C", "quantitativa contínua", caixa)));
    }

    private static double naoNegativo(double v) {
        return Math.max(0, Math.round(v));
    }

    private static double umaCasa(double v) {
        return Math.round(v * 10) / 10.0;
    }

    /** Uma variável: identificador, nome para a tela, unidade, tipo e os valores. */
    public record Coluna(String id, String nome, String unidade, String tipo, double[] valores) {
    }

    /** Variáveis observadas nas mesmas linhas, por isso podem ser correlacionadas entre si. */
    public record Conjunto(String id, String nome, String descricao, List<Coluna> colunas) {
    }
}

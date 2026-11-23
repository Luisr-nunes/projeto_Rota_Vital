package com.hemorede.estatistica;

import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.requisicao.domain.model.ItemRequisicao;
import com.hemorede.estatistica.EstatisticaDtos.Descritiva;
import com.hemorede.estatistica.EstatisticaDtos.FeatureAvaliada;
import com.hemorede.estatistica.EstatisticaDtos.LinhaMemoria;
import com.hemorede.estatistica.EstatisticaDtos.Matriz;
import com.hemorede.estatistica.EstatisticaDtos.Par;
import com.hemorede.estatistica.EstatisticaDtos.Previsao;
import com.hemorede.estatistica.EstatisticaDtos.Somatorios;
import com.hemorede.estatistica.EstatisticaDtos.Variavel;
import com.hemorede.estatistica.HistoricoSintetico.Coluna;
import com.hemorede.estatistica.HistoricoSintetico.Conjunto;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.requisicao.infrastructure.repository.RequisicaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Estatística descritiva, correlação e previsão sobre os dados do sistema (fonte "real")
 * e o histórico sintético (fonte "sintetico"). As variáveis reais vêm de tabelas com
 * linhas diferentes (bolsas, requisições, tipos), então só entram na descritiva; correlação
 * e previsão exigem variáveis medidas nas mesmas linhas.
 */
@Service
public class EstatisticaService {

    /** Feature entra na seleção a partir de correlação moderada com o target. */
    public static final double LIMIAR_SELECAO = 0.3;
    /** Duas features selecionadas com |r| a partir disto entre si repetem a mesma informação. */
    public static final double LIMIAR_REDUNDANCIA = 0.8;
    private static final int LINHAS_MEMORIA = 8;

    private final BolsaRepository bolsaRepository;
    private final RequisicaoRepository requisicaoRepository;

    public EstatisticaService(BolsaRepository bolsaRepository, RequisicaoRepository requisicaoRepository) {
        this.bolsaRepository = bolsaRepository;
        this.requisicaoRepository = requisicaoRepository;
    }

    @Transactional(readOnly = true)
    public List<Variavel> variaveis() {
        List<Variavel> lista = new ArrayList<>();
        for (Conjunto c : todosConjuntos()) {
            for (Coluna col : c.colunas()) {
                lista.add(variavel(c, col));
            }
        }
        return lista;
    }

    @Transactional(readOnly = true)
    public Descritiva descritiva(String id) {
        Localizada v = localizar(id);
        double[] x = v.coluna().valores();
        if (x.length == 0) {
            throw new IllegalArgumentException("Ainda não há dados para \"" + v.coluna().nome() + "\".");
        }
        boolean amostra = x.length >= 2;
        double q1 = Estatistica.quantil(x, 0.25);
        double q3 = Estatistica.quantil(x, 0.75);
        double iqr = q3 - q1;
        double li = q1 - 1.5 * iqr;
        double ls = q3 + 1.5 * iqr;
        List<Estatistica.Classe> hist = Estatistica.histograma(x);
        double[] pontos = hist.stream().mapToDouble(Estatistica.Classe::pontoMedio).toArray();
        double[] freq = hist.stream().mapToDouble(Estatistica.Classe::frequencia).toArray();
        Estatistica.Classe modal = hist.stream().max(Comparator.comparingInt(Estatistica.Classe::frequencia)).orElse(null);
        double media = Estatistica.media(x);
        double mediana = Estatistica.mediana(x);
        Double g1 = Estatistica.assimetria(x);
        Double g2 = Estatistica.curtose(x);
        return new Descritiva(
                variavel(v.conjunto(), v.coluna()),
                Estatistica.soma(x), media, mediana, Estatistica.modas(x), modal,
                Estatistica.mediaPonderada(pontos, freq),
                min(x), max(x), max(x) - min(x), q1, q3, iqr,
                amostra ? Estatistica.variancia(x) : null,
                Estatistica.varianciaPopulacional(x),
                amostra ? Estatistica.desvioPadrao(x) : null,
                amostra ? Estatistica.coeficienteVariacao(x) : null,
                g1, Estatistica.leituraAssimetria(g1), g2, Estatistica.leituraCurtose(g2),
                relacaoMediaMediana(media, mediana, amostra ? Estatistica.desvioPadrao(x) : 0),
                li, ls, (int) Arrays.stream(x).filter(val -> val < li || val > ls).count(),
                hist,
                Arrays.stream(x).limit(12).boxed().toList());
    }

    public Matriz matriz(String conjuntoId) {
        Conjunto c = HistoricoSintetico.conjuntos().stream()
                .filter(k -> k.id().equals(conjuntoId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Conjunto não encontrado: " + conjuntoId
                        + ". Use diario ou entregas."));
        List<List<Double>> r = new ArrayList<>();
        for (Coluna a : c.colunas()) {
            List<Double> linha = new ArrayList<>();
            for (Coluna b : c.colunas()) {
                linha.add(Estatistica.pearson(a.valores(), b.valores()));
            }
            r.add(linha);
        }
        return new Matriz(c.id(), c.nome(), c.descricao(),
                c.colunas().stream().map(col -> variavel(c, col)).toList(), r);
    }

    public Par par(String idX, String idY) {
        Localizada lx = localizarCorrelacionavel(idX);
        Localizada ly = localizarCorrelacionavel(idY);
        if (!lx.conjunto().id().equals(ly.conjunto().id())) {
            throw new IllegalArgumentException("x e y precisam ser do mesmo conjunto, observados nas mesmas linhas.");
        }
        double[] x = lx.coluna().valores();
        double[] y = ly.coluna().valores();
        double mx = Estatistica.media(x);
        double my = Estatistica.media(y);
        double sx = 0, sy = 0, sxy = 0, sx2 = 0, sy2 = 0, sdxdy = 0, sdx2 = 0, sdy2 = 0;
        List<LinhaMemoria> memoria = new ArrayList<>();
        List<double[]> pontos = new ArrayList<>(x.length);
        for (int i = 0; i < x.length; i++) {
            double dx = x[i] - mx;
            double dy = y[i] - my;
            sx += x[i];
            sy += y[i];
            sxy += x[i] * y[i];
            sx2 += x[i] * x[i];
            sy2 += y[i] * y[i];
            sdxdy += dx * dy;
            sdx2 += dx * dx;
            sdy2 += dy * dy;
            if (i < LINHAS_MEMORIA) {
                memoria.add(new LinhaMemoria(x[i], y[i], dx, dy, dx * dy, dx * dx, dy * dy));
            }
            pontos.add(new double[]{x[i], y[i]});
        }
        Double r = Estatistica.pearson(x, y);
        return new Par(variavel(lx.conjunto(), lx.coluna()), variavel(ly.conjunto(), ly.coluna()),
                x.length, mx, my, Estatistica.desvioPadrao(x), Estatistica.desvioPadrao(y),
                Estatistica.covariancia(x, y), r, Estatistica.pearsonPorSomatorios(x, y),
                r == null ? null : r * r, Estatistica.forca(r), Estatistica.direcao(r),
                Estatistica.regressao(x, y),
                new Somatorios(sx, sy, sxy, sx2, sy2, sdxdy, sdx2, sdy2), memoria, pontos);
    }

    /**
     * Seleção de features para o target: avalia |r| de cada variável do conjunto contra o
     * target, marca as que passam do limiar, aponta pares redundantes entre as selecionadas e
     * monta a reta com a melhor delas. Com {@code x}, devolve também ŷ.
     */
    public Previsao previsao(String alvoId, Double x) {
        Localizada alvo = localizarCorrelacionavel(alvoId);
        double[] y = alvo.coluna().valores();
        List<FeatureAvaliada> avaliadas = new ArrayList<>();
        for (Coluna f : alvo.conjunto().colunas()) {
            if (f.id().equals(alvoId)) {
                continue;
            }
            Double r = Estatistica.pearson(f.valores(), y);
            boolean sel = r != null && Math.abs(r) >= LIMIAR_SELECAO;
            String motivo = sel
                    ? "|r| = " + fmt(Math.abs(r)) + " a partir de " + fmt(LIMIAR_SELECAO) + ": ajuda a prever"
                    : "|r| abaixo de " + fmt(LIMIAR_SELECAO) + ": pouca relação linear com o target";
            avaliadas.add(new FeatureAvaliada(variavel(alvo.conjunto(), f), r, r == null ? null : r * r,
                    Estatistica.forca(r), Estatistica.direcao(r), sel, motivo));
        }
        avaliadas.sort(Comparator.comparingDouble((FeatureAvaliada f) -> f.r() == null ? 0 : Math.abs(f.r())).reversed());

        Map<String, Coluna> porId = alvo.conjunto().colunas().stream()
                .collect(Collectors.toMap(Coluna::id, Function.identity()));
        List<FeatureAvaliada> selecionadas = avaliadas.stream().filter(FeatureAvaliada::selecionada).toList();
        List<List<String>> redundantes = new ArrayList<>();
        for (int i = 0; i < selecionadas.size(); i++) {
            for (int j = i + 1; j < selecionadas.size(); j++) {
                Double r = Estatistica.pearson(porId.get(selecionadas.get(i).variavel().id()).valores(),
                        porId.get(selecionadas.get(j).variavel().id()).valores());
                if (r != null && Math.abs(r) >= LIMIAR_REDUNDANCIA) {
                    redundantes.add(List.of(selecionadas.get(i).variavel().id(), selecionadas.get(j).variavel().id()));
                }
            }
        }

        FeatureAvaliada melhor = avaliadas.isEmpty() || avaliadas.get(0).r() == null ? null : avaliadas.get(0);
        Estatistica.Reta reta = melhor == null ? null
                : Estatistica.regressao(porId.get(melhor.variavel().id()).valores(), y);
        Double yPrevisto = reta != null && x != null ? reta.prever(x) : null;
        return new Previsao(variavel(alvo.conjunto(), alvo.coluna()), LIMIAR_SELECAO, avaliadas, redundantes,
                melhor == null ? null : melhor.variavel(), reta, melhor == null ? null : melhor.r2(), x, yPrevisto);
    }

    // ---------- dados reais ----------

    private Conjunto conjuntoReal() {
        LocalDate hoje = LocalDate.now();
        List<Bolsa> disponiveis = bolsaRepository.findByStatus(StatusBolsa.DISPONIVEL);
        double[] dias = disponiveis.stream().map(Bolsa::getDataValidade).filter(Objects::nonNull)
                .mapToDouble(d -> ChronoUnit.DAYS.between(hoje, d)).toArray();
        Map<TipoSanguineo, Long> porTipo = disponiveis.stream().filter(b -> b.getTipoSanguineo() != null)
                .collect(Collectors.groupingBy(Bolsa::getTipoSanguineo, Collectors.counting()));
        double[] estoquePorTipo = Arrays.stream(TipoSanguineo.values())
                .mapToDouble(t -> porTipo.getOrDefault(t, 0L)).toArray();
        double[] bolsasPorRequisicao = requisicaoRepository.findAll().stream()
                .mapToDouble(r -> r.getItens().stream().map(ItemRequisicao::getQuantidade)
                        .filter(Objects::nonNull).mapToInt(Integer::intValue).sum())
                .toArray();
        return new Conjunto("real", "Dados do sistema", "Estoque e requisições registrados no banco", List.of(
                new Coluna("real.diasAteVencimento", "Dias até o vencimento das bolsas em estoque", "dias",
                        "quantitativa discreta", dias),
                new Coluna("real.estoquePorTipo", "Bolsas disponíveis por tipo sanguíneo (8 tipos)", "bolsas",
                        "quantitativa discreta", estoquePorTipo),
                new Coluna("real.bolsasPorRequisicao", "Bolsas pedidas por requisição", "bolsas",
                        "quantitativa discreta", bolsasPorRequisicao)));
    }

    // ---------- apoio ----------

    private List<Conjunto> todosConjuntos() {
        List<Conjunto> todos = new ArrayList<>();
        todos.add(conjuntoReal());
        todos.addAll(HistoricoSintetico.conjuntos());
        return todos;
    }

    private Localizada localizar(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Informe a variável.");
        }
        List<Conjunto> conjuntos = id.startsWith("real.") ? List.of(conjuntoReal()) : HistoricoSintetico.conjuntos();
        for (Conjunto c : conjuntos) {
            for (Coluna col : c.colunas()) {
                if (col.id().equals(id)) {
                    return new Localizada(c, col);
                }
            }
        }
        throw new IllegalArgumentException("Variável não encontrada: " + id);
    }

    private Localizada localizarCorrelacionavel(String id) {
        Localizada l = localizar(id);
        if ("real".equals(l.conjunto().id())) {
            throw new IllegalArgumentException("\"" + l.coluna().nome()
                    + "\" não tem par observado nas mesmas linhas; use as variáveis do histórico diário ou de entregas.");
        }
        return l;
    }

    private static Variavel variavel(Conjunto c, Coluna col) {
        boolean real = "real".equals(c.id());
        return new Variavel(col.id(), col.nome(), col.unidade(), col.tipo(), c.id(),
                real ? "real" : "sintetico", col.valores().length, !real);
    }

    private static String relacaoMediaMediana(double media, double mediana, double desvio) {
        // Diferença pequena perto do desvio padrão conta como "praticamente iguais".
        if (desvio == 0 || Math.abs(media - mediana) < 0.1 * desvio) {
            return "média e mediana praticamente iguais: distribuição equilibrada";
        }
        return media > mediana
                ? "média maior que a mediana: valores altos puxam a média para cima"
                : "média menor que a mediana: valores baixos puxam a média para baixo";
    }

    private static double min(double[] x) {
        return Arrays.stream(x).min().orElse(0);
    }

    private static double max(double[] x) {
        return Arrays.stream(x).max().orElse(0);
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", v);
    }

    private record Localizada(Conjunto conjunto, Coluna coluna) {
    }
}

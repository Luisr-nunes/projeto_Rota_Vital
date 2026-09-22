package com.hemorede.relatorio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corretude da operação escolhida para a entrega de SO: a versão
 * sequencial e a versão com threads de plataforma, com diferentes
 * números de threads, têm que devolver exatamente o mesmo
 * {@link com.hemorede.dto.RelatorioHistoricoResponse.Resultado}, se
 * divergirem, há uma race condition na fusão dos parciais. (A comparação
 * opcional com virtual threads do Java 21 tem seu próprio teste, fora
 * deste módulo, ver {@code scripts/virtual-threads/}.)
 */
class RelatorioHistoricoServiceTest {

    private final RelatorioHistoricoService service = new RelatorioHistoricoService();
    private final GeradorHistoricoRequisicoes gerador = new GeradorHistoricoRequisicoes();

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 8, 16})
    void versaoComThreadsDevolveOMesmoResultadoQueASequencial(int numeroDeThreads) {
        List<RegistroHistoricoRequisicao> registros = gerador.obter(50_000);

        var resultadoSequencial = service.processarSequencial(registros);
        var resultadoParalelo = service.processarComThreads(registros, numeroDeThreads);

        assertThat(resultadoParalelo).isEqualTo(resultadoSequencial);
    }

    @Test
    void particionamentoComMaisFatiasDoQueRegistrosNaoQuebra() {
        List<RegistroHistoricoRequisicao> registros = gerador.obter(5);

        var resultadoSequencial = service.processarSequencial(registros);
        var resultadoParalelo = service.processarComThreads(registros, 32);

        assertThat(resultadoParalelo).isEqualTo(resultadoSequencial);
        assertThat(resultadoParalelo.totalRegistros()).isEqualTo(5);
    }

    @Test
    void geradorEhDeterministico() {
        gerador.limparCache();
        List<RegistroHistoricoRequisicao> primeiraGeracao = gerador.obter(1_000);
        gerador.limparCache();
        List<RegistroHistoricoRequisicao> segundaGeracao = gerador.obter(1_000);

        assertThat(primeiraGeracao).isEqualTo(segundaGeracao);
    }

    @Test
    void totalDeRegistrosPorCategoriaBateComOTamanhoDoHistorico() {
        List<RegistroHistoricoRequisicao> registros = gerador.obter(10_000);
        var resultado = service.processarSequencial(registros);

        assertThat(resultado.totalRegistros()).isEqualTo(10_000);
        assertThat(resultado.porStatus().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(10_000);
        assertThat(resultado.porTipoSanguineo().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(10_000);
    }
}

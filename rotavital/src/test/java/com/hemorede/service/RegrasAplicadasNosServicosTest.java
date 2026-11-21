package com.hemorede.service;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Sexo;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Doador;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.exception.DadosBolsaInvalidosException;
import com.hemorede.exception.DoacaoForaDoIntervaloException;
import com.hemorede.exception.DoadorInaptoException;
import com.hemorede.exception.IncompatibilidadeSanguineaException;
import com.hemorede.exception.TransicaoStatusInvalidaException;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.DoadorRepository;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.repository.RequisicaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Garante que os services aplicam as regras de domínio (com.hemorede.domain.regras). */
@DisplayName("Regras de domínio aplicadas nos services")
class RegrasAplicadasNosServicosTest {

    private static final LocalDate HOJE = LocalDate.now();

    @Nested
    @DisplayName("RequisicaoService.aprovar (RN-09)")
    class Aprovar {

        @Test
        @DisplayName("Requisição ENTREGUE não volta para APROVADA: 409 e nenhuma bolsa é tocada")
        void entregueNaoPodeSerAprovada() {
            RequisicaoRepository repo = mock(RequisicaoRepository.class);
            AlocacaoService alocacao = mock(AlocacaoService.class);
            Requisicao req = new Requisicao();
            req.setId(7L);
            req.setStatus(StatusRequisicao.ENTREGUE);
            req.setItens(List.of(new ItemRequisicao()));
            when(repo.findById(7L)).thenReturn(Optional.of(req));

            RequisicaoService service = new RequisicaoService(repo, mock(HospitalRepository.class), alocacao);

            assertThrows(TransicaoStatusInvalidaException.class, () -> service.aprovar(7L));
            assertEquals(StatusRequisicao.ENTREGUE, req.getStatus());
            verifyNoInteractions(alocacao);
            verify(repo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("AlocacaoService - FEFO com desempate (RN-02)")
    class Fefo {

        private Bolsa bolsa(long id, TipoSanguineo tipo, LocalDate validade) {
            return Bolsa.builder().id(id).tipoSanguineo(tipo).hemoComponente(HemoComponente.HEMACIAS)
                    .dataColeta(HOJE.minusDays(10)).dataValidade(validade).status(StatusBolsa.DISPONIVEL).build();
        }

        private ItemRequisicao item(TipoSanguineo tipo, int qtd) {
            ItemRequisicao item = new ItemRequisicao();
            item.setHemoComponente(HemoComponente.HEMACIAS);
            item.setTipoSanguineo(tipo);
            item.setQuantidade(qtd);
            return item;
        }

        @Test
        @DisplayName("Mesma validade: sai a bolsa do tipo idêntico e o O- universal fica no estoque")
        void empatePrefereTipoIdentico() {
            BolsaRepository repo = mock(BolsaRepository.class);
            Bolsa oNeg = bolsa(1, TipoSanguineo.O_NEG, HOJE.plusDays(5));
            Bolsa aPos = bolsa(2, TipoSanguineo.A_POS, HOJE.plusDays(5));
            when(repo.findByHemoComponenteAndStatus(HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL))
                    .thenReturn(List.of(oNeg, aPos));

            ItemRequisicao item = item(TipoSanguineo.A_POS, 1);
            new AlocacaoService(repo).alocar(item);

            assertEquals(List.of(aPos), item.getBolsasAlocadas());
            assertEquals(StatusBolsa.DISPONIVEL, oNeg.getStatus());
        }

        @Test
        @DisplayName("Validade manda: O- que vence antes sai antes do A+ idêntico")
        void validadeTemPrioridadeSobreTipo() {
            BolsaRepository repo = mock(BolsaRepository.class);
            Bolsa oNeg = bolsa(1, TipoSanguineo.O_NEG, HOJE.plusDays(2));
            Bolsa aPos = bolsa(2, TipoSanguineo.A_POS, HOJE.plusDays(5));
            when(repo.findByHemoComponenteAndStatus(HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL))
                    .thenReturn(List.of(aPos, oNeg));

            ItemRequisicao item = item(TipoSanguineo.A_POS, 1);
            new AlocacaoService(repo).alocar(item);

            assertEquals(List.of(oNeg), item.getBolsasAlocadas());
        }

        @Test
        @DisplayName("Várias bolsas: as que saem vencem antes ou junto das que ficam")
        void saidaRespeitaFefo() {
            BolsaRepository repo = mock(BolsaRepository.class);
            List<Bolsa> estoque = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                estoque.add(bolsa(i, i % 2 == 0 ? TipoSanguineo.O_POS : TipoSanguineo.O_NEG, HOJE.plusDays(10 - i)));
            }
            when(repo.findByHemoComponenteAndStatus(HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL)).thenReturn(estoque);

            ItemRequisicao item = item(TipoSanguineo.O_POS, 3);
            new AlocacaoService(repo).alocar(item);

            LocalDate maisLongaQueSaiu = item.getBolsasAlocadas().stream().map(Bolsa::getDataValidade)
                    .max(LocalDate::compareTo).orElseThrow();
            estoque.stream().filter(b -> !item.getBolsasAlocadas().contains(b))
                    .forEach(b -> assertFalse(b.getDataValidade().isBefore(maisLongaQueSaiu)));
        }
    }

    @Nested
    @DisplayName("DoacaoService - elegibilidade e validade (RN-03, RN-04)")
    class Doacao {

        private DoacaoService serviceCom(Doador doador, long doacoesNoAno) {
            DoadorRepository doadores = mock(DoadorRepository.class);
            BolsaRepository bolsas = mock(BolsaRepository.class);
            when(doadores.findById(doador.getId())).thenReturn(Optional.of(doador));
            when(bolsas.countByDoadorIdAndDataColetaAfter(any(), any())).thenReturn(doacoesNoAno);
            when(bolsas.save(any())).thenAnswer(inv -> inv.getArgument(0));
            DoacaoService s = new DoacaoService(doadores, bolsas);
            ReflectionTestUtils.setField(s, "intervaloMinimoDias", 60);
            ReflectionTestUtils.setField(s, "intervaloMinimoDiasMulher", 90);
            return s;
        }

        private Doador doador(Sexo sexo, LocalDate ultima) {
            Doador d = new Doador(1L, "Teste", "000", TipoSanguineo.O_POS, ultima, new ArrayList<>());
            d.setSexo(sexo);
            d.setDataNascimento(HOJE.minusYears(30));
            d.setPesoKg(70.0);
            return d;
        }

        @Test
        @DisplayName("Doadora com 70 dias desde a última doação: recusada (intervalo de 90 dias)")
        void intervaloFeminino() {
            DoacaoService s = serviceCom(doador(Sexo.FEMININO, HOJE.minusDays(70)), 1);
            var ex = assertThrows(DoacaoForaDoIntervaloException.class, () -> s.registrarDoacao(
                    1L, TipoSanguineo.O_POS, HemoComponente.HEMACIAS, HOJE, HOJE.plusDays(35)));
            assertTrue(ex.getMessage().contains("Faltam 20 dia(s)"));
        }

        @Test
        @DisplayName("Doador com 70 dias: aceito (intervalo de 60 dias)")
        void intervaloMasculino() {
            DoacaoService s = serviceCom(doador(Sexo.MASCULINO, HOJE.minusDays(70)), 1);
            Bolsa b = s.registrarDoacao(1L, TipoSanguineo.O_POS, HemoComponente.HEMACIAS, HOJE, HOJE.plusDays(35));
            assertEquals(StatusBolsa.DISPONIVEL, b.getStatus());
        }

        @Test
        @DisplayName("Tipo da bolsa diferente do tipo cadastrado do doador: 422")
        void tipoDiferenteDoCadastro() {
            DoacaoService s = serviceCom(doador(Sexo.MASCULINO, null), 0);
            assertThrows(IncompatibilidadeSanguineaException.class, () -> s.registrarDoacao(
                    1L, TipoSanguineo.A_POS, HemoComponente.HEMACIAS, HOJE, HOJE.plusDays(35)));
        }

        @Test
        @DisplayName("Peso abaixo de 50 kg: inapto")
        void pesoBaixo() {
            Doador d = doador(Sexo.MASCULINO, null);
            d.setPesoKg(45.0);
            DoacaoService s = serviceCom(d, 0);
            assertThrows(DoadorInaptoException.class, () -> s.registrarDoacao(
                    1L, TipoSanguineo.O_POS, HemoComponente.HEMACIAS, HOJE, HOJE.plusDays(35)));
        }

        @Test
        @DisplayName("Plaquetas com validade de 10 dias passam do prazo de 5 dias: 400")
        void validadeAcimaDoPrazo() {
            DoacaoService s = serviceCom(doador(Sexo.MASCULINO, null), 0);
            assertThrows(DadosBolsaInvalidosException.class, () -> s.registrarDoacao(
                    1L, TipoSanguineo.O_POS, HemoComponente.PLAQUETAS, HOJE, HOJE.plusDays(10)));
        }

        @Test
        @DisplayName("Limite anual atingido: a próxima data é quando a doação mais antiga sai da janela de 12 meses")
        void proximaDataPeloLimiteAnual() {
            Doador d = doador(Sexo.FEMININO, HOJE.minusDays(100));
            DoadorRepository doadores = mock(DoadorRepository.class);
            BolsaRepository bolsas = mock(BolsaRepository.class);
            when(doadores.findById(1L)).thenReturn(Optional.of(d));
            when(bolsas.countByDoadorIdAndDataColetaAfter(any(), any())).thenReturn(3L);
            when(bolsas.findByDoadorIdOrderByDataColetaDesc(1L)).thenReturn(List.of(
                    Bolsa.builder().dataColeta(HOJE.minusDays(100)).build(),
                    Bolsa.builder().dataColeta(HOJE.minusDays(200)).build(),
                    Bolsa.builder().dataColeta(HOJE.minusDays(300)).build()));
            DoacaoService s = new DoacaoService(doadores, bolsas);
            ReflectionTestUtils.setField(s, "intervaloMinimoDias", 60);
            ReflectionTestUtils.setField(s, "intervaloMinimoDiasMulher", 90);

            var r = s.elegibilidade(d, HOJE);

            assertFalse(r.apto());
            assertEquals(HOJE.minusDays(300).plusYears(1), r.proximaDataPossivel());
        }

        @Test
        @DisplayName("Hemocentro corrige o tipo: bolsas disponíveis acompanham, histórico fica como está")
        void corrigirTipoSanguineo() {
            Doador d = doador(Sexo.MASCULINO, null);
            Bolsa disponivel = Bolsa.builder().id(10L).tipoSanguineo(TipoSanguineo.O_POS).status(StatusBolsa.DISPONIVEL).build();
            Bolsa usada = Bolsa.builder().id(11L).tipoSanguineo(TipoSanguineo.O_POS).status(StatusBolsa.UTILIZADA).build();
            DoadorRepository doadores = mock(DoadorRepository.class);
            BolsaRepository bolsas = mock(BolsaRepository.class);
            when(doadores.findById(1L)).thenReturn(Optional.of(d));
            when(doadores.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(bolsas.findByDoadorIdOrderByDataColetaDesc(1L)).thenReturn(List.of(disponivel, usada));

            Doador corrigido = new DoacaoService(doadores, bolsas).corrigirTipoSanguineo(1L, TipoSanguineo.A_NEG);

            assertEquals(TipoSanguineo.A_NEG, corrigido.getTipoSanguineo());
            assertEquals(TipoSanguineo.A_NEG, disponivel.getTipoSanguineo());
            assertEquals(TipoSanguineo.O_POS, usada.getTipoSanguineo());
        }

        @Test
        @DisplayName("Correção recusada (409) com bolsa do doador reservada ou em trânsito")
        void corrigirTipoComBolsaEmUso() {
            Doador d = doador(Sexo.MASCULINO, null);
            DoadorRepository doadores = mock(DoadorRepository.class);
            BolsaRepository bolsas = mock(BolsaRepository.class);
            when(doadores.findById(1L)).thenReturn(Optional.of(d));
            when(bolsas.findByDoadorIdOrderByDataColetaDesc(1L)).thenReturn(List.of(
                    Bolsa.builder().id(12L).tipoSanguineo(TipoSanguineo.O_POS).status(StatusBolsa.EM_TRANSITO).build()));

            assertThrows(TransicaoStatusInvalidaException.class,
                    () -> new DoacaoService(doadores, bolsas).corrigirTipoSanguineo(1L, TipoSanguineo.A_NEG));
            assertEquals(TipoSanguineo.O_POS, d.getTipoSanguineo());
            verify(doadores, never()).save(any());
        }

        @Test
        @DisplayName("Elegibilidade informa a próxima data possível")
        void elegibilidadeComProximaData() {
            Doador d = doador(Sexo.FEMININO, HOJE.minusDays(30));
            var r = serviceCom(d, 1).elegibilidade(d, HOJE);
            assertFalse(r.apto());
            assertEquals(HOJE.plusDays(60), r.proximaDataPossivel());
        }
    }
}

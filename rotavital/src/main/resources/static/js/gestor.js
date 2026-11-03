/* Rota Vital — peças compartilhadas das telas do gestor (Figma 5.1 e 5.6):
   cards de risco de desabastecimento e gráfico/resumo de descarte. */
(function (global) {
  'use strict';
  const RV = global.RV;
  const { ui } = RV;

  const COMPS = [['HEMACIAS', 'Hemácias'], ['PLASMA', 'Plasma'], ['PLAQUETAS', 'Plaquetas'], ['CRIOPRECIPITADO', 'Crio']];
  const pct = (v, d = 1) => RV.num(v, d) + '%';

  /* Faixas do doc/est-u2-probabilidade.md (p em %). */
  function faixa(p) {
    if (p > 50) return ['high', 'Campanha', 'b-minimo'];
    if (p >= 10) return ['mid', 'Priorizar coleta', 'b-vence'];
    return ['', 'Rotina', 'b-ok'];
  }

  const chipsHTML = (comp) => COMPS.map(([k, l]) =>
    `<button type="button" class="chip${comp === k ? ' active' : ''}" data-c="${k}" aria-pressed="${comp === k}">${l}</button>`).join('');

  /* ---------- Risco de desabastecimento ---------- */
  function riscoErroHTML(e) {
    return e && e.status === 503
      ? ui.alertBox('warn', 'Modelo de demanda indisponível', 'O cálculo do risco depende das taxas diárias de demanda (λ) por tipo, que ainda não foram configuradas. Nenhum valor é exibido para não apresentar números fictícios.')
      : ui.alertBox('error', 'Não foi possível calcular o risco', ui.erroMsg(e));
  }
  function riscoHTML(risco) {
    const porTipo = Object.fromEntries(((risco && risco.riscos) || []).map((r) => [r.tipoSanguineo, r]));
    const tipos = RV.TIPOS.filter((t) => porTipo[t]);
    if (!tipos.length) return ui.empty('Sem dados para calcular o risco', 'Não há bolsas nem demanda registradas para o horizonte escolhido.');
    return `<div class="risk-grid">${tipos.map((t) => {
      const r = porTipo[t];
      const p = (r.probabilidadeDesabastecimento || 0) * 100;
      const [cls, label, bcls] = faixa(p);
      return `<article class="risk ${cls}" aria-label="Risco ${RV.tipoLabel(t)}: ${pct(p)}">
        <div class="risk-top"><div class="cell">${ui.blood(t, 36)}<div class="risk-pct"><b>${pct(p)}</b><span>chance de faltar</span></div></div>${ui.badge(label, bcls)}</div>
        <div class="bar"><span style="width:${Math.min(100, p).toFixed(1)}%"></span></div>
        <span class="sub">Estoque ${r.estoqueAtual} ${r.estoqueAtual === 1 ? 'bolsa' : 'bolsas'} · demanda λ ${RV.num(r.lambdaDiario, 2)}/dia</span>
      </article>`;
    }).join('')}</div>`;
  }

  /* ---------- Descarte por vencimento ---------- */
  function descarteHTML(descarte, comp) {
    const taxas = ((descarte && descarte.taxas) || []).filter((x) => x.hemoComponente === comp);
    if (!taxas.length) {
      return ui.empty('Nenhuma bolsa venceu no período', `Não há bolsas de ${RV.compLabel(comp)} com validade nos últimos ${descarte.diasAnalisados} dias, então não há taxa de descarte para mostrar.`);
    }
    const porTipo = Object.fromEntries(taxas.map((x) => [x.tipoSanguineo, x]));
    const maior = Math.max(...taxas.map((x) => x.taxaPercentual));
    const topo = Math.max(10, Math.ceil(maior / 10) * 10);
    const passo = topo / 4;
    const altura = 180; // área das barras (250 − 26 − 44)
    const grades = [0, 1, 2, 3, 4].map((i) => `<div class="grid${i ? '' : ' zero'}" style="top:${26 + altura - (altura * i / 4)}px"><em>${RV.num(passo * i, passo * i % 1 ? 1 : 0)}%</em></div>`).join('');
    const cols = RV.TIPOS.map((t) => {
      const x = porTipo[t];
      if (!x) return `<div class="col"><span class="bar-s zero"></span><span class="val" style="bottom:9px;color:var(--faint)">—</span><span class="lbl"><b>${RV.tipoLabel(t)}</b><span>sem vencimentos</span></span></div>`;
      const h = Math.round(altura * x.taxaPercentual / topo);
      return `<div class="col" title="${RV.tipoLabel(t)}: ${x.bolsasDescartadas} de ${x.bolsasVencidasNoPeriodo} descartadas">
        <span class="bar-s${h ? '' : ' zero'}" style="height:${h}px"></span>
        <span class="val${x.taxaPercentual >= 20 ? ' hi' : ''}" style="bottom:${Math.max(h, 3) + 6}px">${pct(x.taxaPercentual)}</span>
        <span class="lbl"><b>${RV.tipoLabel(t)}</b><span>${x.bolsasDescartadas} de ${x.bolsasVencidasNoPeriodo}</span></span></div>`;
    }).join('');
    return `<div class="plot" role="img" aria-label="Taxa de descarte de ${RV.compLabel(comp)} por tipo sanguíneo">${grades}<div class="cols">${cols}</div></div>`;
  }
  function resumoDescarteHTML(descarte) {
    const taxas = (descarte && descarte.taxas) || [];
    if (!taxas.length) return ui.alertBox('info', 'Sem vencimentos no período', 'Nenhuma bolsa chegou à validade nos dias analisados.');
    const venc = taxas.reduce((s, x) => s + x.bolsasVencidasNoPeriodo, 0);
    const desc = taxas.reduce((s, x) => s + x.bolsasDescartadas, 0);
    const top = taxas.slice().sort((a, b) => b.taxaPercentual - a.taxaPercentual)[0];
    return `<div class="kv">
      <div><span class="k">Bolsas que venceram</span><b class="v">${venc}</b></div>
      <div><span class="k">Bolsas descartadas</span><b class="v">${desc}</b></div>
      <div><span class="k">Taxa geral</span><b class="v t-red">${pct(venc ? desc * 100 / venc : 0)}</b></div>
      <div><span class="k">Maior taxa</span><b class="v t-red">${RV.tipoLabel(top.tipoSanguineo)} · ${RV.esc(RV.compLabel(top.hemoComponente))} · ${pct(top.taxaPercentual)}</b></div></div>`;
  }

  RV.gestor = { COMPS, pct, faixa, chipsHTML, riscoHTML, riscoErroHTML, descarteHTML, resumoDescarteHTML };
})(window);

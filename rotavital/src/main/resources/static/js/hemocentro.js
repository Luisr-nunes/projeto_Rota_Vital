/* Rota Vital — dados e peças compartilhadas da jornada do hemocentro (HU02, HU04). */
(function (global) {
  'use strict';
  const RV = global.RV;
  const { ui, esc } = RV;

  /* Todas as bolsas disponíveis (não vencidas), em ordem FEFO.
     A API consulta por tipo + hemocomponente; aqui juntamos as combinações pedidas. */
  async function bolsasDisponiveis({ tipos = RV.TIPOS, componentes = RV.COMPONENTES } = {}) {
    const pares = [];
    tipos.forEach((t) => componentes.forEach((c) => pares.push([t, c])));
    const res = await Promise.all(pares.map(([t, c]) =>
      RV.api(`/api/estoques/bolsas?tipo=${t}&componente=${c}`).then((r) => (r && r.bolsas) || [])));
    return res.flat().sort((a, b) => (a.dataValidade || '').localeCompare(b.dataValidade || '') || a.id - b.id);
  }

  /* Requisições: urgentes primeiro, depois por ordem de chegada. */
  async function requisicoes() {
    const lista = (await RV.api('/api/requisicoes')) || [];
    return lista.sort((a, b) =>
      (a.prioridade === 'URGENTE' ? 0 : 1) - (b.prioridade === 'URGENTE' ? 0 : 1) ||
      (a.dataSolicitacao || '').localeCompare(b.dataSolicitacao || ''));
  }

  const totalBolsas = (r) => (r.itens || []).reduce((s, i) => s + (i.quantidade || 0), 0);
  const tipoPrincipal = (r) => ((r.itens || [])[0] || {}).tipoSanguineo;
  const itensTexto = (r) => (r.itens || []).map((i) => `${i.quantidade}× ${RV.compLabel(i.hemoComponente)}`).join(' + ') || '—';
  const hospitalNome = (r) => (r.hospitalSolicitante && r.hospitalSolicitante.nome) || 'Hospital';

  /* Linha "Requisição / Linha" do Figma. */
  function reqRow(r, href) {
    const n = totalBolsas(r);
    const comp = (r.itens || []).map((i) => RV.compLabel(i.hemoComponente)).filter((v, i, a) => a.indexOf(v) === i).join(', ');
    return `<a class="req-row" href="${href}">
      ${ui.blood(tipoPrincipal(r), 44)}
      <span class="info"><span class="title">${esc(hospitalNome(r))}</span><span class="detail">#${r.id} · ${n} ${n === 1 ? 'bolsa' : 'bolsas'} · ${esc(comp)} · ${esc(RV.desde(r.dataSolicitacao))}</span></span>
      <span class="status">${ui.badgeReq(r.status)}${r.prioridade === 'URGENTE' ? ui.urgent() : ''}</span>
    </a>`;
  }

  /* Sugestão FEFO para um item: bolsas compatíveis (ABO/Rh), disponíveis e na
     validade, da que vence primeiro para a que vence por último. */
  async function sugestaoFEFO(item) {
    const doadores = RV.DOADORES[item.tipoSanguineo] || [item.tipoSanguineo];
    return bolsasDisponiveis({ tipos: doadores, componentes: [item.hemoComponente] });
  }

  /* Exporta linhas em CSV (separador ;, compatível com Excel em pt-BR). */
  function baixarCSV(nome, cabecalho, linhas) {
    const q = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`;
    const csv = '﻿' + [cabecalho, ...linhas].map((l) => l.map(q).join(';')).join('\r\n');
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    a.download = nome;
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
  }

  /* Aviso deixado por outra tela (ex.: perfil ainda sem jornada própria). */
  function avisoPendente() {
    const m = RV.store.get('rv.aviso', null);
    if (m) { RV.store.del('rv.aviso'); ui.toast('info', 'Jornada em construção', m, 9000); }
  }

  RV.hemo = { bolsasDisponiveis, requisicoes, totalBolsas, tipoPrincipal, itensTexto, hospitalNome, reqRow, sugestaoFEFO, baixarCSV, avisoPendente };
})(window);

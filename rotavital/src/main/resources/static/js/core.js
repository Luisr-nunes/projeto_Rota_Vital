/* Rota Vital — núcleo: chamadas à API, rótulos dos enums, sessão e formatação. */
(function (global) {
  'use strict';

  /* ---------- API ---------- */
  class ApiError extends Error {
    constructor(status, body) {
      super((body && (body.mensagem || body.message)) || ('Erro ' + status));
      this.status = status;
      this.body = body || {};
    }
  }

  async function api(path, options = {}) {
    const opts = { headers: { Accept: 'application/json' }, ...options };
    if (opts.body && typeof opts.body !== 'string') {
      opts.body = JSON.stringify(opts.body);
      opts.headers['Content-Type'] = 'application/json';
    }
    let res;
    try {
      res = await fetch(path, opts);
    } catch (e) {
      throw new ApiError(0, { mensagem: 'Não foi possível conectar ao servidor.' });
    }
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }
    if (!res.ok) throw new ApiError(res.status, data);
    return data;
  }

  /* ---------- Enums ---------- */
  const TIPOS = ['A_POS', 'A_NEG', 'B_POS', 'B_NEG', 'AB_POS', 'AB_NEG', 'O_POS', 'O_NEG'];
  const COMPONENTES = ['HEMACIAS', 'PLASMA', 'PLAQUETAS', 'CRIOPRECIPITADO'];

  const tipoLabel = (t) => t ? t.replace('_POS', '+').replace('_NEG', '−') : '—';

  const COMPONENTE = {
    HEMACIAS: { label: 'Hemácias', validade: 42, faixa: '2–6 °C', transporte: 'Refrigerado 2–6 °C', frase: 'refrigerado 2–6 °C' },
    PLASMA: { label: 'Plasma', validade: 365, faixa: '−20 °C', transporte: 'Congelado −20 °C', frase: 'congelado a −20 °C' },
    PLAQUETAS: { label: 'Plaquetas', validade: 5, faixa: '20–24 °C', transporte: 'Temperatura controlada 20–24 °C', frase: 'em temperatura controlada 20–24 °C' },
    CRIOPRECIPITADO: { label: 'Crioprecipitado', validade: 365, faixa: '−20 °C', transporte: 'Congelado −20 °C', frase: 'congelado a −20 °C' }
  };
  const compLabel = (c) => (COMPONENTE[c] && COMPONENTE[c].label) || c || '—';

  /* Matriz ABO/Rh — espelho de TipoSanguineo.DOADORES_COMPATIVEIS no backend. */
  const DOADORES = {
    A_POS: ['A_POS', 'A_NEG', 'O_POS', 'O_NEG'],
    A_NEG: ['A_NEG', 'O_NEG'],
    B_POS: ['B_POS', 'B_NEG', 'O_POS', 'O_NEG'],
    B_NEG: ['B_NEG', 'O_NEG'],
    AB_POS: TIPOS.slice(),
    AB_NEG: ['A_NEG', 'B_NEG', 'AB_NEG', 'O_NEG'],
    O_POS: ['O_POS', 'O_NEG'],
    O_NEG: ['O_NEG']
  };
  /* Inversa: para quem uma bolsa do tipo X pode ser transfundida. */
  const receptoresDe = (doador) => TIPOS.filter((r) => DOADORES[r].includes(doador));

  const STATUS_REQ = {
    PENDENTE: ['Pendente', 'b-pendente'],
    ALOCADA: ['Alocada', 'b-alocada'],
    APROVADA: ['Aprovada', 'b-aprovada'],
    EM_ROTA: ['Em rota', 'b-rota'],
    ENTREGUE: ['Entregue', 'b-entregue'],
    CANCELADA: ['Cancelada', 'b-cancelada']
  };
  const STATUS_BOLSA = {
    DISPONIVEL: ['Disponível', 'b-disponivel'],
    RESERVADA: ['Reservada', 'b-reservada'],
    EM_TRANSITO: ['Em trânsito', 'b-transito'],
    UTILIZADA: ['Utilizada', 'b-utilizada'],
    DESCARTADA: ['Descartada', 'b-descartada']
  };

  /* ---------- Datas ---------- */
  const DAY = 86400000;
  const parseDate = (s) => {
    if (!s) return null;
    if (/^\d{4}-\d{2}-\d{2}$/.test(s)) { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d); }
    return new Date(s);
  };
  const today = () => { const d = new Date(); d.setHours(0, 0, 0, 0); return d; };
  const pad = (n, l = 2) => String(n).padStart(l, '0');
  const fmtDate = (s) => { const d = parseDate(s); return d ? `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}` : '—'; };
  const fmtTime = (s) => { const d = parseDate(s); return d ? `${pad(d.getHours())}:${pad(d.getMinutes())}` : ''; };
  const diasAte = (s) => { const d = parseDate(s); return d ? Math.round((d - today()) / DAY) : null; };
  const desde = (s) => {
    const d = parseDate(s); if (!d) return '';
    const min = Math.max(0, Math.round((Date.now() - d.getTime()) / 60000));
    if (min < 1) return 'agora';
    if (min < 60) return `há ${min} min`;
    const h = Math.round(min / 60);
    if (h < 24) return `há ${h} h`;
    const dias = Math.round(h / 24);
    return dias === 1 ? 'há 1 dia' : `há ${dias} dias`;
  };
  /* Texto e tom da validade (mesma régua do Figma). */
  function validade(s) {
    const n = diasAte(s);
    if (n === null) return { txt: '—', cls: 't-muted', n };
    if (n < 0) return { txt: 'vencida', cls: 't-red', n };
    if (n === 0) return { txt: 'vence hoje', cls: 't-red', n };
    if (n === 1) return { txt: 'vence amanhã', cls: 't-red', n };
    if (n <= 7) return { txt: `${n} dias`, cls: 't-amber', n };
    return { txt: `${n} dias`, cls: 't-green', n };
  }
  /* Código de exibição da bolsa: ano da coleta + id. */
  const codigoBolsa = (b) => { const d = parseDate(b.dataColeta); return `BR-${d ? d.getFullYear() : '0000'}-${pad(b.id, 4)}`; };
  const num = (n, dec = 0) => (n === null || n === undefined || Number.isNaN(n)) ? '—' : Number(n).toLocaleString('pt-BR', { minimumFractionDigits: dec, maximumFractionDigits: dec });

  /* ---------- Sessão (somente front: o backend ainda não tem autenticação) ---------- */
  const PERFIS = {
    ATENDENTE: { label: 'Atendente do hemocentro', home: 'hemocentro/painel.html', unidade: 'Hemocentro Central' },
    HEMOCENTRO: { label: 'Profissional do hemocentro', home: 'hemocentro/painel.html', unidade: 'Hemocentro Central' },
    HOSPITAL: { label: 'Profissional do hospital', home: null, unidade: 'Hospital' },
    LOGISTICA: { label: 'Operador de logística', home: null, unidade: 'Logística · Hemocentro Central' },
    GESTOR: { label: 'Gestor da rede', home: null, unidade: 'Rede de sangue (simulada)' }
  };
  const KEY_SESSAO = 'rv.sessao';
  const KEY_CONTAS = 'rv.contas';
  const store = {
    get(k, def) { try { const v = localStorage.getItem(k); return v ? JSON.parse(v) : def; } catch (e) { return def; } },
    set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch (e) { /* armazenamento indisponível */ } },
    del(k) { try { localStorage.removeItem(k); } catch (e) { /* idem */ } }
  };
  const sessao = {
    atual: () => store.get(KEY_SESSAO, null),
    entrar(u, lembrar) { store.set(KEY_SESSAO, { ...u, lembrar: !!lembrar, desde: Date.now() }); },
    sair() { store.del(KEY_SESSAO); },
    contas: () => store.get(KEY_CONTAS, {}),
    salvarConta(c) { const all = store.get(KEY_CONTAS, {}); all[c.email.toLowerCase()] = c; store.set(KEY_CONTAS, all); }
  };
  const iniciais = (nome) => (nome || '?').trim().split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
  const saudacao = () => { const h = new Date().getHours(); return h < 12 ? 'Bom dia' : h < 18 ? 'Boa tarde' : 'Boa noite'; };

  /* Hash da senha guardada localmente (nunca o texto puro). */
  async function hashSenha(email, senha) {
    const data = new TextEncoder().encode(`rota-vital:${email}:${senha}`);
    if (global.crypto && crypto.subtle) {
      const buf = await crypto.subtle.digest('SHA-256', data);
      return Array.from(new Uint8Array(buf)).map((b) => b.toString(16).padStart(2, '0')).join('');
    }
    let h = 0; for (const b of data) h = (h * 31 + b) | 0; return 'h' + h;
  }

  const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  global.RV = Object.assign(global.RV || {}, {
    api, ApiError, TIPOS, COMPONENTES, COMPONENTE, DOADORES, STATUS_REQ, STATUS_BOLSA, PERFIS,
    tipoLabel, compLabel, receptoresDe, parseDate, today, fmtDate, fmtTime, diasAte, desde, validade,
    codigoBolsa, num, pad, sessao, iniciais, saudacao, esc, store, hashSenha
  });
})(window);

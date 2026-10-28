/* Rota Vital — componentes de interface (ícones, shell, badges, toasts, modais). */
(function (global) {
  'use strict';
  const RV = global.RV;
  const esc = RV.esc;

  /* Base do site (funciona tanto em / quanto em /hemocentro/). */
  const BASE = (document.currentScript && document.currentScript.src.replace(/js\/ui\.js(\?.*)?$/, '')) || '/';
  const url = (p) => BASE + p;

  /* ---------- Ícones (traço 24×24, mesmos desenhos do Figma) ---------- */
  const P = {
    mail: '<rect x="3" y="5" width="18" height="14" rx="3"/><path d="m4 7 8 6 8-6"/>',
    lock: '<rect x="4.5" y="10.5" width="15" height="10" rx="2.5"/><path d="M8 10.5V7.5a4 4 0 0 1 8 0v3"/>',
    eye: '<path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z"/><circle cx="12" cy="12" r="2.8"/>',
    eyeOff: '<path d="M3 3l18 18M10.6 6a9.6 9.6 0 0 1 1.4-.5c6 0 9.5 6.5 9.5 6.5a16 16 0 0 1-2.6 3.4M6.4 6.9C3.9 8.6 2.5 12 2.5 12S6 18.5 12 18.5c1.6 0 3-.4 4.2-1.1M9.9 9.9a2.8 2.8 0 0 0 4.2 4.2"/>',
    check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
    arrowR: '<path d="M5 12h14M13 6l6 6-6 6"/>',
    arrowL: '<path d="M19 12H5M11 6l-6 6 6 6"/>',
    chart: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
    home: '<path d="M3 11 12 3.5 21 11"/><path d="M5.5 9.5V20h13V9.5"/>',
    drop: '<path d="M12 3C15 7 18 10.5 18 14a6 6 0 0 1-12 0c0-3.5 3-7 6-11Z"/>',
    inbox: '<path d="M3 13h5l1.5 2.5h5L16 13h5"/><path d="M5.5 5h13L21 13v6H3v-6Z"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4.5 20.5c1.2-3.6 4-5.5 7.5-5.5s6.3 1.9 7.5 5.5"/>',
    logout: '<path d="M15 4h3.5A1.5 1.5 0 0 1 20 5.5v13a1.5 1.5 0 0 1-1.5 1.5H15M10 16l-4-4 4-4M6 12h10"/>',
    search: '<circle cx="11" cy="11" r="6.5"/><path d="m20 20-4-4"/>',
    bell: '<path d="M6 10a6 6 0 0 1 12 0c0 6 2.5 7.5 2.5 7.5h-17S6 16 6 10Z"/><path d="M10 20.5a2.2 2.2 0 0 0 4 0"/>',
    bag: '<path d="M8 3h8v3l2 2v11a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8l2-2Z"/><path d="M9.5 13h5M12 10.5v5"/>',
    clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
    alert: '<path d="M12 3.5 22 20H2L12 3.5Z"/><path d="M12 10v4.5M12 17.5v.01"/>',
    info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5.5M12 7.5v.01"/>',
    x: '<path d="M6 6l12 12M18 6 6 18"/>',
    chevD: '<path d="m6 9 6 6 6-6"/>',
    chevR: '<path d="m9 6 6 6-6 6"/>',
    cal: '<rect x="3.5" y="5" width="17" height="15" rx="3"/><path d="M3.5 10h17M8 3v4M16 3v4"/>',
    download: '<path d="M12 4v11M7 10.5l5 5 5-5M4.5 20h15"/>',
    refresh: '<path d="M20 11a8 8 0 0 0-14.5-4.5L4 8M4 4v4h4M4 13a8 8 0 0 0 14.5 4.5L20 16M20 20v-4h-4"/>',
    snow: '<path d="M12 3v18M4.2 7.5l15.6 9M4.2 16.5l15.6-9"/>',
    send: '<path d="M21 3 10 14M21 3l-7 18-4-7-7-4 18-7Z"/>',
    truck: '<path d="M3 6h11v10H3zM14 9h4l3 3.5V16h-7"/><circle cx="7" cy="17.5" r="1.8"/><circle cx="17.5" cy="17.5" r="1.8"/>',
    route: '<circle cx="6" cy="18" r="2.5"/><circle cx="18" cy="6" r="2.5"/><path d="M8.5 18H15a3.5 3.5 0 0 0 0-7H9a3.5 3.5 0 0 1 0-7h6.5"/>',
    thermo: '<path d="M14 14.8V4.5a2 2 0 0 0-4 0v10.3a4 4 0 1 0 4 0Z"/><path d="M12 11v6"/>',
    file: '<path d="M14 3H6.5A1.5 1.5 0 0 0 5 4.5v15A1.5 1.5 0 0 0 6.5 21h11a1.5 1.5 0 0 0 1.5-1.5V8l-5-5Z"/><path d="M14 3v5h5M8.5 13h7M8.5 17h5"/>',
    risk: '<path d="M3 17l6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
    net: '<circle cx="12" cy="5" r="2.2"/><circle cx="5" cy="19" r="2.2"/><circle cx="19" cy="19" r="2.2"/><path d="M12 7.2v4.3M12 11.5 6.4 17.3M12 11.5l5.6 5.8"/>',
    zap: '<path d="M13 3 4 14h7l-1 7 9-11h-7l1-7Z"/>',
    globe: '<circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17M12 3.5c2.5 2.6 3.5 5.4 3.5 8.5s-1 5.9-3.5 8.5c-2.5-2.6-3.5-5.4-3.5-8.5s1-5.9 3.5-8.5Z"/>',
    server: '<rect x="3.5" y="4" width="17" height="7" rx="2"/><rect x="3.5" y="13" width="17" height="7" rx="2"/><path d="M7 7.5h.01M7 16.5h.01"/>',
    db: '<ellipse cx="12" cy="6" rx="7.5" ry="2.8"/><path d="M4.5 6v12c0 1.5 3.4 2.8 7.5 2.8s7.5-1.3 7.5-2.8V6M4.5 12c0 1.5 3.4 2.8 7.5 2.8s7.5-1.3 7.5-2.8"/>',
    building: '<rect x="4" y="3" width="16" height="18" rx="2"/><path d="M9 21v-4h6v4M8 7h2M14 7h2M8 11h2M14 11h2"/>'
  };
  function icon(name, size = 20, color = 'currentColor', sw = 1.8) {
    return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${P[name] || ''}</svg>`;
  }
  let dropSeq = 0;
  function drop(size = 26, light = true) {
    const id = 'rvdrop' + (++dropSeq);
    const [c1, c2] = light ? ['#FF8A8A', '#E53935'] : ['#FF7A7A', '#C62828'];
    return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" aria-hidden="true"><defs><linearGradient id="${id}" x1="12" y1="2" x2="12" y2="22" gradientUnits="userSpaceOnUse"><stop stop-color="${c1}"/><stop offset="1" stop-color="${c2}"/></linearGradient></defs><path d="M12 2.5C15.5 7.1 19 11.1 19 14.7a7 7 0 0 1-14 0C5 11.1 8.5 7.1 12 2.5Z" fill="url(#${id})"/><path d="M8.8 15.2a3.2 3.2 0 0 0 3.2 3.2" stroke="#FFFFFF" stroke-opacity="0.85" stroke-width="1.6" stroke-linecap="round"/></svg>`;
  }

  /* ---------- Peças pequenas ---------- */
  const badgeReq = (s) => { const [l, c] = RV.STATUS_REQ[s] || [s, 'b-normal']; return `<span class="badge ${c}">${esc(l)}</span>`; };
  const badgeBolsa = (s) => { const [l, c] = RV.STATUS_BOLSA[s] || [s, 'b-normal']; return `<span class="badge ${c}">${esc(l)}</span>`; };
  const badge = (label, cls) => `<span class="badge ${cls}">${esc(label)}</span>`;
  const urgent = () => '<span class="urgent">URGENTE</span>';
  const prioridade = (p) => p === 'URGENTE' ? urgent() : badge('Normal', 'b-normal');
  const blood = (tipo, size = 40) => `<span class="blood s${size}" title="${esc(RV.tipoLabel(tipo))}"><span>${esc(RV.tipoLabel(tipo))}</span></span>`;

  const TONE = { error: ['#E53935', 'x'], warn: ['#E0A100', 'alert'], info: ['#00A3C4', 'info'], success: ['#34C759', 'check'] };
  function alertBox(kind, title, msg, actionHtml = '') {
    const [c, ic] = TONE[kind];
    return `<div class="alert a-${kind}" role="${kind === 'error' ? 'alert' : 'status'}">${icon(ic, 20, c, 2.2)}<div class="txt"><b>${esc(title)}</b><p>${esc(msg)}</p></div>${actionHtml}</div>`;
  }
  function checkItem(ok, title, detail) {
    return `<div class="check-item ${ok ? 'ok' : 'err'}"><span class="dot-ic">${icon(ok ? 'check' : 'x', 14, ok ? '#34C759' : '#E53935', 2.6)}</span><div><b>${esc(title)}</b><span class="sub">${esc(detail)}</span></div></div>`;
  }
  function empty(title, msg, actionHtml = '') {
    return `<div class="empty"><span class="ic-circle">${icon('search', 32, '#00A3C4', 2.4)}</span><h3>${esc(title)}</h3><p>${esc(msg)}</p>${actionHtml}</div>`;
  }
  function kpi(label, value, ic, color, bg, note = '', noteCls = '') {
    const isEmpty = value === null || value === undefined || value === '—';
    return `<div class="kpi"><div class="kpi-top"><span class="chip-ic" style="background:${bg}">${icon(ic, 20, color, 2)}</span>${esc(label)}</div>
      <div class="kpi-val"><b class="${isEmpty ? 'empty' : ''}">${isEmpty ? '—' : esc(value)}</b>${note ? `<span class="${noteCls}">${esc(note)}</span>` : ''}</div></div>`;
  }
  const skeletonRows = (n, h = 44) => Array.from({ length: n }, () => `<div class="skeleton" style="height:${h}px"></div>`).join('');

  /* ---------- Toast ---------- */
  function toast(kind, title, msg, ms = 6000) {
    let host = document.querySelector('.toasts');
    if (!host) { host = document.createElement('div'); host.className = 'toasts'; host.setAttribute('aria-live', 'polite'); document.body.appendChild(host); }
    const [c, ic] = TONE[kind];
    const el = document.createElement('div');
    el.className = `toast t-${kind}`;
    el.innerHTML = `<span class="ic">${icon(ic, 18, c, 2.4)}</span><div class="txt"><b>${esc(title)}</b><p>${esc(msg)}</p></div><button type="button" aria-label="Fechar">${icon('x', 16, '#8A9794')}</button>`;
    el.querySelector('button').onclick = () => el.remove();
    host.appendChild(el);
    if (ms) setTimeout(() => el.remove(), ms);
    return el;
  }

  /* ---------- Modal / painel lateral ---------- */
  function overlay(html, { side = false, cls = '' } = {}) {
    const s = document.createElement('div');
    s.className = 'scrim' + (side ? ' side' : '');
    s.innerHTML = side ? `<aside class="drawer ${cls}" role="dialog" aria-modal="true">${html}</aside>` : `<div class="modal ${cls}" role="dialog" aria-modal="true">${html}</div>`;
    const close = () => { s.remove(); document.removeEventListener('keydown', onKey); };
    const onKey = (e) => { if (e.key === 'Escape') close(); };
    s.addEventListener('click', (e) => { if (e.target === s || e.target.closest('[data-close]')) close(); });
    document.addEventListener('keydown', onKey);
    document.body.appendChild(s);
    const first = s.querySelector('button, a, input'); if (first) first.focus({ preventScroll: true });
    return { el: s, close };
  }

  /* ---------- Shell autenticado (topbar + sidebar de vidro por perfil) ---------- */
  const MENUS = {
    hemocentro: {
      busca: 'Buscar bolsa, doador ou requisição…',
      itens: [['Painel', 'home', 'hemocentro/painel.html'], ['Estoque', 'chart', 'hemocentro/estoque.html'], ['Doações', 'drop', null], ['Requisições', 'inbox', 'hemocentro/requisicoes.html']]
    },
    gestor: {
      busca: 'Buscar hospital, relatório…',
      itens: [['Painel', 'chart', null], ['Indicadores', 'risk', 'gestor/indicadores.html'], ['Rede', 'net', 'gestor/rede.html'], ['Relatórios', 'file', null], ['Hospitais', 'building', null]]
    }
  };
  function requireSession() {
    const s = RV.sessao.atual();
    if (!s) { location.replace(url('login.html')); return null; }
    return s;
  }
  function shell(menu, active) {
    const s = requireSession();
    if (!s) return null;
    const perfil = RV.PERFIS[s.perfil] || RV.PERFIS.HEMOCENTRO;
    const M = MENUS[menu];
    const item = (n, ic, href) => {
      const a = n === active;
      const tag = href ? `a href="${url(href)}"` : `button type="button" data-soon="${esc(n)}"`;
      return `<${tag} class="nav-item${a ? ' active' : ''}"${a ? ' aria-current="page"' : ''}>${icon(ic, 24, a ? '#E53935' : '#2E403C', a ? 2 : 1.8)}<span>${esc(n)}</span></${href ? 'a' : 'button'}>`;
    };
    const top = document.createElement('header');
    top.className = 'topbar';
    top.innerHTML = `
      <a class="brand" href="${url(perfil.home || 'login.html')}">${drop(26)}<span class="brand-name">Rota Vital</span><span class="sep"></span><span class="unit">${esc(s.unidade || perfil.unidade)}</span></a>
      <div class="topbar-actions">
        <form class="topbar-search" role="search" data-busca>${icon('search', 18, '#fff')}<input type="search" placeholder="${esc(M.busca)}" aria-label="Buscar"></form>
        <button type="button" class="icon-square" aria-label="Notificações" data-soon="Notificações">${icon('bell', 20, '#fff')}<span class="dot"></span></button>
        <div class="user"><span class="avatar">${esc(RV.iniciais(s.nome))}</span><div><div class="user-name">${esc(s.nome)}</div><div class="user-role">${esc(perfil.label)}</div></div></div>
      </div>`;
    const side = document.createElement('nav');
    side.className = 'sidebar';
    side.setAttribute('aria-label', 'Navegação principal');
    side.innerHTML = M.itens.map(([n, ic, h]) => item(n, ic, h)).join('') + '<span class="grow"></span>' +
      item('Perfil', 'user', null) + `<button type="button" class="nav-item" data-sair>${icon('logout', 24, '#2E403C')}<span>Sair</span></button>`;
    document.body.prepend(side);
    document.body.prepend(top);
    side.querySelector('[data-sair]').onclick = () => { RV.sessao.sair(); location.href = url('login.html'); };
    [...top.querySelectorAll('[data-soon]'), ...side.querySelectorAll('[data-soon]')].forEach((b) => b.addEventListener('click', () => toast('info', `${b.dataset.soon}: em breve`, 'Esta tela faz parte de outra jornada e ainda vai ser implementada.')));
    top.querySelector('[data-busca]').addEventListener('submit', (e) => {
      e.preventDefault();
      const q = e.target.querySelector('input').value.trim();
      if (menu !== 'hemocentro') { toast('info', 'Busca: em breve', 'A busca desta área ainda vai ser implementada.'); return; }
      location.href = url('hemocentro/estoque.html') + (q ? '?q=' + encodeURIComponent(q) : '');
    });
    return s;
  }

  /* Mensagem de erro amigável a partir da ApiError. */
  const erroMsg = (e) => (e && e.message) || 'Erro inesperado.';

  RV.ui = { icon, drop, badge, badgeReq, badgeBolsa, urgent, prioridade, blood, alertBox, checkItem, empty, kpi, toast, overlay, shell, skeletonRows, url, erroMsg };
})(window);

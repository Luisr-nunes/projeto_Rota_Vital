/* Rota Vital — utilidades das telas de acesso (login, criar conta, recuperar senha). */
(function (global) {
  'use strict';
  const { ui } = global.RV;

  /* Preenche marca, ícones dos campos, olho da senha e checkbox. */
  global.authChrome = function () {
    document.querySelectorAll('[data-drop]').forEach((el) => { el.innerHTML = ui.drop(26) + '<span class="brand-name">Rota Vital</span>'; });
    document.querySelectorAll('[data-drop-lg]').forEach((el) => { el.innerHTML = ui.drop(el.classList.contains('lg') ? 28 : 26, false); });
    document.querySelectorAll('[data-ic]').forEach((el) => el.insertAdjacentHTML('afterbegin', ui.icon(el.dataset.ic, 18, '#5B6B68')));
    document.querySelectorAll('[data-icr]').forEach((el) => el.insertAdjacentHTML('beforeend', ui.icon(el.dataset.icr, 18, '#5B6B68')));
    document.querySelectorAll('[data-check]').forEach((el) => { el.innerHTML = ui.icon('check', 12, '#fff', 3.2); });
    document.querySelectorAll('[data-toggle]').forEach((btn) => {
      const input = btn.parentElement.querySelector('input');
      const paint = () => {
        const shown = input.type === 'text';
        btn.innerHTML = ui.icon(shown ? 'eyeOff' : 'eye', 18, '#5B6B68');
        btn.setAttribute('aria-label', shown ? 'Ocultar senha' : 'Mostrar senha');
      };
      btn.addEventListener('click', () => { input.type = input.type === 'password' ? 'text' : 'password'; paint(); });
      paint();
    });
    document.querySelectorAll('.input input, .input select').forEach((i) => i.addEventListener('input', () => markInvalid(i, false)));
  };

  /* Marca/desmarca o campo como inválido e devolve o estado. */
  global.markInvalid = function (input, invalid) {
    const box = input.closest('.input');
    if (box) box.classList.toggle('invalid', !!invalid);
    input.setAttribute('aria-invalid', invalid ? 'true' : 'false');
    return !!invalid;
  };

  /* Leva o usuário para a home do perfil. Perfis cujas jornadas ainda não
     foram implementadas caem no painel do hemocentro, com um aviso. */
  global.irParaHome = function (perfil) {
    const p = global.RV.PERFIS[perfil] || global.RV.PERFIS.HEMOCENTRO;
    if (!p.home) global.RV.store.set('rv.aviso', `A jornada "${p.label}" ainda está em construção. Você foi levado ao painel do hemocentro.`);
    location.href = ui.url(p.home || 'hemocentro/painel.html');
  };
})(window);

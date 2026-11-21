/* Rota Vital — gráficos e tabelas das telas de Estatística (gestor/estatistica.html).
   SVG com viewBox: o gráfico acompanha a largura do cartão, inclusive no celular. */
(function (global) {
  const RV = global.RV;
  const n = (v, d = 2) => RV.num(v, d);
  const COR = { teal: 'var(--teal)', red: 'var(--red)', cyan: 'var(--cyan)', muted: 'var(--muted)', grid: '#DDE3E1' };

  /* Marcas "redondas" para os eixos (1, 2, 5 × 10^k). */
  function ticks(min, max, alvo = 5) {
    if (min === max) { min -= 1; max += 1; }
    const bruto = (max - min) / alvo;
    const mag = Math.pow(10, Math.floor(Math.log10(bruto)));
    const passo = [1, 2, 5, 10].map((m) => m * mag).find((p) => p >= bruto);
    const out = [];
    for (let v = Math.floor(min / passo) * passo; v <= max + passo / 2; v += passo) out.push(+v.toFixed(10));
    return out;
  }
  const casas = (passo) => (passo >= 1 ? 0 : Math.min(3, Math.ceil(-Math.log10(passo))));

  /* Histograma com as linhas da média (vermelha) e da mediana (azul tracejada). */
  function histograma(classes, media, mediana) {
    const W = 600, H = 260, L = 44, R = 14, T = 16, B = 40;
    const fmax = Math.max(...classes.map((c) => c.frequencia), 1);
    const yt = ticks(0, fmax, 4);
    const ytop = yt[yt.length - 1];
    const x0 = classes[0].inicio, x1 = classes[classes.length - 1].fim;
    const sx = (v) => L + (x1 === x0 ? 0.5 : (v - x0) / (x1 - x0)) * (W - L - R);
    const sy = (v) => H - B - (v / ytop) * (H - B - T);
    const dec = casas((x1 - x0) / classes.length || 1);
    let s = yt.map((v) => `<line x1="${L}" x2="${W - R}" y1="${sy(v)}" y2="${sy(v)}" stroke="${COR.grid}" ${v ? 'stroke-dasharray="4 4"' : ''}/><text x="${L - 8}" y="${sy(v) + 4}" text-anchor="end" class="ax">${n(v, 0)}</text>`).join('');
    const passoLbl = Math.ceil(classes.length / 6);
    s += classes.map((c, i) => {
      const xa = sx(c.inicio), xb = x1 === x0 ? W - R : sx(c.fim);
      return `<rect x="${xa + 1}" y="${sy(c.frequencia)}" width="${Math.max(1, xb - xa - 2)}" height="${H - B - sy(c.frequencia)}" rx="3" fill="${COR.teal}" opacity=".78"><title>${n(c.inicio, dec)} a ${n(c.fim, dec)}: ${c.frequencia}</title></rect>`
        + (i % passoLbl === 0 ? `<text x="${xa}" y="${H - B + 18}" text-anchor="middle" class="ax">${n(c.inicio, dec)}</text>` : '');
    }).join('');
    s += `<text x="${sx(x1)}" y="${H - B + 18}" text-anchor="middle" class="ax">${n(x1, dec)}</text>`;
    const linha = (v, cor, dash) => (v >= x0 && v <= x1) ? `<line x1="${sx(v)}" x2="${sx(v)}" y1="${T - 4}" y2="${H - B}" stroke="${cor}" stroke-width="2.5" ${dash ? 'stroke-dasharray="6 4"' : ''}/>` : '';
    s += linha(media, COR.red, false) + linha(mediana, COR.cyan, true);
    return `<svg class="chart-svg" viewBox="0 0 ${W} ${H}" role="img" aria-label="Histograma com ${classes.length} classes">${s}</svg>`;
  }

  /* Boxplot horizontal: bigodes até as cercas de Tukey (Q1 − 1,5·IQR e Q3 + 1,5·IQR). */
  function boxplot(d) {
    const W = 600, H = 124, L = 24, R = 24, y = 46, h = 36;
    const x0 = d.minimo, x1 = d.maximo;
    const sx = (v) => L + (x1 === x0 ? 0.5 : (v - x0) / (x1 - x0)) * (W - L - R);
    const bi = Math.max(d.minimo, d.limiteInferiorOutlier), bs = Math.min(d.maximo, d.limiteSuperiorOutlier);
    const dec = casas((x1 - x0) / 10 || 1);
    const meio = y + h / 2;
    let s = `<line x1="${sx(bi)}" x2="${sx(d.q1)}" y1="${meio}" y2="${meio}" stroke="${COR.muted}" stroke-width="2"/>
      <line x1="${sx(d.q3)}" x2="${sx(bs)}" y1="${meio}" y2="${meio}" stroke="${COR.muted}" stroke-width="2"/>
      <line x1="${sx(bi)}" x2="${sx(bi)}" y1="${y + 8}" y2="${y + h - 8}" stroke="${COR.muted}" stroke-width="2"/>
      <line x1="${sx(bs)}" x2="${sx(bs)}" y1="${y + 8}" y2="${y + h - 8}" stroke="${COR.muted}" stroke-width="2"/>
      <rect x="${sx(d.q1)}" y="${y}" width="${Math.max(2, sx(d.q3) - sx(d.q1))}" height="${h}" rx="6" fill="${COR.teal}" fill-opacity=".18" stroke="${COR.teal}" stroke-width="2"/>
      <line x1="${sx(d.mediana)}" x2="${sx(d.mediana)}" y1="${y}" y2="${y + h}" stroke="${COR.cyan}" stroke-width="3"/>
      <circle cx="${sx(d.media)}" cy="${meio}" r="5" fill="${COR.red}"/>`;
    if (d.minimo < bi) s += `<circle cx="${sx(d.minimo)}" cy="${meio}" r="4" fill="none" stroke="${COR.red}" stroke-width="2"/>`;
    if (d.maximo > bs) s += `<circle cx="${sx(d.maximo)}" cy="${meio}" r="4" fill="none" stroke="${COR.red}" stroke-width="2"/>`;
    const rot = [[bi, 'mín'], [d.q1, 'Q1'], [d.mediana, 'Md'], [d.q3, 'Q3'], [bs, 'máx']];
    s += rot.map(([v, t], i) => `<text x="${sx(v)}" y="${i % 2 ? 30 : 108}" text-anchor="middle" class="ax">${t} ${n(v, dec)}</text>`).join('');
    return `<svg class="chart-svg" viewBox="0 0 ${W} ${H}" role="img" aria-label="Boxplot: mínimo ${n(bi, dec)}, Q1 ${n(d.q1, dec)}, mediana ${n(d.mediana, dec)}, Q3 ${n(d.q3, dec)}, máximo ${n(bs, dec)}">${s}</svg>`;
  }

  /* Diagrama de dispersão com a reta ŷ = a·x + b. */
  function dispersao(pontos, reta, rotX, rotY) {
    const W = 600, H = 330, L = 54, R = 14, T = 14, B = 48;
    const xs = pontos.map((p) => p[0]), ys = pontos.map((p) => p[1]);
    const xt = ticks(Math.min(...xs), Math.max(...xs)), yt = ticks(Math.min(...ys), Math.max(...ys));
    const [xa, xb, ya, yb] = [xt[0], xt[xt.length - 1], yt[0], yt[yt.length - 1]];
    const sx = (v) => L + (v - xa) / (xb - xa) * (W - L - R);
    const sy = (v) => H - B - (v - ya) / (yb - ya) * (H - B - T);
    const dx = casas(xt[1] - xt[0]), dy = casas(yt[1] - yt[0]);
    let s = yt.map((v) => `<line x1="${L}" x2="${W - R}" y1="${sy(v)}" y2="${sy(v)}" stroke="${COR.grid}" stroke-dasharray="4 4"/><text x="${L - 8}" y="${sy(v) + 4}" text-anchor="end" class="ax">${n(v, dy)}</text>`).join('');
    s += xt.map((v) => `<text x="${sx(v)}" y="${H - B + 18}" text-anchor="middle" class="ax">${n(v, dx)}</text>`).join('');
    s += pontos.map((p) => `<circle cx="${sx(p[0]).toFixed(1)}" cy="${sy(p[1]).toFixed(1)}" r="3.5" fill="${COR.teal}" fill-opacity=".45"/>`).join('');
    if (reta) {
      s += `<clipPath id="areaDisp"><rect x="${L}" y="${T}" width="${W - L - R}" height="${H - B - T}"/></clipPath>
        <line clip-path="url(#areaDisp)" x1="${sx(xa)}" y1="${sy(reta.a * xa + reta.b)}" x2="${sx(xb)}" y2="${sy(reta.a * xb + reta.b)}" stroke="${COR.red}" stroke-width="2.5"/>`;
    }
    const meioY = (T + H - B) / 2;
    s += `<text x="${(L + W - R) / 2}" y="${H - 6}" text-anchor="middle" class="ax lbl">${RV.esc(rotX)}</text>
      <text x="14" y="${meioY}" text-anchor="middle" class="ax lbl" transform="rotate(-90 14 ${meioY})">${RV.esc(rotY)}</text>`;
    return `<svg class="chart-svg" viewBox="0 0 ${W} ${H}" role="img" aria-label="Dispersão de ${RV.esc(rotY)} por ${RV.esc(rotX)}">${s}</svg>`;
  }

  /* Matriz de correlação: vermelho para r positivo, azul para negativo; a cor fica mais forte com |r|. */
  function heatmap(m, curto) {
    const cab = m.variaveis.map((v) => `<th scope="col" title="${RV.esc(v.nome)}">${RV.esc(curto(v))}</th>`).join('');
    const linhas = m.variaveis.map((v, i) => `<tr><th scope="row" title="${RV.esc(v.nome)}">${RV.esc(curto(v))}</th>${m.r[i].map((r, j) => {
      if (r === null) return '<td class="na">—</td>';
      const a = Math.min(1, Math.abs(r));
      const bg = r >= 0 ? `rgba(229,57,53,${(a * 0.85).toFixed(2)})` : `rgba(0,163,196,${(a * 0.85).toFixed(2)})`;
      return `<td><button type="button" data-par="${i},${j}" style="background:${bg};color:${a > 0.55 ? '#fff' : 'var(--ink)'}" aria-label="r entre ${RV.esc(v.nome)} e ${RV.esc(m.variaveis[j].nome)}: ${n(r, 2)}">${n(r, 2)}</button></td>`;
    }).join('')}</tr>`).join('');
    return `<div class="scroll-x"><table class="heat"><thead><tr><th></th>${cab}</tr></thead><tbody>${linhas}</tbody></table></div>`;
  }

  /* Tabela simples; no celular rola de lado dentro do cartão. */
  function tabela(cols, linhas, rodape) {
    return `<div class="scroll-x"><table class="tbl"><thead><tr>${cols.map((c) => `<th scope="col">${c}</th>`).join('')}</tr></thead>
      <tbody>${linhas.map((l) => `<tr>${l.map((c) => `<td>${c}</td>`).join('')}</tr>`).join('')}</tbody>
      ${rodape ? `<tfoot><tr>${rodape.map((c) => `<td>${c}</td>`).join('')}</tr></tfoot>` : ''}</table></div>`;
  }

  RV.estat = { n, histograma, boxplot, dispersao, heatmap, tabela };
})(window);

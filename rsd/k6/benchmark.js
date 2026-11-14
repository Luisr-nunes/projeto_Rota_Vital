// Benchmarking de rede do Rota Vital (PI2-174 piloto, PI2-199 final).
// Uso: k6 run -e BASE_URL=https://rota-vital-jnmx.onrender.com -e CENARIO=baixo rsd/k6/benchmark.js
// Rotas de leitura (GET) + envio de telemetria (POST, só memória): nada é gravado no banco de produção.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CENARIO = __ENV.CENARIO || 'baixo';
const TIMEOUT = '10s';

const CENARIOS = {
  // 10 usuários simultâneos: uso normal de um hemocentro e poucos hospitais
  baixo: { executor: 'constant-vus', vus: 10, duration: '1m' },
  // 50 usuários: vários hospitais pedindo bolsas ao mesmo tempo (plantão)
  moderado: { executor: 'constant-vus', vus: 50, duration: '2m' },
  // 100 usuários: pico de emergência; no plano gratuito do Render, rodar só com a equipe avisada
  alto: { executor: 'constant-vus', vus: 100, duration: '2m' },
};

if (!CENARIOS[CENARIO]) {
  throw new Error(`CENARIO inválido: ${CENARIO}. Use: ${Object.keys(CENARIOS).join(', ')}`);
}

// Perda: requisição que não recebeu resposta HTTP (timeout, conexão recusada ou resetada).
// É diferente de http_req_failed, que também conta respostas 4xx/5xx que chegaram.
const perda = new Rate('perda');
// Jitter: |latência atual − latência anterior| do mesmo usuário virtual (RFC 3550, sem suavização).
const jitter = new Trend('jitter_ms', true);
const latenciaTelemetria = new Trend('latencia_telemetria_ms', true);

export const options = {
  scenarios: { [CENARIO]: CENARIOS[CENARIO] },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    perda: ['rate<0.01'],
    jitter_ms: ['p(95)<300'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
};

const ROTAS = [
  '/api/hospitais',
  '/api/estoques/bolsas?tipo=O_POS&componente=HEMACIAS',
  '/api/indicadores',
  '/api/v1/indicadores/risco-desabastecimento?dias=7',
  '/api/telemetria/temperatura?veiculoId=1',
];

let anterior = null; // por VU: cada usuário virtual roda num contexto JS separado

function medir(res) {
  const semResposta = res.status === 0;
  perda.add(semResposta);
  if (semResposta) return;
  const atual = res.timings.duration;
  if (anterior !== null) jitter.add(Math.abs(atual - anterior));
  anterior = atual;
}

export default function () {
  // 1 em cada 4 iterações o VU age como veículo e envia uma leitura de temperatura
  if (Math.random() < 0.25) {
    const leitura = {
      veiculoId: 1000 + __VU,
      temperaturaC: 2 + Math.random() * 4,
      timestamp: new Date().toISOString(),
    };
    const res = http.post(`${BASE_URL}/api/v1/telemetria/leituras`, JSON.stringify(leitura), {
      headers: { 'Content-Type': 'application/json' },
      tags: { rota: '/api/v1/telemetria/leituras' },
      timeout: TIMEOUT,
    });
    check(res, { 'telemetria 202': (r) => r.status === 202 });
    if (res.status !== 0) latenciaTelemetria.add(res.timings.duration);
    medir(res);
  } else {
    const rota = ROTAS[Math.floor(Math.random() * ROTAS.length)];
    const res = http.get(`${BASE_URL}${rota}`, { tags: { rota: rota.split('?')[0] }, timeout: TIMEOUT });
    check(res, { 'status 200': (r) => r.status === 200 });
    medir(res);
  }
  sleep(1); // tempo de "pensar" do usuário entre cliques
}

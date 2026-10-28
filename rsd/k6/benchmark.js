// Benchmarking piloto do Rota Vital (PI2-174) — só rotas de leitura, para não gravar dados em produção.
// Uso: k6 run -e BASE_URL=https://rota-vital-jnmx.onrender.com -e CENARIO=baixo rsd/k6/benchmark.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CENARIO = __ENV.CENARIO || 'baixo';

const CENARIOS = {
  // 10 usuários simultâneos: uso normal de um hemocentro e poucos hospitais
  baixo: { executor: 'constant-vus', vus: 10, duration: '1m' },
};

if (!CENARIOS[CENARIO]) {
  throw new Error(`CENARIO inválido: ${CENARIO}. Use: ${Object.keys(CENARIOS).join(', ')}`);
}

export const options = {
  scenarios: { [CENARIO]: CENARIOS[CENARIO] },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
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

export default function () {
  const rota = ROTAS[Math.floor(Math.random() * ROTAS.length)];
  const res = http.get(`${BASE_URL}${rota}`, { tags: { rota: rota.split('?')[0] } });
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(1); // tempo de "pensar" do usuário entre cliques
}

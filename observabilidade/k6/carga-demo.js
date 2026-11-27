// Gerador de carga da avaliação de observabilidade — só contra o ambiente da VM (nunca o Render
// de produção). Mantém o Grafana com dado fresco durante a janela de correção e alimenta as
// métricas de negócio (hemorede.requisicoes.criadas / .alocacao) com tráfego real.
//
// Uso: sobe sozinho como o serviço "carga" do docker-compose.yml (BASE_URL=http://api:8080 e
// DEMO_SENHA vêm das variáveis de ambiente do serviço).
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DEMO_SENHA = __ENV.DEMO_SENHA;
const EMAIL_HEMOCENTRO = 'hemocentro@rotavital.dev';

export const options = {
  scenarios: {
    carga_demo: { executor: 'constant-vus', vus: 3, duration: '30m' },
  },
};

export function setup() {
  if (!DEMO_SENHA) {
    throw new Error('Defina -e DEMO_SENHA=<a mesma do .env da VM>');
  }
  const login = http.post(`${BASE_URL}/api/auth/login`,
      JSON.stringify({ email: EMAIL_HEMOCENTRO, senha: DEMO_SENHA }),
      { headers: { 'Content-Type': 'application/json' } });
  check(login, { 'login OK': (r) => r.status === 200 });
  const token = login.json('token');

  const hospitais = http.get(`${BASE_URL}/api/hospitais`,
      { headers: { Authorization: `Bearer ${token}` } });
  const hospitalId = hospitais.json('0.id');

  return { token, hospitalId };
}

export default function (data) {
  const auth = { headers: { Authorization: `Bearer ${data.token}`, 'Content-Type': 'application/json' } };

  // Caminho feliz: cria e aprova uma requisição -> conta hemorede.requisicoes.criadas e mede
  // hemorede.requisicoes.alocacao. Estoque insuficiente (422) também é um resultado válido do
  // domínio: a métrica de tempo de alocação é medida mesmo quando a reserva falha.
  const corpo = JSON.stringify({
    hospitalId: data.hospitalId,
    prioridade: 'NORMAL',
    itens: [{ hemoComponente: 'HEMACIAS', tipoSanguineo: 'O_POS', quantidade: 1 }],
  });
  const criada = http.post(`${BASE_URL}/api/requisicoes`, corpo, auth);
  check(criada, { 'requisicao criada (201)': (r) => r.status === 201 });

  if (criada.status === 201) {
    const id = criada.json('id');
    const aprovar = http.post(`${BASE_URL}/api/requisicoes/${id}/aprovar`, null, auth);
    check(aprovar, { 'aprovar respondeu': (r) => r.status === 200 || r.status === 422 });
  }

  // 404 natural (sem endpoint artificial de erro): alimenta o painel de taxa de erro.
  http.get(`${BASE_URL}/api/requisicoes/999999999`, auth);

  sleep(2);
}

# Dashboards

Esta pasta é lida pelo provider configurado em
`grafana/provisioning/dashboards/dashboards.yml` (pasta "Rota Vital" no Grafana).

Os dashboards são montados com o stack no ar (não dá pra escrever um JSON válido de dashboard
do Grafana sem uma instância rodando) — veja o passo a passo no `README.md` da raiz de
`observabilidade/`. Depois de montado, exporte com `Dashboards → Export → Save JSON to file` e
salve o arquivo aqui (ex.: `aplicacao.json`, `infraestrutura.json`).

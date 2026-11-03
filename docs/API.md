# API de identidade e privacidade

Todas as rotas JSON usam `/api/v1`. O navegador usa sessão do servidor; não há token de autenticação exposto ao JavaScript. Requisições de escrita requerem o cookie `XSRF-TOKEN` e o cabeçalho `X-XSRF-TOKEN`. Erros são genéricos e respostas da API usam `Cache-Control: no-store`.

| Método | Caminho | Finalidade | Estado |
|---|---|---|---|
| `GET` | `/api/v1/auth/csrf` | Inicializar token/cookie CSRF | Implementado, público |
| `POST` | `/api/v1/auth/register` | Criar conta com email, senha e nome de exibição | Implementado; resposta genérica `202` |
| `POST` | `/api/v1/auth/login` | Autenticar e abrir sessão | Implementado; erros não revelam se email existe |
| `POST` | `/api/v1/auth/logout` | Invalidar a sessão atual | Implementado; autenticação e CSRF necessários |
| `GET` | `/api/v1/me` | Consultar perfil da identidade autenticada | Implementado |
| `PUT` | `/api/v1/me/profile` | Atualizar o próprio nome de exibição | Implementado; DTO não aceita titular arbitrário |
| `GET` | `/api/v1/privacy/consents/health-data` | Estado e histórico de consentimento | Implementado |
| `PUT` | `/api/v1/privacy/consents/health-data` | Conceder versão vigente explicitamente | Implementado; exige `{"accepted":true}` |
| `DELETE` | `/api/v1/privacy/consents/health-data` | Revogar consentimento ativo | Implementado; preserva histórico |
| `GET` | `/api/v1/periods?from=YYYY-MM-DD&to=YYYY-MM-DD` | Listar observações diárias no intervalo civil | Implementado; exige consentimento; até 367 datas inclusivas |
| `POST` | `/api/v1/periods` | Registrar observação (`entryDate`, `bleedingType`, `flowLevel`, `cycleStart`) | Implementado; exige CSRF/consentimento; somente datas não futuras; início explícito requer `PERIOD` |
| `PUT` | `/api/v1/periods/{id}` | Corrigir observação própria, inclusive marcador de início | Implementado; titular derivado da sessão |
| `DELETE` | `/api/v1/periods/{id}` | Remover observação própria | Implementado; titular derivado da sessão |
| `GET` | `/api/v1/insights/dashboard` | Estimativa e estatísticas básicas do ciclo da conta autenticada | Implementado localmente; exige consentimento; mínimo de 4 inícios explícitos; mediana e faixa observada dos 3–6 intervalos mais recentes |
| `GET` | `/api/v1/symptom-entries?from=YYYY-MM-DD&to=YYYY-MM-DD` | Consultar sintomas no intervalo civil | Implementado; exige consentimento; várias entradas por dia |
| `POST` | `/api/v1/symptom-entries` | Registrar sintoma/data/intensidade e nota opcional | Implementado; intensidade de 1–5, nota até 2.000 caracteres |
| `PUT` | `/api/v1/symptom-entries/{id}` | Corrigir sintoma próprio | Implementado; titular derivado da sessão |
| `DELETE` | `/api/v1/symptom-entries/{id}` | Remover sintoma próprio | Implementado |
| `GET` | `/api/v1/mood-entries?from=YYYY-MM-DD&to=YYYY-MM-DD` | Consultar registros independentes de bem-estar | Implementado; exige consentimento; várias entradas por dia |
| `POST` | `/api/v1/mood-entries` | Registrar humor e escalas opcionais/reflexão | Implementado; faixas de 1–5 e reflexão até 2.000 caracteres |
| `PUT` | `/api/v1/mood-entries/{id}` | Corrigir registro próprio de bem-estar | Implementado; titular derivado da sessão |
| `DELETE` | `/api/v1/mood-entries/{id}` | Remover registro próprio de bem-estar | Implementado |
| `POST` | `/api/v1/me/export` | Gerar exportação dos dados implementados | Implementado; exige senha atual e CSRF, sem cache |
| `DELETE` | `/api/v1/me` | Excluir a conta e invalidar suas sessões | Implementado; exige senha atual e CSRF |
| `GET` | `/actuator/health` | Healthcheck, sem detalhes internos | Implementado |
| `GET` | `/actuator/info` | Metadados mínimos | Implementado |

## Regras de autenticação e privacidade

- A sessão é mantida no servidor via Spring Session JDBC; `KALI_SESSION` é HttpOnly e SameSite=Lax. `COOKIE_SECURE` deve ser `true` em HTTPS/produção.
- CSRF protege todos os métodos mutáveis, inclusive login/logout. O cliente obtém token via `/auth/csrf`.
- Limites atuais por endereço de origem: login 10/minuto, cadastro 5/15 minutos, exportação/exclusão 5/15 minutos, consentimento 10/15 minutos e mutações de registros de saúde 100/15 minutos por caminho.
- O cadastro não exige consentimento de saúde. Conceder consentimento grava finalidade, versão, texto exato apresentado e horário do servidor; revogar preenche `revoked_at`.
- Sem consentimento ativo da versão configurada, `/periods`, `/insights/dashboard` e as rotas de saúde são negadas por interceptor; após revogação, novos acessos e alterações a esses dados são bloqueados.
- Cada dia aceita um único registro por pessoa; tipo de sangramento e fluxo usam valores enumerados. Datas futuras são rejeitadas. O intervalo consultado é limitado a 367 datas civis para evitar consultas sem limite.
- Apenas dias marcados explicitamente como início (`cycleStart=true`) entram na estimativa; escape não pode ser marcado. O cálculo usa até 6 intervalos recentes e só é exibido a partir de 4 inícios (3 intervalos). O centro é a mediana; a faixa corresponde ao menor/maior intervalo observado, sem valor clínico ou garantia.
- Sintomas e registros de bem-estar permitem múltiplos itens no mesmo dia; cada consulta usa intervalo de até 367 datas civis e cada registro rejeita data futura. Intensidades e escalas usam valores de 1 a 5.
- Notas de sintomas e reflexões são opcionais, limitadas a 2.000 caracteres e tratadas como conteúdo sensível; não devem ser incluídas em logs.
- A exportação contém conta, perfil, consentimentos, períodos, sintomas e registros de bem-estar; exige confirmação de senha, CSRF e resposta sem cache.
- Exclusão confirma a senha, remove a conta e todos os registros subordinados por chave estrangeira em cascata e revoga todas as sessões da conta.

### Valores de sintomas e bem-estar

- `symptomType`: `CRAMPS`, `PAIN`, `HEADACHE`, `BLOATING`, `ACNE`, `BREAST_TENDERNESS`, `BOWEL_CHANGES`, `ENERGY_CHANGE`, `APPETITE_CHANGE`, `SLEEP_CHANGE` ou `DISCHARGE`.
- `mood`: `VERY_LOW`, `LOW`, `NEUTRAL`, `GOOD` ou `VERY_GOOD`.
- Intensidade e escalas opcionais são inteiros de 1 a 5. Estresse usa 1 como baixo e 5 como alto; energia e qualidade percebida do sono usam 1 como baixo e 5 como alto.
- Notas/reflexões podem ser omitidas ou `null`; limite de 2.000 caracteres.

## Contratos planejados

| Área | Caminho proposto | Observação |
|---|---|---|
| Conta | `/api/v1/auth/password-reset` | Pendente; requer fluxo de email/transporte seguro. |
| Ciclo | `/api/v1/cycles`, `/api/v1/cycles/estimate` | Modelagem de episódios agrupados permanece pendente; a estimativa simples usa marcadores em `period_entries`. |
| Insights | `/api/v1/insights/summary` | O dashboard básico foi entregue em `/api/v1/insights/dashboard`; estatísticas ampliadas permanecem pendentes. |

Esquemas futuros devem continuar usando DTOs, validação e autorização por titular autenticado.

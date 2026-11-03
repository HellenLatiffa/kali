# Modelo de dados

## Convenções

- Chaves primárias UUID; toda tabela de domínio pertence a `user_id`.
- Instantes técnicos são armazenados com fuso (`TIMESTAMPTZ` em PostgreSQL) em UTC; datas de calendário menstrual usam `DATE` e não devem sofrer conversão de fuso.
- Restrições e índices são definidos por migrations Flyway. Cascata de exclusão para dados subordinados será usada apenas depois de validar a política de retenção e exportação.
- Coletar o mínimo necessário. Não registrar localização, identificadores publicitários nem dados de terceiros.

## Schema inicial da fundação

| Tabela | Campos principais | Regras |
|---|---|---|
| `users` | `id`, `email`, `password_hash`, `created_at`, `updated_at` | Email armazenado em minúsculas e com unicidade; a camada de aplicação normaliza antes de gravar. Hash BCrypt somente, nunca senha original. |
| `user_profiles` | `user_id`, `display_name`, `created_at`, `updated_at` | Relação 1:1 e remoção em cascata ao excluir a conta. Não coleta data de nascimento nesta fase. |
| `privacy_consents` | `id`, `user_id`, `purpose`, `policy_version`, `notice_text`, `granted_at`, `revoked_at`, `created_at` | Histórico versionado; texto exibido é preservado em cada aceite e revogação não apaga a trilha. |

As migrations Flyway `V1` e `V2` criam as tabelas da aplicação e passam a preservar o texto exato do consentimento. Registros legados sem texto recebem um marcador de migração, sem serem interpretados como autorização vigente. Spring Session JDBC mantém a própria tabela de sessão no PostgreSQL e a limpa ao invalidar sessões.

## Dados de calendário implementados

| Tabela | Conteúdo e relações | Restrições/índices |
|---|---|---|
| `period_entries` | `id`, `user_id`, `entry_date`, `bleeding_type`, `flow_level`, `created_at`, `updated_at` | FK para `users` com `ON DELETE CASCADE`; unicidade por pessoa/data; `CHECK` para enums. |

Flyway `V4` cria `period_entries` e já foi aplicada no PostgreSQL local. A unidade de registro é um dia civil observado; não há entidade de episódio nem vinculação entre dias. Flyway `V6` adiciona `cycle_start` com padrão `false`, restrição que permite marcar somente dias `PERIOD` e índice composto por titular/marcador/data. A V6 foi validada em H2 e aplicada no PostgreSQL local 17.11. Registros anteriores recebem `false`; não houve inferência/backfill de inícios históricos. O fluxo (`LIGHT`, `MEDIUM`, `HEAVY`) e o tipo (`PERIOD`, `SPOTTING`) são controlados. O banco garante no máximo um registro por pessoa e data. Datas futuras e consultas com mais de 367 datas inclusivas são recusadas pela API.

A estimativa usa somente inícios explicitamente marcados pelo titular autenticado. Com ao menos quatro inícios, calcula os intervalos entre datas, usa até os seis intervalos mais recentes, toma a mediana como referência e projeta o menor/maior intervalo observado como faixa. Não tenta inferir menstruações a partir de dias sem registro. Não modela episódios, ovulação ou fertilidade.

## Sintomas e bem-estar implementados

| Tabela | Conteúdo e relações | Regras |
|---|---|---|
| `symptom_entries` | `id`, `user_id`, `entry_date`, `symptom_type`, `intensity`, `notes`, timestamps | FK com cascata; tipos controlados; intensidade 1–5; notas opcionais até 2.000 caracteres; permite vários registros/dia. |
| `wellbeing_entries` | `id`, `user_id`, `entry_date`, `mood`, `stress_level`, `energy_level`, `sleep_quality`, `reflection`, timestamps | FK com cascata; humor enumerado; escalas opcionais 1–5; reflexão opcional até 2.000 caracteres; permite vários registros/dia. |

Flyway `V5` cria ambas as tabelas com índices por titular/data. As escalas são autoavaliações subjetivas, não medidas clínicas. Conteúdo livre é sensível, exportável e removido ao excluir a conta; não pode ser escrito em logs.

Os tipos de sintoma são `CRAMPS`, `PAIN`, `HEADACHE`, `BLOATING`, `ACNE`, `BREAST_TENDERNESS`, `BOWEL_CHANGES`, `ENERGY_CHANGE`, `APPETITE_CHANGE`, `SLEEP_CHANGE` e `DISCHARGE`. Humor usa `VERY_LOW`, `LOW`, `NEUTRAL`, `GOOD` e `VERY_GOOD`. Para estresse, 1 representa baixo e 5 alto; energia e qualidade percebida do sono também usam 1–5, sem significado clínico.

## Entidades futuras para o MVP

| Tabela | Conteúdo e relações | Restrições/índices a definir |
|---|---|---|
| `menstrual_cycles` | Proprietária, início/fim observados e metadados mínimos | `user_id`, datas ordenadas; índice por pessoa e início. |
| `notification_preferences` | Canal, categorias e estado opt-in | Uma linha por pessoa; notificações discretas e desativadas por padrão. |

Todas as consultas de dados privados filtram a conta autenticada. Nunca confiar em `user_id` enviado pelo cliente como autorização. O perfil deriva o UUID do principal da sessão; a exclusão remove conta, perfil, consentimentos e observações de período com cascata, além de invalidar todas as sessões dessa conta.

## Consentimento

`privacy_consents` registra finalidade `HEALTH_DATA_PROCESSING`, versão, texto exato apresentado, instante UTC concedido e instante UTC revogado. Revogar preserva a linha de auditoria. Para processamento, somente consentimento não revogado na versão corrente configura estado `GRANTED`; os estados `REQUIRED`, `REVOKED` e `RECONSENT_REQUIRED` não autorizam processamento de saúde.

Exportação inclui conta, perfil, histórico de consentimento e todos os registros de período, sintomas e bem-estar. Excluir a conta remove esses registros pelas FKs em cascata. Novas entidades de saúde devem ser adicionadas à exportação e aos testes de exclusão junto às respectivas migrations.

## Auditoria e evolução

Guardar horários de criação/atualização e versões dos textos de consentimento, sem duplicar conteúdo íntimo em logs. Definir retenção, backup e recuperação para produção antes de coletar dados reais. Migrations subsequentes são aditivas e revisadas.

# Estado do projeto

Atualizado: 2026-11-03

## Fase atual

Fase 5 — concluída após validação no PostgreSQL local 17.11. Dashboard/estimativa implementados; próximos itens são Fase 6 e prontidão para lançamento.

## Implementado

- Cadastro, login e logout com Spring Security, BCrypt, sessões JDBC, rotação de sessão, cookies HttpOnly/SameSite e CSRF.
- Perfil GET/PUT derivado da identidade autenticada; nenhuma operação aceita ID de titular como autorização.
- Consentimento explícito de saúde com finalidade, versão, texto apresentado, data e revogação; versões antigas exigem novo consentimento.
- Interceptor bloqueia períodos, sintomas, bem-estar/diário e outras rotas de saúde se consentimento vigente não estiver concedido.
- Exportação e exclusão exigem senha atual e CSRF; exportação `no-store`, exclusão em cascata e invalidação de todas as sessões.
- Rate limiting de cadastro/login/consentimento/exportação/exclusão e mutações de saúde por endereço de origem.
- Interface React ligada às APIs reais para cadastro/login, perfil, consentimento, exportação e exclusão; sem tokens no `localStorage`.
- API e calendário mensal real para registros diários de menstruação/escape e fluxo, com criação, consulta, correção e remoção.
- Registros vinculados à conta, derivados do principal autenticado, um por pessoa/data e com datas civis; não aceita datas futuras nem intervalos extensos.
- Interceptor exige consentimento vigente nas rotas de calendário, sintomas e bem-estar; após revogação, leitura e escrita são bloqueadas.
- Exportação incorpora registros de período; a FK `ON DELETE CASCADE` remove os registros na exclusão da conta.
- Migration Flyway V4 define `period_entries`, enums por `CHECK`, unicidade por titular/data e cascata ao excluir a conta.
- Migration Flyway V5 define `symptom_entries` e `wellbeing_entries`, limites de valores/textos, índices por pessoa/data e cascata ao excluir a conta.
- Migration Flyway V6 adiciona `cycle_start` com padrão falso, valida que somente registros `PERIOD` possam ser marcados e indexa marcadores por titular/data. Aplicada no PostgreSQL local 17.11; registros anteriores permanecem `false`, sem inferência/backfill.
- Estimativa usa somente início menstrual marcado explicitamente, exige 4 inícios (3 intervalos), usa a mediana e a faixa min/max dos últimos 3–6 intervalos; não infere a partir de lacunas nem calcula fertilidade/ovulação.
- Endpoint `GET /api/v1/insights/dashboard` exige sessão e consentimento, deriva o titular do principal e retorna estatísticas/estimativa sem dados de outras contas.
- Dashboard React responsivo exibe data central, faixa observada, base utilizada e ressalva de incerteza/não contracepção; formulário permite marcar o primeiro dia menstrual e o calendário identifica os inícios.
- Sintomas: múltiplas entradas por data com tipos controlados, intensidade subjetiva de 1–5 e observação opcional limitada a 2.000 caracteres.
- Bem-estar: múltiplas entradas por data, humor categórico, escalas opcionais de estresse/energia/qualidade do sono e reflexão opcional limitada a 2.000 caracteres.
- APIs e formulários reais para consultar/criar/editar/remover sintomas e bem-estar; validação por intervalo civil e recusa de datas futuras.
- Exportação inclui sintomas e bem-estar; exclusão de conta em cascata remove os registros.
- Operações mutáveis dos registros de saúde incluídas no rate limit por IP existente. Texto livre não é escrito em logs nem enviado a terceiros.
- Spring Boot importa `..\.env` (e permite sobrescrita por variáveis do processo); datasource e Flyway usam credenciais separadas (`kali_app` e `kali_migrator`).
- Migration V3 cria tabelas Spring Session; inicialização automática de schema desativada para não exigir DDL da conta runtime.
- Script `scripts\provision-postgres.ps1` consulta conflitos, para se houver nomes existentes, aplica apenas as instruções SQL aprovadas e solicita senhas com entrada protegida.
- Script `scripts\configure-postgres-env.ps1` prepara `.env` somente para roles já existentes, sem SQL administrativo, usando prompts `SecureString`, validação de compatibilidade e ACL restrita ao usuário Windows atual.
- Script `scripts\validate-postgres.ps1` verifica login e permissões efetivas de ambas as roles e histórico Flyway sem modificar o banco ou imprimir credenciais.
- Script `scripts\apply-runtime-grants.ps1` inclui `symptom_entries` e `wellbeing_entries` nos grants DML explícitos; não foi necessário executá-lo, pois os grants efetivos foram aprovados.
- Script `scripts\test-postgres-runtime.ps1` testa DML sintético para sintomas e bem-estar numa transação revertida com `ROLLBACK`.
- Flyway V5 foi aplicada em `kali_db`; banco e roles não foram recriados, e nenhuma senha foi modificada.
- Flyway V6 foi aplicada em `kali_db` após autorização explícita; somente a tabela existente `period_entries` foi alterada. Nenhum banco/role foi recriado e nenhuma senha foi modificada.
- Validação de credenciais para `.env` também protege interpretação do PowerShell/Java Properties: proíbe espaços, barra invertida, `${` e prefixos de comentário `#`/`!`.
- `.env.example` documenta os dois usuários com placeholders; `.gitignore` já ignora `.env`.
- Atualização de README e documentação de API, modelo, segurança e roadmap.

## Arquivos desta etapa

- Backend: migration `V6__mark_cycle_starts.sql`, entidade/API de períodos, pacote `com.kali.insights` e `PeriodEntryIntegrationTest`.
- Frontend: `frontend/src/App.tsx` e `frontend/src/App.css`.
- Scripts PostgreSQL: `validate-postgres.ps1` e `test-postgres-runtime.ps1` atualizados e executados com sucesso após V6.
- Documentação: `README.md`, `docs/API.md`, `docs/DATA_MODEL.md`, `docs/SECURITY.md`, `docs/ROADMAP.md` e este status.

## Comandos executados nesta continuação

- `.\scripts\test.ps1` — passou nesta fase: 13 casos fictícios de senha, 16 testes Java, Flyway V1–V6/H2, lint Oxlint e build TypeScript/Vite.
- `backend\mvnw.cmd --no-transfer-progress -Dtest=PeriodEntryIntegrationTest test` — passou após as últimas alterações: 3 testes, sem falhas/erros.
- Os testes cobrem insuficiência de histórico, consentimento e revogação, isolamento por titular, marcação inválida em `SPOTTING`, mediana/faixa, limite de 6 intervalos e exportação dos marcadores.
- `.\scripts\start-backend.ps1` — inicializado com Java 21. Flyway validou 6 migrations e aplicou somente V6 a partir da versão 5; JDBC/Hikari e Hibernate conectaram/validaram o PostgreSQL 17.11.
- `.\scripts\validate-postgres.ps1` — passou: V1–V6 registradas; DML runtime permitido nas tabelas previstas, sem TRUNCATE; roles sem atributos elevados; sem alteração de grants.
- `.\scripts\test-postgres-runtime.ps1` — passou; DML sintético, incluindo `cycle_start`, revertido por `ROLLBACK`.
- `GET http://127.0.0.1:8080/actuator/health` — `UP`.
- As verificações acima não exibiram credenciais; backend e frontend estão iniciados para a pré-visualização local em `http://127.0.0.1:5173`. O dashboard requer sessão autenticada e consentimento vigente; não foram criadas contas nem dados de saúde sintéticos para a pré-visualização.

## Testes e builds

- Na fase anterior, `.\scripts\test.ps1` passou com Temurin Java 21.0.13: 13 casos fictícios de senha, 15 testes Java, Flyway V1–V5 em H2, lint Oxlint e build TypeScript/Vite.
- Na fase atual, a suíte passou novamente: 16 testes Java (incluindo estimativa e acesso entre contas), Flyway V1–V6 no H2, lint Oxlint e build Vite.
- Após a validação contra PostgreSQL, `npm run lint` e `npm run build` passaram novamente para conferir a alteração textual da tela inicial.
- Flyway aplicou V1–V6 no banco H2 `kali-test`; JPA validou o modelo durante os testes.
- `PeriodEntryIntegrationTest` cobre consentimento obrigatório/revogado, isolamento entre duas contas, edição/remoção, conflito por dia, rejeição de data futura/intervalo excessivo, exportação e exclusão em cascata. Somente contas/datas sintéticas.
- `SymptomWellbeingIntegrationTest` cobre consentimento, entradas múltiplas por dia, isolamento/IDOR, limites/validação, edição, remoção, exportação e cascata. Apenas contas/textos sintéticos.
- Testes fictícios cobrem pontuação, igualdade de credenciais, limite de comprimento, espaços, barra invertida, sintaxe placeholder, newline, ASCII e round-trip de `SecureString`.
- Antes da migration, `.\scripts\validate-postgres.ps1` confirmou em modo somente leitura o histórico V1–V3. Após autorização explícita, Flyway aplicou V4 no PostgreSQL 17.11; nenhuma role, senha ou banco foi recriado/modificado.
- `.\scripts\validate-postgres.ps1` passou após a migration: V1–V4 bem-sucedidas, `period_entries` com SELECT/INSERT/UPDATE/DELETE para `kali_app`, sem TRUNCATE; roles sem atributos elevados ou membership inesperada.
- `.\scripts\test-postgres-runtime.ps1` passou contra a tabela real `period_entries`, incluindo INSERT/UPDATE/DELETE sintéticos e `ROLLBACK`.
- Na validação desta etapa, `.\scripts\start-backend.ps1` iniciou com Java 21.0.13, conectou ao PostgreSQL 17.11 e aplicou V5; Hibernate `ddl-auto=validate` concluiu sem erro.
- `.\scripts\validate-postgres.ps1` confirmou Flyway V1–V5, grants SELECT/INSERT/UPDATE/DELETE e ausência de TRUNCATE para `kali_app` em todas as tabelas previstas, incluindo `symptom_entries` e `wellbeing_entries`.
- `.\scripts\test-postgres-runtime.ps1` aprovou operações DML em `symptom_entries` e `wellbeing_entries` e nas tabelas de aplicação/sessão, revertendo os dados sintéticos com `ROLLBACK`.
- V6 está aplicada; validação real confirmou PostgreSQL 17.11, Flyway V1–V6, permissões, smoke DML com rollback e healthcheck UP.
- `/actuator/health` respondeu `UP`; o processo foi encerrado depois da verificação.
- A inicialização da fase anterior confirmou Flyway V4; nesta continuação V5 também foi aplicada e validada no PostgreSQL real.
- Testes automatizados usam somente contas/dados sintéticos e H2.
- Usuário confirmou manualmente que `postgres`, `kali_app` e `kali_migrator` autenticam no PostgreSQL 17.11 e acessam `kali_db`; nenhuma credencial foi compartilhada.
- `.env` existe, contém as chaves esperadas, tem ACL protegida ao usuário Windows atual e permanece excluído pelo padrão `**/.env` (verificado previamente com `git check-ignore` em repositório descartável; verificação atual confirmada também no repositório Git do workspace).
- Spring Boot iniciado com Temurin Java 21.0.13; Hikari conectou a `kali_db` no PostgreSQL 17.11 usando JDBC como `kali_app`; `/actuator/health` respondeu `{"status":"UP"}`.
- Flyway validou V1–V6; V6 ficou registrada com sucesso no schema `public`.
- Grants DML foram aplicados inicialmente às cinco tabelas existentes e a default privileges de objetos futuros de `kali_migrator`; a V4 herdou os privilégios, verificados na `period_entries`. Nenhum banco/role foi recriado ou senha alterada.
- Validação posterior confirmou `kali_app` com CONNECT/USAGE e SELECT/INSERT/UPDATE/DELETE nas tabelas runtime, incluindo `period_entries`, `symptom_entries` e `wellbeing_entries`, sem CREATE no banco/schema, TRUNCATE, acesso ao histórico Flyway ou membership em `kali_migrator`.
- O verificador confirmou também default privileges SELECT/INSERT/UPDATE/DELETE para futuras tabelas criadas por `kali_migrator` no schema `public`.
- `kali_migrator` conecta, tem CREATE no schema e controle do histórico Flyway. Ambas as roles não são superuser, CREATEDB, CREATEROLE, replication ou BYPASSRLS.
- Ownership observado: `kali_migrator` é proprietária de `kali_db` (schema `public` pertence a `pg_database_owner`); como owner, pode administrar/remover esse banco dedicado. Reduzir esse privilégio exigiria transferir ownership e conceder DDL apenas no schema mediante aprovação administrativa.
- Testes Java automatizados usam H2; a validação real foi feita pela inicialização do Spring Boot, JDBC/Hikari, Flyway, health check e smoke DML no PostgreSQL.
- Na fase anterior, a primeira inicialização coincidiu com `mvn clean verify` e falhou ao carregar uma classe removida pela limpeza concorrente; execução serializada passou. Não foi falha de autenticação ou banco.
- Docker/Testcontainers não estão disponíveis; testes automatizados Java contra PostgreSQL/Testcontainers seguem pendentes.
- Direção futura solicitada e documentada: Java/PostgreSQL permanecem fonte de verdade; Python para job batch local com SQL versionado e dados sintéticos/aggregados; nuvem portável a decidir após análise de segurança, LGPD, região e custo. Nenhuma integração ou recurso Python/cloud foi implementado.
- `docker-compose.yml` ainda não foi adaptado/testado para usar as roles separadas e não deve ser usado como validação desta configuração local.

## Não implementado

Verificação/recuperação de email, MFA, agrupamento de registros em ciclos, estatísticas ampliadas, gráficos, retenção/backups e revisão legal.

## Verificações já realizadas

- `.\scripts\test.ps1` confirmou Maven Wrapper/Java 21, 16 testes Java, Flyway V1–V6 em H2, lint Oxlint e build de produção do frontend.
- PostgreSQL local: Spring Boot/JDBC, Flyway V1–V5, permissões das duas roles, healthcheck e smoke DML sintético com `ROLLBACK` aprovados após autorização para V5.
- A sintaxe PowerShell de `validate-postgres.ps1`, `test-postgres-runtime.ps1` e `apply-runtime-grants.ps1` foi validada com o parser do PowerShell.
- `Test-NetConnection 127.0.0.1 -Port 5432` e `pg_isready` confirmaram somente disponibilidade TCP do serviço PostgreSQL 17.
- As consultas da aplicação sempre usam o principal da sessão; testes tentaram incluir o UUID de outra conta e confirmaram que o perfil dela não muda.
- Revogação não apaga o histórico; bloqueia interceptor de rotas futuras de saúde e não bloqueia perfil, exportação ou exclusão.
- Falhas de credencial/login não revelam se o email está cadastrado. Limite local por instância não substitui rate limit distribuído.

## Pendências externas

- Roles e banco foram criados/verificados manualmente pelo usuário; nenhum provisionamento/recriação pelo assistente.
- Adicionar testes automatizados PostgreSQL/Testcontainers antes de implantação. Considerar reduzir ownership de `kali_migrator` se a política exigir que ela não possa remover `kali_db`.
- Definir o caso de uso ETL, limites para uso de dados reais e critérios objetivos de provedor/custo antes de iniciar essa direção futura.
- Manter `COOKIE_SECURE=false` somente em HTTP local; usar `true` sob HTTPS.
- Configurar verificação/recuperação de email e rate limit compartilhado antes de publicar.
- Obter avaliação jurídica de finalidade/base legal, texto de consentimento, menores, retenção e LGPD antes de dados reais.

## Preparação para portfólio

- README de apresentação, guia local separado, documentação de contribuição e modelo de pull request.
- Workflow GitHub Actions para testes Java/PowerShell, lint e build frontend em Windows, Java 21 e Node.js 24.
- Publicação destinada à conta HellenLatiffa; CI remoto depende da execução no GitHub.

# Kali

Plataforma de acompanhamento menstrual, bem-estar e autoconhecimento, com privacidade por padrão. A etapa atual oferece identidade, consentimento versionado, calendário diário, registro de sintomas, diário opcional de humor/bem-estar e uma estimativa pessoal incerta da próxima menstruação baseada em inícios marcados explicitamente.

## Pré-requisitos

- Windows 11 e PowerShell 5.1 ou posterior.
- JDK Eclipse Temurin 21 em `C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot`.
- Node.js 24.19.0 ou compatível e npm.
- PostgreSQL 17 para execução local; o serviço já pode existir fora do PATH.
- Docker Desktop não é necessário para o PostgreSQL local. O Docker Compose ainda não foi adaptado/testado para a separação de roles e não faz parte deste procedimento.

Maven não precisa ser instalado globalmente: `backend\mvnw.cmd` usa o Maven Wrapper e baixa a distribuição oficial para o cache local do usuário.

## Preparação

1. Verifique o ambiente, sem alterar configurações globais:

   ```powershell
   .\scripts\check-environment.ps1
   ```

2. Instale as dependências do frontend:

   ```powershell
   Push-Location .\frontend
   npm install
   Pop-Location
   ```

3. Configure PostgreSQL conforme “PostgreSQL local e credenciais” abaixo. O serviço instalado já aceita conexões TCP em `127.0.0.1:5432`; isso não confirma autenticação nem a existência do banco.
4. Execute o script de provisionamento aprovado para criar o `.env` local. O arquivo é ignorado pelo Git; não o compartilhe nem use senhas reais em comandos ou logs.

Os valores de senha em `.env.example` são placeholders. `COOKIE_SECURE=false` é exclusivamente para desenvolvimento local HTTP; em HTTPS, configure `COOKIE_SECURE=true` (padrão seguro da aplicação). Não reutilize senhas pessoais.

### PostgreSQL local e credenciais

O backend lê as variáveis do processo e, quando iniciado com o diretório de trabalho `backend`, importa também `..\.env`. Variáveis de ambiente do Windows têm precedência. São usadas duas contas:

- `kali_migrator`: proprietária do banco e usada somente pelo Flyway para migrations.
- `kali_app`: conta runtime, sem permissão de criar/alterar objetos; recebe apenas `CONNECT`, `USAGE` no schema e DML nas tabelas.

No estado local validado, `kali_migrator` é proprietária de `kali_db`; portanto, pode administrar/remover esse banco dedicado, embora não seja superuser nem possa criar outros bancos ou roles. Para restringi-la ainda mais a DDL no schema, seria necessário transferir a propriedade do banco e conceder privilégios de schema com a conta administrativa, mediante autorização explícita.

Como as roles e o banco já foram criados, configure apenas as credenciais existentes; este comando não cria nem altera roles/bancos:

```powershell
.\scripts\configure-postgres-env.ps1
```

O script solicita somente as senhas existentes de `kali_app` e `kali_migrator` em prompts protegidos. Ele não solicita a senha de `postgres`, não executa SQL e grava `.env` com ACL do Windows restrita ao usuário atual. Recusa sobrescrever `.env` existente. As credenciais devem ser diferentes, ter ao menos 24 caracteres ASCII imprimíveis, não conter espaços, barra invertida ou `${`, e não começar com `#` ou `!`, para permanecerem compatíveis com o carregamento atual.

O provisionamento administrativo original, caso seja necessário numa instalação nova, está em `scripts\provision-postgres.ps1`; não o execute para esta instância já provisionada.

Os testes não interativos de validação com valores fictícios podem ser executados com `.\scripts\test-provision-postgres.ps1` e também fazem parte de `.\scripts\test.ps1`.

Flyway cria as tabelas de identidade, privacidade, sessão, calendário, sintomas e bem-estar a partir de migrations. V1–V6 foram validadas e aplicadas no PostgreSQL local 17.11. V6 adiciona um marcador explícito do primeiro dia menstrual para estimativas. Hibernate valida o schema (`ddl-auto=validate`) e Spring Session não inicializa schema automaticamente.

## Verificação

Na raiz do projeto:

```powershell
.\scripts\test.ps1
```

Ou execute as verificações separadamente:

```powershell
$jdk21 = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:JAVA_HOME = $jdk21
$env:PATH = "$jdk21\bin;$env:PATH"

Push-Location .\backend
.\mvnw.cmd --no-transfer-progress clean verify
Pop-Location

Push-Location .\frontend
npm run lint
npm run build
Pop-Location
```

Os scripts definem `JAVA_HOME` e `PATH` temporariamente e restauram os valores ao terminar; o Maven Wrapper seleciona Temurin 21 apenas para o processo Java do projeto. Nada altera a instalação global do Java 27. A configuração `.vscode\settings.json` também seleciona o JDK 21 para este workspace.

## Executar localmente

Depois de provisionar banco, usuário e `.env`:

```powershell
.\scripts\start-backend.ps1
```

Em outro terminal:

```powershell
.\scripts\start-frontend.ps1
```

O backend publica `GET http://localhost:8080/actuator/health`; a interface fica em `http://localhost:5173` e encaminha `/api` ao backend.

O backend executa migrations Flyway ao iniciar. A V6 foi aplicada em `kali_db`; execuções futuras não devem reaplicá-la. Use `.\scripts\start-backend.ps1` e consulte `http://localhost:8080/actuator/health`. A integração real foi validada nesta máquina; os testes automatizados Java continuam usando H2.

Para inspecionar, sem alterar o banco, logins, grants efetivos e versões Flyway contra PostgreSQL:

```powershell
.\scripts\validate-postgres.ps1
```

O script lê `.env` sem imprimir valores e testa conexões independentes com `kali_app` e `kali_migrator`. A role runtime deve ter `CONNECT`, `USAGE` e DML necessário nas tabelas da aplicação/sessões, mas não `CREATE`, `TRUNCATE` ou acesso à tabela de histórico Flyway.

Se grants DML estiverem ausentes, após revisar e autorizar a mudança, `.\scripts\apply-runtime-grants.ps1` aplica permissões somente nas tabelas Kali existentes e privilégios DML padrão para futuras tabelas criadas por `kali_migrator` no schema `public`. Não recria nem altera roles/bancos.

Após validar os grants, `.\scripts\test-postgres-runtime.ps1` verifica operações DML em transação usando apenas dados sintéticos e finaliza com `ROLLBACK`.

## Identidade e privacidade disponíveis

- `POST /api/v1/auth/register`: cadastro com email normalizado e senha de pelo menos 12 caracteres; resposta genérica reduz enumeração.
- `POST /api/v1/auth/login` e `POST /api/v1/auth/logout`: sessões no servidor, cookie HttpOnly/SameSite=Lax, rotação de ID e CSRF para operações mutáveis.
- `GET /api/v1/me` e `PUT /api/v1/me/profile`: perfil da identidade autenticada; não aceitam ID de titular fornecido pelo cliente.
- `GET`, `PUT` e `DELETE /api/v1/privacy/consents/health-data`: consultar, conceder e revogar consentimento explícito e versionado; finalidade, versão, texto exato e horários são preservados.
- `GET /api/v1/periods?from=...&to=...`, `POST` e `PUT /api/v1/periods/{id}`, `DELETE /api/v1/periods/{id}`: calendário diário; datas civis, um registro por dia, fluxo/tipo validados e consentimento vigente obrigatório.
- `GET /api/v1/symptom-entries?from=...&to=...`, `POST`, `PUT /api/v1/symptom-entries/{id}` e `DELETE`: sintomas e sinais corporais com intensidade subjetiva de 1–5, várias entradas por dia e observação opcional.
- `GET /api/v1/mood-entries?from=...&to=...`, `POST`, `PUT /api/v1/mood-entries/{id}` e `DELETE`: registros independentes de humor, estresse, energia e qualidade percebida do sono; reflexão opcional.
- `GET /api/v1/insights/dashboard`: estimativa simples a partir de inícios explicitamente marcados, somente com pelo menos 4 inícios/3 intervalos; usa mediana e faixa observada dos 3–6 intervalos mais recentes. Não estima ovulação/fertilidade e não é método contraceptivo.
- `POST /api/v1/me/export` e `DELETE /api/v1/me`: exigem senha atual, CSRF e autenticação recente; exportação sem cache, exclusão em cascata e invalidação das sessões.

O consentimento deve estar na versão vigente para processar dados de saúde. Após revogação, leituras e alterações do calendário, sintomas, bem-estar e dashboard são bloqueados; identidade, perfil, exportação e exclusão continuam disponíveis. As escalas são percepções pessoais. A estimativa do ciclo é incerta, não diagnóstica e não contraceptiva.

O frontend não armazena tokens/sessões em `localStorage`; usa o cookie HttpOnly do servidor e o cookie CSRF legível necessário para enviar `X-XSRF-TOKEN`. Recuperação/verificação de email ainda não está disponível.

## Estrutura

- `backend`: monólito Spring Boot, persistência PostgreSQL, Flyway e testes.
- `frontend`: aplicação React + TypeScript + Vite.
- `docs`: requisitos, arquitetura, dados, API, segurança e roadmap.
- `scripts`: verificações e execução com configuração de Java limitada ao processo.
- `docker-compose.yml`: configuração anterior de PostgreSQL via Docker, não validada com as contas separadas desta etapa.

Consulte [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md) para o estado verificado e [docs/ROADMAP.md](docs/ROADMAP.md) para as próximas etapas.

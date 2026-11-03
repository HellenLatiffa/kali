# Segurança e privacidade

## Classificação e princípios

Registros de ciclo, sintomas, humor e diário são dados pessoais sensíveis. Aplicar minimização, finalidade explícita, consentimento granular quando aplicável, acesso mínimo e privacidade por padrão. A conformidade com LGPD exige avaliação jurídica especializada; este documento é uma base técnica, não parecer legal.

## Controles implementados nesta etapa

- BCrypt com fator 12; senhas em claro não são persistidas. Senhas têm mínimo de 12 caracteres e limite de 72 bytes UTF-8 do BCrypt.
- Cadastro e autenticação normalizam email; respostas de cadastro/login são genéricas para reduzir enumeração.
- Sessões server-side em Spring Session JDBC, timeout de 30 minutos, rotação de ID na autenticação e invalidação de todas as sessões ao excluir a conta.
- Cookie `KALI_SESSION` HttpOnly e SameSite=Lax. `COOKIE_SECURE` é seguro por padrão; `.env.example` usa `false` apenas para localhost HTTP. Em produção HTTPS, definir `true`.
- CSRF em todos os métodos mutáveis. A SPA envia `X-XSRF-TOKEN`; logout também requer token.
- Limitação em memória por IP nos endpoints de cadastro, login, consentimento, exportação, exclusão e mutações de registros de saúde. Os endereços encaminhados só devem ser confiados atrás de proxies explicitamente configurados.
- Validação Bean Validation, DTOs sem campos de titular arbitrário, sessão exigida e operações de perfil ligadas ao principal autenticado.
- Consentimento de saúde versionado, com texto exato armazenado no histórico e revogável; interceptor protege períodos, sintomas, bem-estar/diário e demais caminhos de saúde.
- Exportação autenticada por senha, POST+CSRF, `no-store`; inclui período, sintomas e bem-estar. Exclusão autenticada por senha e CSRF remove os registros em cascata e revoga sessões.
- Notas/reflexões opcionais são limitadas a 2.000 caracteres e não são registradas em logs; IDs de registro são sempre combinados ao principal autenticado nas consultas e mutações.
- Respostas de erro sem detalhes de autenticação/DB e cabeçalhos sem cache para `/api`.
- Flyway versiona as tabelas da aplicação; nenhum dado real é usado nos testes.
- Endpoint do dashboard deriva o titular da sessão, é protegido por consentimento e calcula somente a partir de marcadores de início explicitamente registrados; não recebe UUID de usuário nem expõe conteúdo livre.

## Pendências antes do MVP público

- Rate limit atual usa memória local por instância; adotar armazenamento compartilhado e proteção na borda antes de escalar.
- Não há verificação/recuperação de email, MFA, nem fluxo de desbloqueio.
- A migration V6 (marcador explícito de início do ciclo) foi aplicada e validada no PostgreSQL local 17.11. Testes automatizados Java continuam usando H2; Docker/Testcontainers não estão disponíveis neste ambiente.
- Estimativas dependem de entradas explícitas e históricas suficientes; não são diagnósticas, não informam fertilidade/ovulação e não são contracepção. Validar o texto/uso com revisão clínica e jurídica antes de produção.
- Configurar HTTPS, gestão de segredos, criptografia/backups em repouso, política de retenção e teste de restauração.
- Verificar encaminhamento de IP/CORS e proteção de origem com a topologia final de produção.
- Implementar logging operacional estruturado que nunca capture credenciais, tokens ou conteúdo íntimo; definir monitoramento sem analytics invasivo.
- Expandir testes IDOR e autorização para cada novo recurso de saúde, exportação e exclusão.
- Procedimento de resposta a incidentes e revisão OWASP Top 10.
- Avaliação legal de bases/consentimentos, menores de idade, direitos dos titulares e operadores.

## Efeito da revogação

Revogar grava `revoked_at` e impede imediatamente novo processamento nos caminhos de saúde. Login, perfil, histórico do consentimento, exportação e exclusão continuam acessíveis para exercício de direitos. Revogar não apaga registros anteriores; a exclusão explícita da conta é que remove os dados da aplicação. Não há retenção/backups configurados para lançamento ainda.

O aviso de consentimento e os controles são uma implementação técnica inicial, não parecer jurídico nem garantia de conformidade LGPD. Obter revisão especializada antes de uso público ou coleta real.

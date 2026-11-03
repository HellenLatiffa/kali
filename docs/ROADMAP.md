# Roadmap

## Fase 1 — Fundação técnica (concluída)

- [x] Inspecionar especificação e ambiente Windows.
- [x] Documentar requisitos, arquitetura, modelo, API, segurança e roadmap.
- [x] Criar backend Java 21/Spring Boot, Wrapper, Flyway e healthcheck.
- [x] Criar frontend React/TypeScript/Vite e scripts Windows.
- [x] Executar builds e testes disponíveis; registrar limitações no status.

**Aceite:** compilação/testes reproduzíveis, migration inicial aplicada nos testes, build/lint frontend aprovados, nenhuma instalação global nem segredo commitado.

## Fase 2 — Identidade e privacidade (implementada; integração real com PostgreSQL validada)

- [x] Cadastro/login/logout; sessões server-side, hash BCrypt, CSRF e limite de tentativas.
- [x] Perfil autenticado, consentimento versionado e revogação com bloqueio de processamento de saúde.
- [x] Exportação e exclusão protegidas por senha e CSRF.
- [x] Testes de isolamento entre contas, consentimento, CSRF, sessão, rate limit e exportação/exclusão.
- [x] Configurar credenciais locais e validar migrações/sessões no PostgreSQL 17 real.
- [ ] Implementar verificação e recuperação de senha quando houver serviço de email configurado.

## Fase 3 — Perfil e calendário (concluída)

- [x] Reutilizar o perfil mínimo autenticado já entregue na Fase 2.
- [x] Criar, consultar, corrigir e remover observações diárias de sangramento/fluxo.
- [x] Garantir uma observação por pessoa/data, datas civis e rejeição de datas futuras.
- [x] Proteger as operações por consentimento vigente e pelo titular da sessão.
- [x] Incluir os registros na exportação e cascata de exclusão.
- [x] Integrar calendário mensal real no frontend, sem mocks.
- [x] Cobrir isolamento entre contas, consentimento, conflitos, validação, exportação e exclusão em testes H2.
- [x] Aplicar/validar a migration V4 no PostgreSQL local após autorização explícita.

Agrupamento em ciclos, estimativas e recomendações permanecem fora desta fase.

## Fase 4 — Sintomas e bem-estar (concluída)

- [x] Registrar múltiplos sintomas por dia com catálogo controlado, intensidade subjetiva de 1–5 e nota opcional.
- [x] Registrar múltiplas entradas independentes por dia para humor, estresse, energia e qualidade do sono.
- [x] Tornar escalas complementares e reflexão opcionais; limitar textos a 2.000 caracteres.
- [x] Derivar o titular da sessão, exigir consentimento vigente e permitir consulta/edição/exclusão próprias.
- [x] Incluir os dados na exportação e exclusão em cascata; testar isolamento, validação e revogação em H2.
- [x] Integrar formulários reais no frontend, sem mocks.
- [x] Aplicar/validar a migration V5 no PostgreSQL 17 local após autorização explícita; confirmar JDBC, Flyway V1–V5, permissões e smoke DML com rollback.

Notas e reflexões são conteúdo sensível: não registrar em logs nem enviá-las a serviços externos.

## Fase 5 — Estimativas e dashboard (concluída)

- [x] Adicionar marcador explícito do primeiro dia menstrual; não inferir início a partir de lacunas de registro.
- [x] Estimar após pelo menos 4 inícios (3 intervalos), usando mediana e faixa min/max dos 3–6 intervalos recentes.
- [x] Criar endpoint privado protegido por consentimento, estatísticas básicas e dashboard responsivo com ressalva de incerteza/não contracepção.
- [x] Cobrir estimativa, insuficiência de histórico, consentimento e isolamento por titular em testes H2.
- [x] Aplicar/validar a migration V6 no PostgreSQL local após autorização explícita; executar validação JDBC/Flyway, permissões e smoke DML com rollback.

Ovulação e janela fértil não são estimadas. A estimativa continua sujeita a revisão clínica/legal antes de qualquer lançamento público.

## Fase 6 — Histórico, direitos e prontidão

- Estatísticas descritivas, exportação explícita e exclusão de dados.
- Testes de integração PostgreSQL/Testcontainers, restauração de backup, CI e auditorias.
- Revisões de acessibilidade, segurança e LGPD antes de lançamento.

Nutrição, atividade, educação editorial, notificações e assistente são itens futuros; IA e integrações externas permanecem fora do MVP inicial.

## Direção futura — Python, SQL, ETL e nuvem (planejada; não implementada)

- Manter Spring Boot/Java como API transacional e PostgreSQL/Flyway como fonte de verdade.
- Avaliar Python para jobs batch locais, inicialmente com dados sintéticos e métricas agregadas, sem criar microserviço de produto.
- Versionar transformações SQL; antes de acessar dados reais, definir uma role de leitura restrita a views agregadas e revisar consentimento, finalidade e exportação/exclusão.
- Não colocar dados de saúde identificáveis em serviços gratuitos de nuvem. Escolher provedor apenas após comparar custo sustentável, região, LGPD/contratos, criptografia, IAM, auditoria, retenção e limites de serviço.
- Manter execução portável entre ambiente local e eventual tarefa containerizada/agendada; sem dependência de provedor nesta fase.

**Aceite para iniciar:** caso de uso e dados permitidos definidos, modelo de ameaça aprovado, protótipo com dados sintéticos reproduzível localmente, custos/limites documentados e revisão de privacidade antes de qualquer dado real ou cloud.

## Ritmo de trabalho

Concluir uma fase por vez: explicar escopo, implementar, executar testes pertinentes, atualizar `PROJECT_STATUS.md` e só então avançar. Nenhuma etapa posterior é considerada entregue por estar documentada.

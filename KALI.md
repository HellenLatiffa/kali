# KALI — Instruções de desenvolvimento para o Codex

## Missão

Atue como engenheiro de software sênior, arquiteto de sistemas e especialista em segurança. Desenvolva o **Kali**, uma plataforma original para acompanhamento do ciclo menstrual, bem-estar e autoconhecimento, inspirada simbolicamente na deusa Kali. O produto pode tomar como referência categorias de funcionalidades existentes no Flo e no Clue, **sem copiar código, identidade visual ou conteúdo protegido**.

Construa uma aplicação real, testável e documentada. **Comece pelo planejamento e pela fundação técnica, não por um protótipo de telas desconectadas.**

## Público e princípios

- Público: pessoas que menstruam, com necessidades e objetivos variados.
- Linguagem acolhedora e inclusiva, sem pressupor gênero, regularidade do ciclo ou intenção de engravidar.
- Privacidade por padrão, acessibilidade (WCAG 2.2 AA) e recursos principais gratuitos no lançamento.
- Não comercializar dados íntimos nem utilizar anúncios baseados em dados de saúde.
- Conteúdo informativo, não diagnóstico ou prescrição. Previsões menstruais e férteis são estimativas incertas, **nunca um método contraceptivo confiável**.
- Adequação à LGPD e avaliação especializada antes de lançamento público, sobretudo para menores de idade.

## Escopo funcional completo (visão de produto)

### 1. Perfil e identidade
- Cadastro, login, logout, recuperação de senha e configuração do perfil.
- Preferências, objetivos, configurações de notificações e acessibilidade.
- Consentimentos específicos para tratamento de dados sensíveis, exportação e exclusão de dados.

### 2. Calendário menstrual
- Registrar início/fim do período, fluxo e sangramento fora do período.
- Visualizar histórico, duração e variabilidade dos ciclos.
- Estimar próxima menstruação e, apenas onde apropriado, ovulação/janela fértil com incerteza explícita.
- Considerar ciclos irregulares e uso de contraceptivos hormonais, sem inferências enganosas.
- Editar e corrigir registros históricos.

### 3. Sintomas e sinais corporais
- Cólicas, intensidade da dor, cefaleia, inchaço, acne, sensibilidade mamária, alterações intestinais, energia, apetite, sono, secreções e observações livres.
- Registros por dia, incluindo múltiplas entradas e edições.

### 4. Diário de autoconhecimento
- Humor, estresse, energia, sono, reflexões, anotações privadas e acompanhamento de padrões.
- Mostrar relações observadas sem afirmar causalidade não comprovada.

### 5. Alimentação
- Conteúdos e sugestões gerais sobre hidratação, alimentação equilibrada, ferro, fibras e outros nutrientes.
- Receitas acessíveis, preferências alimentares e diário opcional.
- Não prescrever dietas terapêuticas ou suplementos, nem prometer benefícios exclusivos por fase do ciclo.

### 6. Exercícios e movimento
- Registrar caminhada, corrida, musculação, alongamento, yoga e outras atividades.
- Monitorar disposição, esforço percebido e recuperação.
- Sugerir adaptações com base nos sintomas e preferências pessoais, sem estabelecer regras rígidas por fase do ciclo.

### 7. Educação em saúde
- Biblioteca sobre fisiologia menstrual, anatomia, fertilidade, contracepção, SOP, endometriose, menopausa, saúde sexual e emocional.
- Registrar fontes, autores e datas de revisão. Informar quando procurar atendimento médico.

### 8. Estatísticas e relatórios
- Gráficos de duração de ciclos, sintomas, dores, sono, humor, atividades e tendências.
- Exportação de registros, com ação explícita da usuária e proteção dos arquivos gerados.

### 9. Notificações
- Lembretes opcionais de período previsto, registros, hidratação, autocuidado e consultas.
- Sem conteúdo íntimo na tela bloqueada por padrão.

### 10. Assistente Kali (futuro)
- Assistente opcional para educação em saúde, navegação e explicação de tendências.
- Não diagnosticar, prescrever ou substituir profissional de saúde.
- Não enviar dados íntimos a provedores de IA sem consentimento explícito, granular e revogável.

## MVP obrigatório (primeira versão)

Implemente primeiro, em etapas:
1. Cadastro, autenticação e autorização.
2. Consentimento para dados sensíveis e controles de privacidade.
3. Perfil e preferências.
4. Calendário com inclusão/edição de menstruações e fluxo.
5. Registro de sintomas.
6. Diário de humor e energia.
7. Estimativa simples da próxima menstruação, com intervalo e ressalvas.
8. Dashboard responsivo.
9. Histórico e estatísticas básicas.
10. Exportação e exclusão de dados pessoais.
11. Testes automatizados e documentação.

**Fora do MVP:** IA, recomendações avançadas, assinatura premium, redes sociais e integrações complexas.

## Tecnologias preferidas

**Backend:** Java 21 LTS, Spring Boot, Spring Security, Spring Data JPA, Hibernate, PostgreSQL, Flyway, Maven, Bean Validation, OpenAPI/Swagger, JUnit 5, Mockito e Testcontainers.

**Frontend:** React, TypeScript, Vite, Tailwind CSS, React Router, TanStack Query, componentes acessíveis e biblioteca de gráficos adequada.

**Infraestrutura:** Docker, Docker Compose, GitHub Actions, variáveis de ambiente, logs estruturados e documentação Markdown.

Use um **monólito modular** inicialmente. Não introduza microserviços ou dependências desnecessárias. Justifique mudanças de stack.

## Arquitetura proposta

Organize o backend por módulos de negócio, por exemplo:

- `identity`
- `profile`
- `privacy`
- `menstrualcycle`
- `symptoms`
- `wellbeing`
- `insights`
- `shared`

Prepare a extensão futura para `nutrition`, `activity`, `education`, `notifications` e `assistant` sem precisar implementá-los agora.

Adote DTOs, validação, regras de domínio explícitas, separação de responsabilidades, tratamento global de erros e migrations versionadas. **Nunca exponha entidades JPA diretamente nas respostas da API.**

## Banco de dados (modelagem a refinar)

Considere inicialmente as entidades/tabelas:

- `users`
- `user_profiles`
- `privacy_consents`
- `menstrual_cycles`
- `period_entries`
- `symptom_entries`
- `mood_entries`
- `notification_preferences`

Planeje relacionamentos, restrições, índices, convenções de datas/fusos, auditoria mínima e exclusão segura. Cada operação autenticada deve limitar acesso aos dados pertencentes à própria usuária. Utilize somente dados sintéticos em desenvolvimento/testes.

## Segurança e dados sensíveis (obrigatório)

- Tratar registros de saúde como **dados pessoais sensíveis** sob a LGPD.
- Minimizar coleta, definir finalidades e documentar consentimentos aplicáveis.
- Hash de senha robusto e proteção contra enumeração de usuários e abuso de autenticação.
- Autorização em todos os endpoints; testar acesso indevido entre contas (IDOR/BOLA).
- HTTPS em produção, gestão segura de segredos e proteção de dados em repouso.
- Política de sessão/tokens segura, controle de CORS e proteção apropriada a CSRF conforme o mecanismo escolhido.
- Proteções para OWASP Top 10, validação de entrada, limitação de requisições e logs sem conteúdo sensível.
- Exportação, revogação de consentimento, exclusão e política de retenção definidas.
- Notificações discretas; sem ferramentas de analytics invasivas ou envio de dados sensíveis a terceiros por padrão.
- Backups protegidos e procedimento de resposta a incidentes.

## UX/UI

Crie uma identidade original com roxo, lilás, ameixa, rosa e tons neutros; estética elegante e acolhedora. Construa design system com tokens de cores, tipografia, espaçamento, estados interativos e componentes reutilizáveis. Suporte modo claro/escuro, leitores de tela, navegação por teclado e interface responsiva. Dashboard com calendário, registros rápidos, tendência de ciclo e atalhos para sintomas e diário.

## Engenharia e documentação

Mantenha:
- `README.md`: pré-requisitos, instalação, execução, testes e comandos.
- `docs/ARCHITECTURE.md`: arquitetura e módulos.
- `docs/REQUIREMENTS.md`: requisitos e escopo.
- `docs/DATA_MODEL.md`: entidades, relacionamentos e justificativas.
- `docs/SECURITY.md`: riscos, medidas e pendências legais/técnicas.
- `docs/API.md`: endpoints e contratos iniciais.
- `docs/ROADMAP.md`: fases, backlog e critérios de aceite.
- `PROJECT_STATUS.md`: implementado, testado, pendente e próximos passos.
- `.env.example`: apenas nomes e valores seguros de exemplo, **nunca credenciais reais**.

Crie testes unitários e de integração, verificações de autorização, validações de regra de negócio e pipeline básico de CI quando apropriado. Não alegue que algo funciona sem executar testes ou declarar claramente as limitações.

## Modo de trabalho do Codex

1. Inspecione o repositório existente antes de criar ou alterar arquivos.
2. **Primeiro**, produza a proposta de arquitetura, modelo de dados, endpoints REST iniciais, roadmap e riscos de segurança em Markdown.
3. **Depois**, implemente **somente a fundação técnica**: estrutura do backend/frontend, configuração do PostgreSQL, Flyway, Docker Compose, healthcheck, testes mínimos e instruções de execução local.
4. Não implemente IA, telas extensas ou recursos fora do MVP nesta primeira execução.
5. Execute build e testes disponíveis; reporte os resultados e qualquer impedimento.
6. Atualize `PROJECT_STATUS.md` após cada fase.
7. Em cada nova etapa, apresente arquivos alterados, decisões, comandos de verificação, status dos testes e próximos itens.
8. Priorize código legível, segurança e arquitetura coerente em vez de complexidade desnecessária.

## Primeira entrega esperada

- Documentação inicial de produto, arquitetura, API, segurança e roadmap.
- Repositório estruturado com backend Java/Spring Boot e frontend React/TypeScript.
- Banco PostgreSQL configurado para desenvolvimento com Docker Compose e migrations iniciais.
- Aplicações iniciando localmente e healthcheck simples.
- Testes iniciais executáveis.
- README com comandos específicos para Windows/PowerShell quando necessário.
- Relatório final do que foi efetivamente implementado e do que ficou para a próxima fase.

**Comece agora pela inspeção do projeto e pela Etapa 1. Não trate ideias futuras como funcionalidades já concluídas.**

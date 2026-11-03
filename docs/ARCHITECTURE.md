# Arquitetura

## Decisão

Monólito modular inicialmente, com um único backend Spring Boot e frontend SPA separado. Persistência relacional em PostgreSQL, migrations Flyway e contratos HTTP JSON. Não serão introduzidos microserviços nem serviços externos de saúde/analytics no MVP.

## Componentes

- **Backend:** Java 21, Spring Boot 3.5, Spring MVC, Spring Data JPA/Hibernate, Bean Validation, Actuator e Flyway; build Maven via Wrapper.
- **Banco:** PostgreSQL 17. Datas e instantes persistidos com semântica explícita; o cliente usa datas civis sem convertê-las em instante.
- **Frontend:** React, TypeScript e Vite; componentes acessíveis e design tokens originais antes de implementar telas funcionais.
- **Ambiente local:** scripts PowerShell, variáveis por processo e Docker Compose opcional. A instalação Windows de PostgreSQL existente também pode ser usada.

## Organização proposta do backend

Pacote raiz `com.kali`, com módulos isolados por domínio:

- `identity`: usuários e autenticação.
- `profile`: dados mínimos de perfil e preferências.
- `privacy`: consentimentos, exportação, revogação e exclusão.
- `menstrualcycle`: ciclos, períodos e regras de estimativa.
- `symptoms`: entradas de sintomas.
- `wellbeing`: humor, energia e diário.
- `insights`: estatísticas e estimativas, sempre com incerteza.
- `shared`: erros HTTP, configuração e utilitários realmente transversais.

Cada módulo deverá separar domínio, aplicação, persistência e API quando a complexidade justificar. DTOs serão contratos HTTP; entidades JPA nunca serão serializadas diretamente. Autorização e escopo por pessoa usuária devem ser aplicados na camada de serviço/repositório e verificados por testes.

Pacotes futuros reservados conceitualmente: `nutrition`, `activity`, `education`, `notifications` e `assistant`; não fazem parte da fundação funcional.

## Organização do frontend

`src` será organizado por funcionalidades e componentes compartilhados conforme o MVP crescer. Rotas, cache remoto e biblioteca de gráficos só serão adicionados junto de casos de uso reais. Design tokens deverão suportar tema claro/escuro, foco visível, contraste e responsividade.

## Persistência e operação

Flyway é a fonte versionada do schema. Hibernate não cria nem atualiza tabelas (`ddl-auto=validate`). Segredos são fornecidos por variáveis de ambiente locais fora do Git. Actuator expõe somente health/info sem detalhes internos. Logs não podem conter senhas, tokens, conteúdo de diário ou registros de saúde.

O Docker Compose existente é legado e não foi validado com as roles separadas. O fluxo atual usa PostgreSQL local; consulte LOCAL_DEVELOPMENT.md.

## Python, SQL, ETL e nuvem (direção futura)

- Java/Spring continua sendo a API transacional e fonte de verdade de identidade e registros; PostgreSQL continua sendo o armazenamento relacional.
- Python será avaliado primeiro para um job batch independente e executável localmente, sem virar microserviço de produto. Transformações e agregações SQL devem ser versionadas e reproduzíveis.
- A prova inicial deve usar dados sintéticos. Uma futura role de leitura para ETL deve acessar somente views/projeções agregadas explicitamente aprovadas, nunca receber acesso amplo às tabelas de saúde.
- Não transportar dados pessoais sensíveis para nuvem em uma camada gratuita. Antes de escolher provedor, avaliar região de processamento, LGPD/contratos, criptografia, gestão de chaves, IAM, auditoria, retenção, exclusão e custos após o limite gratuito.
- O desenho deve permanecer portável: job containerizado ou tarefa agendada, configuração por ambiente e nenhuma dependência de serviço proprietário até existir caso de uso aprovado.
- Caso de uso inicial recomendado: validar métricas agregadas com dados sintéticos. Importação real, backup em nuvem e análise de dados de saúde ficam fora desta proposta.

## Estado desta fase

A aplicação implementa identidade, perfil, consentimento, calendário menstrual, sintomas, bem-estar e dashboard de estimativas, com APIs e interface integradas. Consulte PROJECT_STATUS.md para as validações e limitações. A organização acima também inclui direções futuras.

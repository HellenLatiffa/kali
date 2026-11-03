# Requisitos e escopo

## Produto

Kali é uma plataforma original para pessoas que menstruam acompanharem ciclo, sintomas, humor e bem-estar. A comunicação deve ser inclusiva e acolhedora, sem presumir identidade de gênero, regularidade menstrual ou intenção de engravidar. O conteúdo é educativo, não diagnóstico nem prescritivo.

Princípios obrigatórios:

- Privacidade por padrão, minimização de dados, consentimento específico e controle da pessoa usuária.
- Dados de saúde são dados pessoais sensíveis; não comercializar registros íntimos nem fazer publicidade baseada neles.
- Previsões são estimativas com incerteza explícita, nunca contracepção confiável.
- Acessibilidade alinhada à WCAG 2.2 AA, navegação por teclado, leitores de tela e layout responsivo.
- Avaliação especializada de LGPD, segurança e adequação etária antes de qualquer lançamento público.

## Visão de produto

O produto completo poderá incluir identidade e perfil, calendário menstrual, sintomas, diário de autoconhecimento, alimentação e movimento informativos, educação em saúde, estatísticas/exportação, lembretes opcionais e, futuramente, um assistente educacional opt-in. Recursos pagos, redes sociais, IA e integrações complexas estão fora do MVP inicial.

## MVP, em etapas

1. Cadastro, autenticação e autorização.
2. Consentimento para dados sensíveis e controles de privacidade.
3. Perfil e preferências.
4. Registros de menstruação e fluxo.
5. Sintomas e diário de humor/energia.
6. Estimativa simples da próxima menstruação, com intervalo e ressalvas.
7. Dashboard responsivo, histórico e estatísticas básicas.
8. Exportação e exclusão de dados.
9. Testes, documentação, acessibilidade e preparação para avaliação legal.

## Entrega desta fase

Somente a fundação técnica: esqueleto Spring Boot e React, PostgreSQL/Flyway, healthcheck, migration inicial de identidade/consentimento, testes mínimos, configurações Windows/JDK 21 e documentação. Não há endpoints de conta, autenticação, consentimento ou dados de ciclo nesta fase.

## Critérios de aceite por etapa

- Cada etapa tem critérios funcionais, de autorização e privacidade, testes automatizados e revisão da documentação antes da etapa seguinte.
- Nenhum endpoint de dados pessoais é considerado pronto sem teste de isolamento entre contas.
- Nenhuma tela ou texto pode sugerir diagnóstico, prescrição ou confiabilidade contraceptiva.

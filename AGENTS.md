# AGENTS.md: contexto do projeto Hornet

Leia este arquivo inteiro antes de qualquer tarefa. Ele vale para Codex, Claude Code, Antigravity e qualquer outro agente de IA usado neste repositório. No Claude Code, o arquivo CLAUDE.md importa este.

## O que é o projeto
Equipe Hornet, Trilha A (Inteligência Artificial), Missão 01 do SELDON (Engenharia em equipe).

Fundação de um projeto Android de observabilidade térmica: detectar aquecimento anormal da bateria de um aparelho Android usando uma linha de base estatística (média móvel e desvio padrão) mais um limiar absoluto. Não há modelo de IA nesta fase: a linha de base vem antes.

Público: pessoas que exigem muito do celular, especialmente gamers mobile, afetadas por thermal throttling (redução de desempenho por superaquecimento).

Entrega: segunda-feira, 23/11/2026, das 10h às 12h (link do repositório, um PR revisado e o resultado do GitHub Actions). A apresentação consiste em mostrar o README.

Leia também: docs/escopo.md (escopo fechado), docs/arquitetura.md e docs/contrato-core.md (código do contrato e regras do detector). As tarefas de cada pessoa estão nas issues do GitHub (aba Issues, filtro `assignee:@me`), e o fluxo de Git, PR e cronograma está na issue fixada "Guia do time".

## Stack
Kotlin, Jetpack Compose, Gradle (Kotlin DSL), JUnit 4, Java 17 (Temurin). Apenas dois módulos: `app` (Android) e `core` (Kotlin puro, sem Android). Não crie outros módulos.

## Arquitetura em uma frase
Hexagonal leve: o `core` define as regras e as interfaces (portas `AnomalyDetector` e `TemperatureSource`); o `app` implementa a leitura da bateria (adaptador) e a interface. O `app` depende do `core`; o `core` nunca depende do `app` nem de qualquer coisa do Android.

Na fase 2 (25/11 a 27/11) o `core` ganha o contrato v2: `AmostraTelemetria`, `EstadoThrottling`, `ResultadoThrottling` e as portas `TelemetriaSource` e `ThrottlingClassifier` (frequência da CPU, uso da CPU e status térmico, além da temperatura). O `app` implementa a `TelemetriaSource` com adaptadores do Android. Detalhes em `docs/contrato-core.md` (seção 6), `docs/classificador-throttling.md` e na issue fixada "Guia da fase 2" (#49).

## Regras do detector (resumo; detalhes e vetores de teste em docs/contrato-core.md)
Ordem de decisão para cada leitura (parâmetros padrão: janela N = 10, k = 3, piso do desvio 0,1 °C, limiar 45 °C, faixa válida 0 a 60 °C):
1. Valor não finito (NaN, infinito) ou fora da faixa: INVALIDA. Não entra na janela.
2. Valor maior ou igual ao limiar: LIMIAR_EXCEDIDO. Vale mesmo sem base e não entra na janela.
3. Janela com menos de N leituras: SEM_BASE. A leitura entra na janela.
4. z = (x - média) / max(desvio, piso), com desvio populacional da janela. Se z > k: PICO. Não entra na janela. Só subidas contam; queda brusca não é anomalia.
5. Caso contrário: NORMAL. Entra na janela e a mais antiga sai.

O Android entrega a temperatura da bateria em décimos de °C (345 significa 34,5 °C) e nunca em °F.

## Idioma e estilo
- Respostas, comentários, mensagens de commit e documentação em português do Brasil.
- Identificadores de código em português sem acentos (Temperatura, Avaliacao), como no contrato.
- Sem emojis e sem travessão. Use vírgula, dois-pontos ou hífen.
- Código simples e legível, funções curtas, sem bibliotecas novas sem necessidade.

## Comandos
```bash
./gradlew assembleDebug testDebugUnitTest :core:test
```
No Windows: `.\gradlew.bat assembleDebug testDebugUnitTest :core:test`. O comando precisa passar antes de qualquer PR. `testDebugUnitTest` roda os testes do módulo `app` e `:core:test` roda os do módulo `core`.

## Git (obrigatório)
- Nunca faça commit direto em `main` ou `develop`. Trabalhe em uma `feature/*` criada a partir de `develop`.
- Commits atômicos (uma mudança lógica por commit), em Conventional Commits, em português, no imperativo, sem ponto final, até 72 caracteres. Exemplo: `feat(core): aplica limiar absoluto no detector`. Tipos: feat, fix, docs, test, refactor, chore, ci.
- Não inclua trailers de coautoria nem links de conversa nas mensagens de commit. O uso de IA é registrado na descrição do PR.
- Antes de abrir o PR: `git fetch origin`, `git merge origin/develop`, rodar o comando de testes e `git push`.
- Todo PR usa o template de .github/pull_request_template.md e precisa de uma aprovação de outro integrante e do CI verde.
- Nunca use `git push --force`.

## Posse de arquivos (evita conflito de merge)
Edite apenas os arquivos da sua posse. Se precisar mudar um arquivo de outra pessoa, peça no grupo.

| Pessoa | GitHub | Arquivos |
|---|---|---|
| Célio (líder, Backend) | celiobenj | `settings.gradle.kts`, `build.gradle.kts` (raiz), `gradle/wrapper/`, `gradlew`, `core/build.gradle.kts`, contrato do core (`Temperatura.kt`, `StatusLeitura.kt`, `Avaliacao.kt`, `ConfiguracaoDetector.kt`, `AnomalyDetector.kt`, `TemperatureSource.kt`) e `TemperaturaBasicaTest.kt`, `.github/`, `.gitattributes`, `AGENTS.md`, `CLAUDE.md`, `README.md` e todo o `docs/` exceto os arquivos das outras pessoas |
| Bruno (Backend) | Bruno-Bras | `core/src/main/kotlin/dev/hornet/observability/core/BaselineDetector.kt`, `JanelaMovel.kt`, `core/src/test/kotlin/dev/hornet/observability/core/BaselineDetectorTest.kt`, `JanelaMovelTest.kt`, `docs/decisoes/ADR-002.md`, `docs/evidencias/sessao-real.md` |
| Eduardo (Frontend) | ewmneng25 | `app/` inteiro, `gradle/libs.versions.toml` (depois da fundação) e `docs/evidencias/app/` |
| David (QA, Full Stack) | DavidIsrael1026 | em `core/src/test/kotlin/dev/hornet/observability/core/`: `SerieSintetica.kt`, `SerieSinteticaTest.kt` e `AvaliacaoSinteticaTest.kt`; além de `docs/evidencias/metrica-sintetica.md` |
| Marcelo (QA) | Marcelo-Heitor-de-Almeida-Lira | em `core/src/test/kotlin/dev/hornet/observability/core/`: `TemperaturaConversaoTest.kt` e `CenariosDeUnidadeTest.kt`; além de `docs/testes.md` e `docs/evidencias/ci-e-testes.md` |

### Posse de arquivos na fase 2 (25/11 a 27/11)
Vale além da tabela acima. Arquivos novos, por pessoa:

| Pessoa | Arquivos novos da fase 2 |
|---|---|
| Célio | em `core/src/main/kotlin/dev/hornet/observability/core/`: `AmostraTelemetria.kt`, `EstadoThrottling.kt`, `ResultadoThrottling.kt`, `TelemetriaSource.kt` e `ThrottlingClassifier.kt`; em `core/src/test/kotlin/dev/hornet/observability/core/`: `AmostraTelemetriaTest.kt`; além de `README.md`, `docs/arquitetura.md`, `docs/escopo.md` e `docs/contrato-core.md` |
| Bruno | em `core/src/main/kotlin/dev/hornet/observability/core/`: `ClassificadorThrottling.kt` e `ConfiguracaoThrottling.kt`; em `core/src/test/kotlin/dev/hornet/observability/core/`: `ClassificadorThrottlingTest.kt`; além de `docs/classificador-throttling.md` |
| David | em `app/src/main/java/dev/hornet/observability/data/`: `FrequenciaCpuSource.kt`, `UsoCpuSource.kt`, `StatusTermicoSource.kt` e `TelemetriaAndroidSource.kt`; em `app/src/test/java/dev/hornet/observability/data/`: os testes novos dos leitores; além de `docs/validacao/frequencia-e-status-termico.md` |
| Eduardo | o restante de `app/` (inclui `ui/`, o ViewModel e `MainActivity.kt`) e `docs/evidencias/app/` |
| Marcelo | em `core/src/test/kotlin/dev/hornet/observability/core/`: `ClassificadorThrottlingCenariosTest.kt`; além de `docs/validacao/uso-cpu.md`, `docs/testes.md`, `docs/evidencias/ci-e-testes.md`, `docs/evidencias/telemetria-aparelho.md` e `docs/evidencias/aparelho/` |

## Definição de pronto
- O projeto compila e o comando de testes passa.
- Só arquivos da sua posse foram alterados.
- Nenhum módulo ou pasta vazia foi criado.
- Commits atômicos, com mensagens no padrão.
- PR aberto com o template preenchido, CI verde e uma aprovação.

## Para o agente de IA
- Confirme com a pessoa qual issue do GitHub ela está executando e respeite a posse de arquivos dela.
- Proponha um plano e liste os arquivos que serão criados ou alterados antes de editar.
- Rode o comando de testes e mostre o resultado antes de propor commits.
- Explique o que fez em linguagem simples quando a pessoa pedir. O objetivo do projeto também é aprender.

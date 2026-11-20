# Plano de testes

Responsável: Marcelo Almeida (QA), com apoio de David Israel (QA). Este arquivo é da posse de Marcelo. Estado dos casos: Planejado, Automatizado ou Manual verificado.

## 1. Estratégia
- **Unitários na JVM (core):** regras do detector, conversões de unidade e série sintética. Rodam com `./gradlew :core:test` e no CI.
- **Unitários do app:** adaptador `BatteryTemperatureSource` e `TemperaturaViewModel` com dublês (fonte e detector falsos). Rodam com `./gradlew testDebugUnitTest`.
- **Avaliação de qualidade (métrica de sucesso):** teste automatizado sobre a série sintética (CT-17).
- **Manuais no aparelho ou emulador:** tela, botão de simulação e gráfico (CT-20 a CT-24).
- **Fora desta fase:** testes instrumentados e testes de integração com o sistema Android.

## 2. Como executar
```bash
./gradlew assembleDebug testDebugUnitTest :core:test
```
Relatórios em `core/build/reports/tests/test/index.html` e `app/build/reports/tests/testDebugUnitTest/index.html`. No GitHub: aba Actions, execução do PR, artefato `apk-e-relatorios`.

## 3. Casos críticos
| ID | Cenário | Entrada | Esperado | Tipo | Arquivo | Estado |
|---|---|---|---|---|---|---|
| CT-01 | Conversão °F para °C | 95, 113, 32, 212 °F | 35, 45, 0, 100 °C | Unitário | TemperaturaConversaoTest | Planejado |
| CT-02 | Conversão de décimos | 345, 450, 0 | 34,5, 45,0 e 0,0 °C | Unitário | TemperaturaConversaoTest | Planejado |
| CT-03 | Ida e volta | 35 °C | 95 °F | Unitário | TemperaturaConversaoTest | Planejado |
| CT-04 | Mesma série em °F e em °C | série convertida de °C para °F e de volta | mesmos status | Unitário | CenariosDeUnidadeTest | Planejado |
| CT-05 | Série em °F lida como °C | valores de 86 a 95 | todos INVALIDA | Unitário | CenariosDeUnidadeTest | Planejado |
| CT-06 | Décimos lidos como °C | 345 como se fosse °C | INVALIDA | Unitário | CenariosDeUnidadeTest | Planejado |
| CT-07 | Valores não finitos | NaN, infinitos | INVALIDA | Unitário | BaselineDetectorTest | Planejado |
| CT-08 | Fronteiras da faixa | 0,0, -0,1, 60,0, 60,1 | SEM_BASE, INVALIDA, LIMIAR_EXCEDIDO, INVALIDA | Unitário | BaselineDetectorTest | Planejado |
| CT-09 | Calibração | 10 leituras iguais | 10 SEM_BASE e a 11ª NORMAL | Unitário | BaselineDetectorTest | Planejado |
| CT-10 | Limiar sem base | 45,0 na primeira leitura | LIMIAR_EXCEDIDO | Unitário | BaselineDetectorTest | Planejado |
| CT-11 | Pico por z-score | 36,0 após 10 leituras de 30,0 | PICO, e a seguinte NORMAL | Unitário | BaselineDetectorTest | Planejado |
| CT-12 | Fronteira de k | 32,0 e 32,1 após janela 30/31 | NORMAL e PICO | Unitário | BaselineDetectorTest | Planejado |
| CT-13 | Queda brusca | 24,0 após 10 leituras de 30,0 | NORMAL | Unitário | BaselineDetectorTest | Planejado |
| CT-14 | Piso do desvio | 30,2 e 30,5 após série constante | NORMAL e PICO | Unitário | BaselineDetectorTest | Planejado |
| CT-15 | Reinício | reiniciar() | volta a SEM_BASE | Unitário | BaselineDetectorTest | Planejado |
| CT-16 | Janela não contaminada | PICO, LIMIAR e INVALIDA intercalados | média e desvio da janela inalterados | Unitário | BaselineDetectorTest | Planejado |
| CT-17 | Métrica de sucesso | série sintética (semente 42) | recall de pelo menos 90% e falsos positivos de no máximo 5% | Avaliação | AvaliacaoSinteticaTest | Planejado |
| CT-18 | Determinismo | mesma semente duas vezes | séries idênticas | Unitário | SerieSinteticaTest | Planejado |
| CT-19 | Adaptador | lambda devolvendo 345, null e Int.MIN_VALUE | 34,5 °C, null e null | Unitário (app) | BatteryTemperatureSourceTest | Planejado |
| CT-20 | App abre e mostra a temperatura real | aparelho ou emulador | valor coerente e status visível | Manual | | Planejado |
| CT-21 | Botão "Simular pico" | tocar após a calibração | status PICO e ponto destacado no gráfico | Manual | | Planejado |
| CT-22 | Gráfico | 60 leituras | série, linha do limiar e pontos anômalos visíveis | Manual | | Planejado |
| CT-23 | Rotação da tela | girar o aparelho | histórico mantido | Manual | | Planejado |
| CT-24 | Acessibilidade | leitor de tela e tamanho de fonte grande | status lido em texto e gráfico com descrição | Manual | | Planejado |

Dica para CT-20 a CT-22 em emulador ou aparelho com depuração USB: `adb shell dumpsys battery set temp 450` define a temperatura em décimos de °C (se o comando for aceito na versão do Android) e `adb shell dumpsys battery reset` restaura.

## 4. Critérios de aceitação por tarefa
Estão nas issues do GitHub (uma por tarefa, com os critérios de aceitação). O QA confere cada critério ao revisar o PR correspondente.

## 5. Evidências
Registradas pelo QA. Marcelo, em docs/evidencias/ci-e-testes.md: link da execução do CI verde, print do artefato baixado e resultado dos testes manuais. David, em docs/evidencias/metrica-sintetica.md: valores medidos de recall e falsos positivos e como reproduzi-los.

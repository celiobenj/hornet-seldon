# Plano de testes

Responsável: Marcelo Almeida (QA), com apoio de David Israel (QA). Este arquivo é da posse de Marcelo. Estado dos casos: Planejado, Automatizado ou Manual verificado.

## 1. Estratégia
- **Unitários na JVM (core):** regras do detector, conversões de unidade, série sintética e classificador de throttling (CT-25 a CT-41). Rodam com `./gradlew :core:test` e no CI.
- **Unitários do app:** adaptador `BatteryTemperatureSource` e `TemperaturaViewModel` com dublês (fonte e detector falsos). Rodam com `./gradlew testDebugUnitTest`.
- **Avaliação de qualidade (métrica de sucesso):** teste automatizado sobre a série sintética (CT-17).
- **Manuais no aparelho ou emulador:** tela, botão de simulação e gráfico (CT-20 a CT-24), e tela multi-sinal do classificador (CT-42 a CT-48).
- **Fora desta fase:** testes instrumentados e testes de integração com o sistema Android.

## 2. Como executar
No Linux e no macOS, use `./gradlew`. No Windows, use `.\gradlew.bat` nos mesmos comandos.

Somente os testes unitários do módulo `core`, na JVM:
```bash
./gradlew :core:test
```

Somente os testes unitários do módulo `app`, na JVM:
```bash
./gradlew testDebugUnitTest
```

Comando completo usado no CI, que também gera o APK de depuração:
```bash
./gradlew assembleDebug testDebugUnitTest :core:test
```
É este comando que precisa passar antes de qualquer PR. O workflow `.github/workflows/ci.yml` executa exatamente ele com Java 17 (Temurin).

Relatórios em `core/build/reports/tests/test/index.html` e `app/build/reports/tests/testDebugUnitTest/index.html`. No GitHub: aba Actions, execução do PR, artefato `apk-e-relatorios`.

## 3. Casos críticos
| ID | Cenário | Entrada | Esperado | Tipo | Arquivo | Estado |
|---|---|---|---|---|---|---|
| CT-01 | Conversão °F para °C | 95, 113, 32, 212 °F | 35, 45, 0, 100 °C | Unitário | TemperaturaConversaoTest | Automatizado |
| CT-02 | Conversão de décimos | 345, 450, 0 | 34,5, 45,0 e 0,0 °C | Unitário | TemperaturaConversaoTest | Automatizado |
| CT-03 | Ida e volta | 35 °C | 95 °F | Unitário | TemperaturaConversaoTest | Automatizado |
| CT-04 | Mesma série em °F e em °C | série convertida de °C para °F e de volta | mesmos status | Unitário | CenariosDeUnidadeTest | Automatizado |
| CT-05 | Série em °F lida como °C | valores de 86 a 95 | todos INVALIDA | Unitário | CenariosDeUnidadeTest | Automatizado |
| CT-06 | Décimos lidos como °C | 345 como se fosse °C | INVALIDA | Unitário | CenariosDeUnidadeTest | Automatizado |
| CT-07 | Valores não finitos | NaN, infinitos | INVALIDA | Unitário | BaselineDetectorTest | Automatizado |
| CT-08 | Fronteiras da faixa | 0,0, -0,1, 60,0, 60,1 | SEM_BASE, INVALIDA, LIMIAR_EXCEDIDO, INVALIDA | Unitário | BaselineDetectorTest | Automatizado |
| CT-09 | Calibração | 10 leituras iguais | 10 SEM_BASE e a 11ª NORMAL | Unitário | BaselineDetectorTest | Automatizado |
| CT-10 | Limiar sem base | 45,0 na primeira leitura | LIMIAR_EXCEDIDO | Unitário | BaselineDetectorTest | Automatizado |
| CT-11 | Pico por z-score | 36,0 após 10 leituras de 30,0 | PICO, e a seguinte NORMAL | Unitário | BaselineDetectorTest | Automatizado |
| CT-12 | Fronteira de k | 32,0 e 32,1 após janela 30/31 | NORMAL e PICO | Unitário | BaselineDetectorTest | Automatizado |
| CT-13 | Queda brusca | 24,0 após 10 leituras de 30,0 | NORMAL | Unitário | BaselineDetectorTest | Automatizado |
| CT-14 | Piso do desvio | 30,2 e 30,5 após série constante | NORMAL e PICO | Unitário | BaselineDetectorTest | Automatizado |
| CT-15 | Reinício | reiniciar() | volta a SEM_BASE | Unitário | BaselineDetectorTest | Automatizado |
| CT-16 | Janela não contaminada | PICO, LIMIAR e INVALIDA intercalados | média e desvio da janela inalterados | Unitário | BaselineDetectorTest | Automatizado |
| CT-17 | Métrica de sucesso | série sintética (semente 42) | recall de pelo menos 90% e falsos positivos de no máximo 5% | Avaliação | AvaliacaoSinteticaTest | Automatizado |
| CT-18 | Determinismo | mesma semente duas vezes | séries idênticas | Unitário | SerieSinteticaTest | Automatizado |
| CT-19 | Adaptador | lambda devolvendo 345, null e Int.MIN_VALUE | 34,5 °C, null e null | Unitário (app) | BatteryTemperatureSourceTest | Automatizado |
| CT-20 | App abre e mostra a temperatura real | aparelho ou emulador | valor coerente e status visível | Manual | | Manual verificado |
| CT-21 | Botão "Simular pico" | tocar após a calibração | status PICO e ponto destacado no gráfico | Manual | | Manual verificado |
| CT-22 | Gráfico | 60 leituras | série, linha do limiar e pontos anômalos visíveis | Manual | | Manual verificado |
| CT-23 | Rotação da tela | girar o aparelho | histórico mantido | Manual | | Manual verificado |
| CT-24 | Acessibilidade | leitor de tela e tamanho de fonte grande | status lido em texto e gráfico com descrição | Manual | | Manual verificado |
| CT-25 | Estado INDETERMINADO | temperatura inválida de 61 °C com `Avaliacao` `INVALIDA`, atual 1.000.000 kHz, máxima 2.000.000 kHz, teto 1.600.000 kHz, uso 90%, status térmico 6 | `INDETERMINADO`, mesmo com status térmico elevado | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-26 | Estado NORMAL | 35 °C, `NORMAL`, atual 1.000.000 kHz, máxima 2.000.000 kHz, teto 2.000.000 kHz, uso 10%, status térmico 0 | `NORMAL`, frequência baixa com uso baixo é economia de energia | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-27 | Estado AQUECENDO | 36 °C com `PICO` e 38 °C com `NORMAL`, sem sinais opcionais | `AQUECENDO` nos dois casos, por anomalia térmica ou temperatura absoluta | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-28 | Estado THROTTLING_PROVAVEL | 39 °C, `NORMAL`, máxima 2.000.000 kHz, teto 1.780.000 kHz, uso e status térmico nulos | `THROTTLING_PROVAVEL` | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-29 | Estado THROTTLING_CONFIRMADO | 35 °C, `NORMAL`, status térmico 2, demais sinais opcionais nulos | `THROTTLING_CONFIRMADO` | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-30 | Limite exato do teto | 40 °C, `NORMAL`, máxima 2.000.000 kHz, teto 1.800.000 kHz, razão exata 0,90 | `AQUECENDO`, razão igual ao limite não é teto imposto | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-31 | Limite exato da frequência | 38 °C, `NORMAL`, atual 1.400.000 kHz, máxima 2.000.000 kHz, uso 70%, status térmico 0, razão exata 0,70 | `AQUECENDO`, razão igual ao limite não é frequência baixa | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-32 | Limite exato de uso da CPU | 38 °C, `NORMAL`, atual 1.380.000 kHz, máxima 2.000.000 kHz, uso exatamente 70%, status térmico 0 | `THROTTLING_PROVAVEL`, uso 70% é alto e a razão 0,69 é baixa | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-33 | Limite exato de 38 °C | 38 °C e 37,9 °C, `NORMAL`, sem sinais opcionais | `AQUECENDO` em 38 °C e `NORMAL` em 37,9 °C | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-34 | Limite exato de 40 °C | 40 °C e 39,9 °C, `NORMAL`, máxima 2.000.000 kHz, teto 1.780.000 kHz | `THROTTLING_CONFIRMADO` em 40 °C e `THROTTLING_PROVAVEL` em 39,9 °C | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-35 | Limites exatos do status térmico | 35 °C, `NORMAL`, status térmico 1 e depois status térmico 2 | `NORMAL` com 1 e `THROTTLING_CONFIRMADO` com 2 | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-36 | Sem uso da CPU | 38 °C, `NORMAL`, atual 1.380.000 kHz, máxima 2.000.000 kHz, uso nulo, status térmico 0 | `AQUECENDO`, regra de frequência baixa com uso alto é ignorada | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-37 | Sem status térmico | 35 °C, `NORMAL`, status térmico nulo, demais sinais opcionais ausentes | `NORMAL`, sem exceção e sem confirmação por status | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-38 | Sem frequência | atual, máxima e teto nulos; máxima zero; atual zero; falta apenas do teto com atual e máxima presentes | razões ausentes quando o dado necessário falta, razão de frequência disponível quando só o teto falta, `NORMAL` sem exceção | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-39 | Somente temperatura | 35 °C, `NORMAL`, todos os sinais opcionais nulos | `NORMAL`, classificação sem exceção usando apenas temperatura | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-40 | Unidades | atual 1.380.000 kHz, máxima 2.000.000 kHz, teto 1.780.000 kHz, `Temperatura.deDecimosDeCelsius(345)` | razões 0,69 e 0,89 sem arredondar e sem uso de MHz no core; 345 décimos viram 34,5 °C | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-41 | Precedência | 35 °C com status térmico 2, teto reduzido e uso alto; 35 °C com atual 1.000.000 kHz, máxima 2.000.000 kHz e uso 10% | `THROTTLING_CONFIRMADO` por status térmico no primeiro caso e `NORMAL` por economia de energia no segundo | Automatizado | ClassificadorThrottlingCenariosTest | Automatizado |
| CT-42 | Tela multi-sinal em repouso | aparelho em repouso, modo demonstração desligado, após duas amostras de 5 s | `NORMAL`, cards de temperatura, frequência, uso e status térmico visíveis, uso sem valor indisponível após a segunda amostra | Manual | Tela multi-sinal | Planejado |
| CT-43 | Botão Gerar carga por 60 s da E2 | tocar no botão e observar durante 60 s e 30 s depois | uso da CPU sobe, frequência responde, estado permanece `NORMAL` se temperatura, teto e status térmico não indicarem throttling | Manual | Tela multi-sinal | Planejado |
| CT-44 | Modo demonstração da E3 para AQUECENDO | ativar cenário de pico ou temperatura 38 °C | estado `AQUECENDO` e motivo identifica temperatura | Manual | Tela multi-sinal | Planejado |
| CT-45 | Modo demonstração da E3 para THROTTLING_PROVAVEL | ativar cenário de teto reduzido com 38 °C ou frequência baixa com uso alto e 38 °C | estado `THROTTLING_PROVAVEL` e motivo identifica a evidência usada | Manual | Tela multi-sinal | Planejado |
| CT-46 | Modo demonstração da E3 para THROTTLING_CONFIRMADO | ativar status térmico 2 ou teto reduzido com 40 °C | estado `THROTTLING_CONFIRMADO` e motivo identifica status térmico ou teto mais temperatura | Manual | Tela multi-sinal | Planejado |
| CT-47 | Modo demonstração da E3 para INDETERMINADO | ativar temperatura inválida com status térmico alto | tela mostra Dados insuficientes e motivo identifica temperatura inválida | Manual | Tela multi-sinal | Planejado |
| CT-48 | Sinais ausentes e unidades na tela | modo demonstração sem uso, sem status térmico e sem frequência; conferir 345 décimos de °C | ausentes aparecem como indisponível, temperatura aparece como 34,5 °C, frequência da tela em MHz sem afetar o core em kHz | Manual | Tela multi-sinal | Planejado |

Dica para CT-20 a CT-22 em emulador ou aparelho com depuração USB: `adb shell dumpsys battery set temp 450` define a temperatura em décimos de °C (se o comando for aceito na versão do Android) e `adb shell dumpsys battery reset` restaura.

CT-25 a CT-41 dependem de `ClassificadorThrottlingCenariosTest`. CT-42 a CT-48 dependem do botão `Gerar carga por 60 s` da E2 e do modo demonstração da E3, e serão executados em M3.

## 4. Critérios de aceitação por tarefa
Estão nas issues do GitHub (uma por tarefa, com os critérios de aceitação). O QA confere cada critério ao revisar o PR correspondente.

### 4.1 Conversões e unidades (CT-01 a CT-06)
- CT-01: os quatro pares de Fahrenheit para Celsius (95, 113, 32 e 212) produzem 35, 45, 0 e 100 °C, com tolerância de `1e-9` em cada `Double`.
- CT-02: 345, 450 e 0 décimos de °C produzem 34,5, 45,0 e 0,0 °C, com a mesma tolerância.
- CT-03: 35 °C produz 95 °F, e converter esse valor de volta produz 35 °C.
- CT-04: a série em °F, convertida para °C e depois de volta para °F, produz exatamente os mesmos status da série original em °C.
- CT-05: todos os valores de 86 a 95 lidos como °C recebem `INVALIDA` e não entram na janela.
- CT-06: 345 lido como °C recebe `INVALIDA`, enquanto 345 décimos viram 34,5 °C.

### 4.2 Detector e janela móvel (CT-07 a CT-16)
- Cada vetor V-01 a V-14 de `docs/contrato-core.md` produz o status e os valores de `media`, `desvio` e `zScore` esperados. Fora de `NORMAL` e `PICO`, esses três campos ficam nulos.
- A janela recebe somente leituras `SEM_BASE` e `NORMAL`. Após `PICO`, `LIMIAR_EXCEDIDO` ou `INVALIDA`, a média e o desvio da janela continuam inalterados.
- Uma queda brusca é classificada como `NORMAL`, e um `PICO` não altera a classificação da leitura seguinte.
- Após `reiniciar()`, a próxima leitura volta a ser `SEM_BASE`.

### 4.3 Métrica e determinismo (CT-17 e CT-18)
- CT-17: com semente 42, recall de pelo menos 90% nos cinco picos injetados e no máximo 5% de falsos positivos entre as leituras não injetadas a partir do índice 10.
- CT-18: duas execuções com a mesma semente produzem séries idênticas, elemento por elemento.

### 4.4 Adaptador do app (CT-19)
- `BatteryTemperatureSourceTest`: 345 vira 34,5 °C, uma leitura `null` continua `null`, e `Int.MIN_VALUE` vira `null`.

### 4.5 Verificações manuais (CT-20 a CT-24)
- CT-20: o valor exibido é coerente com a temperatura da bateria e o status aparece visível na tela.
- CT-21: após a calibração, tocar no botão de simulação muda o status para `PICO` e destaca o ponto no gráfico.
- CT-22: com 60 leituras, o gráfico mostra a série, a linha do limiar e os pontos anômalos.
- CT-23: depois de girar a tela, o histórico de leituras continua presente.
- CT-24: o leitor de tela anuncia o status em texto e o gráfico possui descrição de conteúdo.

### 4.6 Classificador de throttling e tela multi-sinal (CT-25 a CT-48)
- CT-25 a CT-29: cada estado aparece em pelo menos um caso, e o motivo identifica a regra aplicada.
- CT-30 a CT-35: os limiares exatos seguem `docs/classificador-throttling.md`. Razão do teto 0,90 e razão de frequência 0,70 não acionam as regras. Uso 70%, temperatura 38 °C, temperatura 40 °C e status térmico 2 são inclusivos. Status térmico 1 não confirma throttling.
- CT-36 a CT-39: sinais ausentes são ignorados sem exceção. A regra que depende do sinal ausente não aciona, e as demais continuam disponíveis.
- CT-40: o core usa frequência em kHz e converte 345 décimos de °C para 34,5 °C. As razões são calculadas sem arredondar e sem usar MHz.
- CT-41: status térmico 2 tem prioridade sobre teto e uso alto. Frequência baixa com uso baixo resulta em `NORMAL`.
- CT-42 a CT-48: a execução depende da tela multi-sinal integrada, do botão `Gerar carga por 60 s` da E2 e do modo demonstração da E3. O protocolo detalhado está na seção 5.

Um caso só muda de estado para Automatizado ou Manual verificado quando o comando completo do CI passa e o critério correspondente foi conferido.

## 5. Protocolo de teste manual da tela multi-sinal (M3)

Executar em aparelho físico, não em emulador. Este protocolo cobre CT-42 a CT-48. Só marcar `Manual verificado` depois de executar os passos no aparelho e registrar a evidência.

### 5.1 Pré-requisitos

- build de depuração com a tela multi-sinal integrada, o botão `Gerar carga por 60 s` da E2 e o modo demonstração da E3;
- aparelho com Android 10 ou superior para `PowerManager.getCurrentThermalStatus()`;
- depuração USB habilitada e `adb` disponível para restaurar a temperatura, se o modo demonstração não simular esse dado;
- aparelho em repouso, sem carregador e com temperatura da bateria estável por pelo menos 5 minutos.

Se algum pré-requisito faltar, manter CT-42 a CT-48 como `Planejado`.

### 5.2 Preparação

1. Instalar o build no aparelho e abrir o app.
2. Aguardar duas amostras de 5 segundos. A primeira amostra pode mostrar uso da CPU como `indisponível`, porque o cálculo precisa de duas leituras.
3. Conferir os cards de temperatura da bateria, frequência da CPU, uso da CPU e status térmico do Android.
4. Registrar o estado inicial, o motivo exibido e os valores dos quatro cards.

### 5.3 Execução por estado

| Estado | Como provocar | O que observar | Caso |
|---|---|---|---|
| `NORMAL` | Manter o repouso após a calibração. Depois tocar em `Gerar carga por 60 s` da E2 e aguardar 60 segundos mais 30 segundos de recuperação. | No repouso, estado `Normal`, motivo compatível com operação normal e uso com valor numérico após a segunda amostra. Durante a carga, o uso sobe e a frequência responde. Com temperatura abaixo de 38 °C, teto em 100% e status térmico 0 ou 1, o estado continua `Normal`. Após os 60 segundos, o uso volta a cair. | CT-42 e CT-43 |
| `AQUECENDO` | Ativar no modo demonstração da E3 o cenário de pico ou o cenário de temperatura 38,0 °C. | Estado `Aquecendo`, motivo cita pico ou faixa de aquecimento, e o card de temperatura mostra o valor coerente com o cenário. | CT-44 |
| `THROTTLING_PROVAVEL` | Ativar no modo demonstração da E3 o cenário de teto reduzido para 0,89 do máximo com 38 °C, ou o cenário de frequência baixa com uso alto e 38 °C. | Estado `Throttling provável`, motivo cita teto reduzido ou frequência baixa com uso alto, e temperatura no limite de aquecimento. | CT-45 |
| `THROTTLING_CONFIRMADO` | Ativar no modo demonstração da E3 o cenário de status térmico 2, ou o cenário de teto 0,80 com 40 °C. Se houver cenário de sobreposição, ativá-lo também. | Estado `Throttling confirmado`, motivo cita nível térmico 2 ou teto reduzido com temperatura. Na sobreposição com status térmico 2, o status térmico vence. | CT-46 |
| `INDETERMINADO` | Ativar no modo demonstração da E3 o cenário de temperatura inválida com status térmico alto. | A tela mostra `Dados insuficientes`, o motivo cita temperatura inválida e nenhum estado de throttling é confirmado. | CT-47 |
| Sinais ausentes e unidades | Ativar no modo demonstração da E3 o cenário sem uso da CPU, sem status térmico e sem frequência. Informar 345 décimos de °C quando o cenário permitir. | Sinais ausentes aparecem como `indisponível`, nunca como zero. A temperatura aparece como 34,5 °C. A frequência na tela aparece em MHz, enquanto o core recebe kHz. O app não fecha e não mostra exceção. | CT-48 |

Após cada cenário da E3, desativar o modo demonstração, aguardar duas amostras e conferir o retorno ao estado anterior.

### 5.4 Limpeza e evidências

1. Desativar o modo demonstração e encerrar a carga se ainda estiver ativa.
2. Se a temperatura foi alterada por `adb`, executar `adb shell dumpsys battery reset`.
3. Registrar em `docs/evidencias/telemetria-aparelho.md`: modelo do aparelho, versão do Android, horário, CT executados, estado observado, motivo exibido e print da tela.
4. Marcar cada caso como `Manual verificado` somente depois que o comportamento esperado foi conferido no aparelho.

## 6. Evidências
Registradas pelo QA. Marcelo, em docs/evidencias/ci-e-testes.md: link da execução do CI verde, print do artefato baixado e resultado dos testes manuais. David, em docs/evidencias/metrica-sintetica.md: valores medidos de recall e falsos positivos e como reproduzi-los.

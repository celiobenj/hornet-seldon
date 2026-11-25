# Regras do classificador de throttling

Rascunho da tarefa B1. O classificador combina a temperatura da bateria, as frequências da CPU, o uso da CPU e o status térmico do Android. A temperatura vem do detector da v1 e serve como evidência indireta de aquecimento. [Tarefa B1](https://github.com/celiobenj/hornet-seldon/issues/37)

## Entradas

O classificador recebe uma `AmostraTelemetria` e a `Avaliacao` produzida pelo `BaselineDetector` para a temperatura da mesma amostra. Cada classificação considera uma amostra. O resultado é um `ResultadoThrottling`, com o estado e um motivo em português que identifica a regra aplicada. [Rascunho do contrato v2](https://github.com/celiobenj/hornet-seldon/issues/33) · [Contrato do detector](contrato-core.md)

| Entrada | Significado |
|---|---|
| `instanteMs` | Identifica o instante da coleta. A decisão usa os sinais dessa amostra. |
| `temperaturaBateria` | Temperatura da bateria em °C. |
| `frequenciaAtualKhz` | Frequência atual da CPU em kHz, obtida de `scaling_cur_freq`. |
| `frequenciaMaximaKhz` | Frequência máxima de fábrica em kHz, obtida de `cpuinfo_max_freq`. |
| `tetoFrequenciaKhz` | Teto de frequência em kHz, obtido de `scaling_max_freq`. |
| `usoCpuPercentual` | Uso da CPU em percentual, de 0 a 100, inclusive. |
| `statusTermico` | Código térmico do Android, de 0 (`NONE`) a 6 (`SHUTDOWN`), inclusive. |
| `avaliacaoTemperatura` | `Avaliacao` do detector da v1. `INVALIDA` indica uma temperatura inválida. `PICO` e `LIMIAR_EXCEDIDO` indicam anomalia térmica. |

A razão de frequência é `frequenciaAtualKhz / frequenciaMaximaKhz`. A razão do teto é `tetoFrequenciaKhz / frequenciaMaximaKhz`. As duas divisões usam ponto flutuante. Uma razão de teto abaixo de `razaoTetoImposto` indica um teto imposto para fins da classificação. Use as razões sem arredondar antes da comparação.

## Ordem de decisão

A primeira regra cuja condição é satisfeita determina o estado e o motivo.

1. Se a avaliação da temperatura for `INVALIDA`, retorna `INDETERMINADO`. A classificação exige uma temperatura válida, mesmo quando há um status térmico elevado.
2. Se o status térmico for pelo menos `statusTermicoMinimo`, retorna `THROTTLING_CONFIRMADO`. O motivo informa o nível térmico reportado pelo Android. Com o parâmetro padrão, a confirmação começa em `MODERATE` (2).
3. Se houver teto imposto e a temperatura atingir `temperaturaMinimaConfirmacaoCelsius`, retorna `THROTTLING_CONFIRMADO`. O classificador infere throttling pela combinação do teto reduzido com a bateria quente. O motivo identifica o teto e a temperatura observados.
4. Se houver teto imposto e a temperatura atingir `temperaturaMinimaAquecimentoCelsius`, retorna `THROTTLING_PROVAVEL`. O teto reduzido coincide com aquecimento da bateria.
5. Se o uso da CPU estiver disponível, a razão de frequência for menor que `razaoFrequenciaBaixa`, o uso for pelo menos `usoCpuAltoPercentual` e a temperatura atingir `temperaturaMinimaAquecimentoCelsius`, retorna `THROTTLING_PROVAVEL`. A CPU está sob carga alta e opera em frequência baixa durante o aquecimento.
6. Se a avaliação da temperatura indicar `PICO` ou `LIMIAR_EXCEDIDO`, ou se a temperatura atingir `temperaturaMinimaAquecimentoCelsius`, retorna `AQUECENDO`. A temperatura subiu de forma anormal ou atingiu o limiar de aquecimento.
7. Nos demais casos, retorna `NORMAL`. Frequência baixa com uso baixo é compatível com economia de energia. Quando há sinais ausentes, o estado indica que as evidências disponíveis não acionaram as regras anteriores.

## Sinais ausentes

- Dados opcionais indisponíveis ficam `null`. Uma frequência nula, zero ou negativa torna ausente toda razão que dependa dela. Se a frequência máxima faltar, as duas razões ficam ausentes. Se faltar apenas a frequência atual ou o teto, a outra razão ainda pode ser calculada.
- Uso da CPU fora de 0 a 100, NaN ou infinito é ausente. Um uso de 0% é válido. Status térmico fora de 0 a 6 é ausente. O código 0 é válido.
- Uma regra que depende de um sinal ausente é ignorada, sem lançar exceção. Sem uso da CPU, uma frequência baixa não aciona a regra 5. As regras do status térmico, do teto e da temperatura continuam disponíveis conforme seus próprios sinais.
- Uma temperatura inválida é tratada pela regra 1. Uma avaliação `SEM_BASE` indica uma leitura válida durante a calibração e permite aplicar as regras que usam o valor absoluto em °C.

## Vetores de teste

As frequências da tabela estão em kHz. `null` indica um sinal indisponível. O status da avaliação é uma entrada de cada vetor, produzida pelo detector com o histórico correspondente. A coluna Regra registra a primeira condição satisfeita com os valores padrão.

| ID | Temp. (°C) | Avaliação | Atual | Máxima | Teto | Uso (%) | Status térmico | Regra | Estado esperado |
|---|---:|---|---:|---:|---:|---:|---:|---:|---|
| T-01 | 61,0 | `INVALIDA` | 1 000 000 | 2 000 000 | 1 600 000 | 90 | 6 | 1 | `INDETERMINADO` |
| T-02 | 35,0 | `NORMAL` | null | null | null | null | 2 | 2 | `THROTTLING_CONFIRMADO` |
| T-03 | 35,0 | `NORMAL` | null | null | null | null | 1 | 7 | `NORMAL` |
| T-04 | 40,0 | `NORMAL` | null | 2 000 000 | 1 780 000 | null | null | 3 | `THROTTLING_CONFIRMADO` |
| T-05 | 39,9 | `NORMAL` | null | 2 000 000 | 1 780 000 | null | null | 4 | `THROTTLING_PROVAVEL` |
| T-06 | 38,0 | `NORMAL` | null | 2 000 000 | 1 780 000 | null | 0 | 4 | `THROTTLING_PROVAVEL` |
| T-07 | 37,9 | `NORMAL` | null | 2 000 000 | 1 780 000 | null | 0 | 7 | `NORMAL` |
| T-08 | 40,0 | `NORMAL` | null | 2 000 000 | 1 800 000 | null | null | 6 | `AQUECENDO` |
| T-09 | 38,0 | `NORMAL` | 1 380 000 | 2 000 000 | null | 70 | null | 5 | `THROTTLING_PROVAVEL` |
| T-10 | 38,0 | `NORMAL` | 1 400 000 | 2 000 000 | null | 70 | 0 | 6 | `AQUECENDO` |
| T-11 | 38,0 | `NORMAL` | 1 380 000 | 2 000 000 | null | 69,9 | 0 | 6 | `AQUECENDO` |
| T-12 | 38,0 | `NORMAL` | 1 380 000 | 2 000 000 | null | null | 0 | 6 | `AQUECENDO` |
| T-13 | 36,0 | `PICO` | null | null | null | null | null | 6 | `AQUECENDO` |
| T-14 | 45,0 | `LIMIAR_EXCEDIDO` | null | null | null | null | null | 6 | `AQUECENDO` |
| T-15 | 35,0 | `NORMAL` | 1 000 000 | 2 000 000 | 2 000 000 | 10 | 0 | 7 | `NORMAL` |
| T-16 | 37,9 | `SEM_BASE` | null | null | null | null | null | 7 | `NORMAL` |
| T-17 | 38,0 | `SEM_BASE` | null | null | null | null | null | 6 | `AQUECENDO` |
| T-18 | 40,0 | `NORMAL` | 1 000 000 | 0 | 1 600 000 | 80 | 7 | 6 | `AQUECENDO` |
| T-19 | 38,0 | `NORMAL` | 1 380 000 | 2 000 000 | null | 101 | 0 | 6 | `AQUECENDO` |
| T-20 | 41,0 | `NORMAL` | 1 000 000 | 2 000 000 | 1 600 000 | 90 | 2 | 2 | `THROTTLING_CONFIRMADO` |
| T-21 | 40,0 | `NORMAL` | 1 000 000 | 2 000 000 | 1 600 000 | 90 | 1 | 3 | `THROTTLING_CONFIRMADO` |
| T-22 | 37,9 | `NORMAL` | 1 380 000 | 2 000 000 | null | 70 | null | 7 | `NORMAL` |
| T-23 | 38,0 | `NORMAL` | 1 380 000 | 2 000 000 | 1 780 000 | 70 | null | 4 | `THROTTLING_PROVAVEL` |

T-08 usa razão de teto igual a 0,90. T-10 usa razão de frequência igual a 0,70. T-09 e T-22 têm a mesma frequência e o mesmo uso da CPU. A diferença de 0,1 °C entre as amostras muda o resultado na fronteira da regra 5.

Nos casos de sobreposição, confira também a evidência que o campo `motivo` deve comunicar. A redação pode variar.

| Vetor | Regra que vence | Evidência esperada no motivo |
|---|---:|---|
| T-20 | 2 | O Android reportou nível térmico 2 (`MODERATE`). |
| T-21 | 3 | O teto foi reduzido a 80% do máximo e a bateria atingiu 40 °C. |
| T-23 | 4 | O teto foi reduzido a 89% do máximo e a bateria atingiu 38 °C. A regra 4 tem prioridade sobre a regra 5, que também se aplica. |

## Parâmetros de `ConfiguracaoThrottling`

| Parâmetro | Padrão | Significado |
|---|---:|---|
| `temperaturaMinimaAquecimentoCelsius` | 38,0 °C | Temperatura mínima inclusiva para o aquecimento e para as regras de throttling provável. |
| `temperaturaMinimaConfirmacaoCelsius` | 40,0 °C | Temperatura mínima inclusiva para confirmar throttling quando há teto imposto. |
| `razaoTetoImposto` | 0,90 | Limite exclusivo da razão do teto. A razão precisa ficar abaixo desse valor. |
| `razaoFrequenciaBaixa` | 0,70 | Limite exclusivo da razão de frequência. A razão precisa ficar abaixo desse valor. |
| `usoCpuAltoPercentual` | 70,0% | Uso mínimo inclusivo da CPU para considerar a carga alta. |
| `statusTermicoMinimo` | 2 (`MODERATE`) | Código mínimo inclusivo para confirmar throttling pelo status térmico. |

Os limiares são provisórios. A tarefa B3 ajustará esses valores com as observações de D1, M1 e M3 no aparelho. [Tarefa B3](https://github.com/celiobenj/hornet-seldon/issues/39) · [Guia da fase 2](https://github.com/celiobenj/hornet-seldon/issues/49)

Para verificar que a implementação usa a configuração recebida, repita T-09 com `temperaturaMinimaAquecimentoCelsius` em 39,0 °C e os demais parâmetros no padrão. A amostra de 38,0 °C passa à regra 7 e o resultado esperado é `NORMAL`.

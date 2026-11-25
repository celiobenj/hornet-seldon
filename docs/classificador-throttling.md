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

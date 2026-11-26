# Contrato do core

Fonte da verdade do módulo `core`. O dono destes arquivos é o líder (Célio). Qualquer mudança de assinatura deve ser combinada no grupo antes, porque Bruno, Eduardo, David e Marcelo programam contra este contrato em paralelo.

Pacote: `dev.hornet.observability.core`
Pasta: `core/src/main/kotlin/dev/hornet/observability/core/`

## 1. Arquivos do contrato (código)

Seis arquivos, criados na fundação (tarefa C-1). Copie exatamente.

### Temperatura.kt
```kotlin
package dev.hornet.observability.core

/**
 * Temperatura guardada sempre em graus Celsius.
 * Use as fábricas para não confundir a unidade de entrada.
 */
@JvmInline
value class Temperatura(val celsius: Double) {

    /** Valor equivalente em graus Fahrenheit. */
    val fahrenheit: Double
        get() = celsius * 9.0 / 5.0 + 32.0

    companion object {
        fun deCelsius(valor: Double): Temperatura = Temperatura(valor)

        fun deFahrenheit(valor: Double): Temperatura = Temperatura((valor - 32.0) * 5.0 / 9.0)

        /** Formato do Android (BatteryManager.EXTRA_TEMPERATURE): 345 significa 34,5 graus Celsius. */
        fun deDecimosDeCelsius(valor: Int): Temperatura = Temperatura(valor / 10.0)
    }
}
```

### StatusLeitura.kt
```kotlin
package dev.hornet.observability.core

/** Classificação de uma leitura de temperatura. */
enum class StatusLeitura {
    /** Ainda não há leituras suficientes na janela para comparar. */
    SEM_BASE,

    /** Dentro do comportamento recente. */
    NORMAL,

    /** Subida brusca em relação à janela (z-score acima de k). */
    PICO,

    /** Temperatura maior ou igual ao limiar absoluto. */
    LIMIAR_EXCEDIDO,

    /** Valor impossível para a bateria (NaN, infinito ou fora da faixa). Não é anomalia. */
    INVALIDA,
}
```

### Avaliacao.kt
```kotlin
package dev.hornet.observability.core

/**
 * Resultado da avaliação de uma leitura.
 * media, desvio e zScore só são preenchidos quando o status é NORMAL ou PICO.
 * desvio é o desvio efetivo usado no cálculo (já com o piso aplicado).
 */
data class Avaliacao(
    val temperatura: Temperatura,
    val status: StatusLeitura,
    val media: Double? = null,
    val desvio: Double? = null,
    val zScore: Double? = null,
) {
    /** Verdadeiro para PICO e LIMIAR_EXCEDIDO. */
    val anomala: Boolean
        get() = status == StatusLeitura.PICO || status == StatusLeitura.LIMIAR_EXCEDIDO
}
```

### ConfiguracaoDetector.kt
```kotlin
package dev.hornet.observability.core

/** Parâmetros do detector. Os valores padrão são os do escopo (docs/escopo.md). */
data class ConfiguracaoDetector(
    val janela: Int = 10,
    val k: Double = 3.0,
    val pisoDesvioCelsius: Double = 0.1,
    val limiarCelsius: Double = 45.0,
    val faixaMinimaCelsius: Double = 0.0,
    val faixaMaximaCelsius: Double = 60.0,
) {
    init {
        require(janela >= 2) { "A janela precisa ter pelo menos 2 leituras" }
        require(k > 0.0) { "k precisa ser positivo" }
        require(pisoDesvioCelsius > 0.0) { "O piso do desvio precisa ser positivo" }
        require(faixaMinimaCelsius < limiarCelsius) { "O limiar precisa ser maior que o mínimo da faixa" }
        require(limiarCelsius <= faixaMaximaCelsius) { "O limiar não pode passar do máximo da faixa" }
    }
}
```

### AnomalyDetector.kt
```kotlin
package dev.hornet.observability.core

/** Porta de entrada: avalia leituras de temperatura, uma por vez. Guarda estado (a janela). */
interface AnomalyDetector {
    /** Classifica a leitura e atualiza o estado interno conforme as regras do detector. */
    fun avaliar(temperatura: Temperatura): Avaliacao

    /** Esquece todas as leituras anteriores. A próxima leitura volta a ser SEM_BASE. */
    fun reiniciar()
}
```

### TemperatureSource.kt
```kotlin
package dev.hornet.observability.core

/** Porta de saída: de onde vêm as leituras. No app, é implementada com o BatteryManager. */
interface TemperatureSource {
    /** Leitura atual, ou null se estiver indisponível. */
    fun lerAtual(): Temperatura?
}
```

## 2. Regras do BaselineDetector

Implementa `AnomalyDetector`, recebe um `ConfiguracaoDetector` (padrão: `ConfiguracaoDetector()`). Mantém uma janela das últimas `janela` leituras aceitas. Para cada chamada de `avaliar(x)`, na ordem:

1. Se `x` não é finito (NaN ou infinito) ou está fora de `[faixaMinimaCelsius, faixaMaximaCelsius]`: status INVALIDA. Não entra na janela.
2. Se `x >= limiarCelsius`: status LIMIAR_EXCEDIDO. Vale mesmo com a janela vazia. Não entra na janela.
3. Se a janela tem menos de `janela` leituras: status SEM_BASE. A leitura entra na janela.
4. Com a janela cheia: `media` = média da janela; `sigma` = desvio padrão populacional da janela (divide por N); `desvio` = max(`sigma`, `pisoDesvioCelsius`); `z` = (`x` - `media`) / `desvio`.
   - Se `z > k` (estritamente maior): status PICO. Não entra na janela.
   - Caso contrário: status NORMAL. Entra na janela e a leitura mais antiga sai.
5. `Avaliacao.media`, `desvio` e `zScore` só são preenchidos em NORMAL e PICO. Nos demais status ficam nulos. O `desvio` informado é o efetivo, depois do piso.
6. Só subidas contam: uma queda brusca dá z negativo e é NORMAL.
7. `reiniciar()` esvazia a janela.

Observação: cuidado com comparações de Double nas fronteiras. Nos testes, use os vetores abaixo, que foram escolhidos para dar resultados exatos.

## 3. Vetores de teste (configuração padrão)

| ID | Entrada | Esperado |
|---|---|---|
| V-01 | 10 leituras de 30,0 e depois mais uma de 30,0 | As 10 primeiras são SEM_BASE. A 11ª é NORMAL, com media 30,0, desvio 0,1 e z 0,0. |
| V-02 | Após V-01: 36,0 e depois 30,0 | 36,0 é PICO (z = 60). Em seguida 30,0 é NORMAL, porque o pico não contaminou a janela. |
| V-03 | Após V-01: 30,2 | NORMAL (z = 2, por causa do piso de 0,1). |
| V-04 | Após V-01: 30,5 | PICO (z = 5). |
| V-05 | Janela com 30,0 e 31,0 alternados (10 leituras, media 30,5 e sigma 0,5), depois 32,0 | NORMAL, porque z = 3,0 não é maior que k. |
| V-06 | Mesma janela de V-05, depois 32,1 | PICO (z de aproximadamente 3,2). |
| V-07 | Primeira leitura 45,0 | LIMIAR_EXCEDIDO, mesmo sem base. |
| V-08 | Primeira leitura 44,9 | SEM_BASE. |
| V-09 | 60,0 e depois 60,1 | 60,0 é LIMIAR_EXCEDIDO. 60,1 é INVALIDA. |
| V-10 | 0,0 e depois -0,1 | 0,0 é SEM_BASE (primeira leitura). -0,1 é INVALIDA. |
| V-11 | NaN, infinito positivo, infinito negativo | INVALIDA nos três. |
| V-12 | Após V-01: 24,0 | NORMAL (queda brusca não é anomalia). |
| V-13 | Após V-01: `reiniciar()` e depois 30,0 | SEM_BASE. |
| V-14 | Após V-01: 36,0 (PICO), 45,0 (LIMIAR), NaN (INVALIDA) e depois 30,0 | A última é NORMAL com media 30,0 e desvio 0,1: nenhuma das anteriores entrou na janela. |

Conversões (testes de Temperatura): 95 °F = 35 °C; 113 °F = 45 °C; 32 °F = 0 °C; 212 °F = 100 °C; 345 décimos = 34,5 °C; 450 décimos = 45,0 °C; 35 °C = 95 °F. Compare Doubles com tolerância (por exemplo 1e-9).

## 4. Série sintética (testes de avaliação)

- Semente 42 (`java.util.Random(42)`), 200 leituras.
- Base 30,0 °C mais ruído gaussiano com desvio 0,3, arredondado a 0,1 (`Math.round(x * 10) / 10.0`), como a resolução do sensor.
- Picos injetados nos índices 40, 80, 120, 150 e 180: a leitura vira base + 6,0 + ruído (por volta de 36 °C, abaixo do limiar de 45 °C).
- Rótulos: os índices dos picos. Meta: pelo menos 90% desses índices com status PICO, e no máximo 5% de PICO ou LIMIAR_EXCEDIDO entre as leituras não injetadas com índice 10 ou maior.

Valor de referência: uma implementação independente das regras da seção 2, rodando esta série, deu recall de 100% (5 de 5 picos) e 5 falsos positivos em 185 leituras (2,7%). Se o resultado do detector real for muito diferente, revise o detector antes de pensar em mexer na meta.

## 5. Como o app usa o contrato

O `TemperaturaViewModel` recebe um `TemperatureSource` e um `AnomalyDetector` no construtor. A cada 5 segundos chama `fonte.lerAtual()`, passa o resultado a `detector.avaliar(...)` e guarda as últimas 60 `Avaliacao` no estado da tela. O botão "Simular pico" chama `detector.avaliar(Temperatura.deCelsius(ultimaLeituraNormal + 6.0))`.

## 6. Contrato v2: telemetria e classificador de throttling

A v2 acrescenta ao `core` os sinais que a temperatura sozinha não mostra: frequência da CPU, uso da CPU e status térmico do Android. O contrato da v1 (seções 1 a 5) não muda: `Temperatura`, `Avaliacao`, `AnomalyDetector`, `BaselineDetector` e `TemperatureSource` continuam como estão. Os arquivos novos ficam em `core/src/main/kotlin/dev/hornet/observability/core/` e o `core` continua sem depender do Android. As regras de decisão estão em [classificador-throttling.md](classificador-throttling.md).

### AmostraTelemetria.kt
```kotlin
data class AmostraTelemetria(
    val instanteMs: Long,
    val temperaturaBateria: Temperatura,
    val frequenciaAtualKhz: Long? = null,   // scaling_cur_freq
    val frequenciaMaximaKhz: Long? = null,  // cpuinfo_max_freq (máximo de fábrica)
    val tetoFrequenciaKhz: Long? = null,    // scaling_max_freq (teto imposto pelo sistema)
    val usoCpuPercentual: Double? = null,   // 0 a 100
    val statusTermico: Int? = null,         // PowerManager: 0 (NONE) a 6 (SHUTDOWN)
) {
    fun razaoFrequencia(): Double?   // atual / máximo
    fun razaoTeto(): Double?         // teto / máximo
    fun usoCpuValido(): Double?
    fun statusTermicoValido(): Int?
}
```

### EstadoThrottling.kt, ResultadoThrottling.kt, TelemetriaSource.kt e ThrottlingClassifier.kt
```kotlin
enum class EstadoThrottling { INDETERMINADO, NORMAL, AQUECENDO, THROTTLING_PROVAVEL, THROTTLING_CONFIRMADO }

data class ResultadoThrottling(val estado: EstadoThrottling, val motivo: String)

interface TelemetriaSource {
    /** Amostra atual, ou null se a temperatura da bateria estiver indisponível. */
    fun lerAtual(): AmostraTelemetria?
}

interface ThrottlingClassifier {
    fun classificar(amostra: AmostraTelemetria, avaliacaoTemperatura: Avaliacao): ResultadoThrottling
}
```
`TelemetriaSource.lerAtual()` segue o mesmo padrão de `TemperatureSource.lerAtual()`. A porta de entrada `ThrottlingClassifier` não guarda estado: cada chamada classifica uma amostra, usando a `Avaliacao` que o `AnomalyDetector` produziu para a temperatura dessa mesma amostra. A implementação `ClassificadorThrottling` e a `ConfiguracaoThrottling` ficam com o Bruno (issue B2).

### Regras dos campos
- **Sinal ausente:** campo `null`. O classificador ignora a regra que depende dele e nunca lança exceção. Só a temperatura é obrigatória.
- **Frequências em kHz.** Uma frequência nula, zero ou negativa conta como ausente. Sem a frequência máxima, as duas razões ficam ausentes. Se faltar apenas a frequência atual ou o teto, a outra razão ainda é calculada.
- **Razões:** divisão em ponto flutuante, sem arredondar e sem limitar a 1. Um teto maior que o máximo dá razão acima de 1. Quem decide o que é "teto imposto" é o classificador, com `razaoTetoImposto`, e não o contrato.
- **Uso da CPU:** `usoCpuValido()` devolve o valor só se for finito e estiver de 0 a 100, inclusive. 0% é válido. NaN, infinito e valores fora da faixa contam como ausentes.
- **Status térmico:** `statusTermicoValido()` devolve o código só se estiver de 0 a 6, inclusive. O código 0 é válido. Fora da faixa conta como ausente.

### Núcleos com máximos diferentes
Em CPUs com grupos de núcleos de máximos diferentes, `frequenciaAtualKhz`, `frequenciaMaximaKhz` e `tetoFrequenciaKhz` pertencem ao mesmo grupo (`policy`): o adaptador escolhe o grupo de maior razão atual/máximo e informa os três valores desse grupo. Assim as razões ficam coerentes. Exemplo do Moto G60 em `docs/validacao/frequencia-e-status-termico.md`: `policy0` com máximo de 1804,8 MHz e `policy6` com 2304,0 MHz.

### Origem do uso da CPU
`usoCpuPercentual` é o uso do sistema quando o aparelho permite lê-lo. Em aplicativos comuns, `/proc/stat` e `/proc/loadavg` falharam com `EACCES` na validação (`docs/validacao/uso-cpu.md`), então o plano B usa o uso do próprio processo, medido com `Process.getElapsedCpuTime()`. Esse valor só reflete a carga gerada pelo próprio app (por exemplo, o botão de gerar carga) e não a carga de outros aplicativos. O adaptador devolve `null` na primeira leitura, porque o cálculo precisa de duas amostras.

### Como o app usa o contrato v2
Um adaptador `TelemetriaSource` monta a `AmostraTelemetria` a partir da bateria, do `cpufreq`, do uso da CPU e do `PowerManager`. A cada 5 segundos, o ViewModel chama `lerAtual()`, avalia a temperatura com o `AnomalyDetector`, passa a amostra e a `Avaliacao` ao `ThrottlingClassifier` e publica o resultado para a tela.

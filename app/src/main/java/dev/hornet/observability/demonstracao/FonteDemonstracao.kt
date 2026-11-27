package dev.hornet.observability.demonstracao

import dev.hornet.observability.core.AmostraTelemetria
import dev.hornet.observability.core.Temperatura
import dev.hornet.observability.core.TelemetriaSource

/**
 * Fonte de telemetria roteirizada para apresentações: dados simulados, não lidos do aparelho.
 *
 * Entrega as fases abaixo, em ordem, e depois recomeça. Com os parâmetros padrão do
 * ClassificadorThrottling, cada fase produz um estado diferente:
 * 1. Repouso: bateria fria e CPU folgada. Estado NORMAL.
 * 2. Aquecendo: bateria a partir de 38 °C, sem limitação. Estado AQUECENDO.
 * 3. Teto imposto: o teto cai para 85% do máximo com a bateria abaixo de 40 °C. Estado THROTTLING_PROVAVEL.
 * 4. Status térmico moderado: o Android reporta nível 2. Estado THROTTLING_CONFIRMADO.
 */
class FonteDemonstracao(
    private val lerInstanteMs: () -> Long = { System.currentTimeMillis() },
) : TelemetriaSource {

    private var indice = 0

    override fun lerAtual(): AmostraTelemetria {
        val passo = ROTEIRO[indice]
        indice = (indice + 1) % ROTEIRO.size
        return AmostraTelemetria(
            instanteMs = lerInstanteMs(),
            temperaturaBateria = Temperatura.deCelsius(passo.celsius),
            frequenciaAtualKhz = passo.frequenciaAtualKhz,
            frequenciaMaximaKhz = FREQUENCIA_MAXIMA_KHZ,
            tetoFrequenciaKhz = passo.tetoFrequenciaKhz,
            usoCpuPercentual = passo.usoCpuPercentual,
            statusTermico = passo.statusTermico,
        )
    }

    /** Um passo do roteiro: os sinais de uma amostra. */
    private data class Passo(
        val celsius: Double,
        val frequenciaAtualKhz: Long,
        val tetoFrequenciaKhz: Long,
        val usoCpuPercentual: Double,
        val statusTermico: Int,
    )

    companion object {
        /** Quantidade de amostras de cada fase. A 5 s por leitura, cada fase dura 15 s. */
        const val AMOSTRAS_POR_FASE = 3

        /** Total de amostras antes de o roteiro recomeçar. */
        val TAMANHO_ROTEIRO: Int
            get() = ROTEIRO.size

        private const val FREQUENCIA_MAXIMA_KHZ = 2_000_000L

        private val ROTEIRO = listOf(
            // Fase 1: repouso.
            Passo(31.0, 900_000L, 2_000_000L, 15.0, 0),
            Passo(31.1, 950_000L, 2_000_000L, 18.0, 0),
            Passo(31.0, 900_000L, 2_000_000L, 16.0, 0),
            // Fase 2: aquecendo, ainda sem teto reduzido e com uso abaixo de 70%.
            Passo(38.2, 1_800_000L, 2_000_000L, 60.0, 0),
            Passo(38.6, 1_800_000L, 2_000_000L, 62.0, 0),
            Passo(39.0, 1_750_000L, 2_000_000L, 64.0, 1),
            // Fase 3: teto imposto (85% do máximo) com a bateria entre 38 °C e 40 °C.
            Passo(39.2, 1_700_000L, 1_700_000L, 80.0, 1),
            Passo(39.5, 1_650_000L, 1_700_000L, 82.0, 1),
            Passo(39.8, 1_600_000L, 1_700_000L, 85.0, 1),
            // Fase 4: o Android reporta status térmico moderado (2).
            Passo(40.5, 1_300_000L, 1_500_000L, 88.0, 2),
            Passo(41.0, 1_200_000L, 1_500_000L, 90.0, 2),
            Passo(41.2, 1_200_000L, 1_500_000L, 90.0, 2),
        )
    }
}

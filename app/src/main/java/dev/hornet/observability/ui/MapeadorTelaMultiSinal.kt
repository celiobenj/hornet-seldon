package dev.hornet.observability.ui

import dev.hornet.observability.core.AmostraTelemetria
import dev.hornet.observability.core.EstadoThrottling
import dev.hornet.observability.core.ResultadoThrottling

/**
 * Converte o que o ViewModel publica (tipos do core) no modelo da tela multi-sinal.
 *
 * Sinal ausente ou inválido vira null, e a tela mostra "indisponível". Nunca vira zero.
 */
fun mapearParaTela(estado: TelemetriaUiState): EstadoTelaMultiSinal {
    val amostra = estado.amostra
    val resultado = estado.resultado
    if (amostra == null || resultado == null) {
        return EstadoTelaMultiSinal(
            estado = EstadoThrottlingTela.DADOS_INSUFICIENTES,
            motivo = if (estado.indisponivel) {
                "Temperatura da bateria indisponível."
            } else {
                "Aguardando a primeira leitura."
            },
            simulacao = estado.simulacao,
        )
    }

    return EstadoTelaMultiSinal(
        temperaturaTexto = textoTemperatura(amostra),
        frequenciaAtualMhz = khzParaMhz(amostra.frequenciaAtualKhz),
        frequenciaMaximaMhz = khzParaMhz(amostra.frequenciaMaximaKhz),
        razaoFrequencia = amostra.razaoFrequencia()?.takeIf { it.isFinite() },
        usoCpuPercentual = amostra.usoCpuValido(),
        statusTermico = amostra.statusTermicoValido()?.let(::nomeStatusTermico),
        estado = estadoDaTela(resultado.estado),
        motivo = resultado.motivo,
        simulacao = estado.simulacao,
    )
}

/** Converte o estado do core no enum da tela. INDETERMINADO vira "Dados insuficientes". */
fun estadoDaTela(estado: EstadoThrottling): EstadoThrottlingTela = when (estado) {
    EstadoThrottling.INDETERMINADO -> EstadoThrottlingTela.DADOS_INSUFICIENTES
    EstadoThrottling.NORMAL -> EstadoThrottlingTela.NORMAL
    EstadoThrottling.AQUECENDO -> EstadoThrottlingTela.AQUECENDO
    EstadoThrottling.THROTTLING_PROVAVEL -> EstadoThrottlingTela.THROTTLING_PROVAVEL
    EstadoThrottling.THROTTLING_CONFIRMADO -> EstadoThrottlingTela.THROTTLING_CONFIRMADO
}

/** Nome do nível térmico do PowerManager, com o código entre parênteses. Ex.: "Moderado (2)". */
fun nomeStatusTermico(codigo: Int): String {
    val nome = when (codigo) {
        0 -> "Nenhum"
        1 -> "Leve"
        2 -> "Moderado"
        3 -> "Severo"
        4 -> "Crítico"
        5 -> "Emergência"
        6 -> "Desligamento"
        else -> "Desconhecido"
    }
    return "$nome ($codigo)"
}

/** Frequência nula, zero ou negativa conta como ausente, como no contrato v2. */
fun khzParaMhz(khz: Long?): Long? = khz?.takeIf { it > 0L }?.let { it / KHZ_POR_MHZ }

private fun textoTemperatura(amostra: AmostraTelemetria): String? =
    amostra.temperaturaBateria.celsius
        .takeIf { it.isFinite() }
        ?.let { "${"%.1f".format(it)} °C" }

private const val KHZ_POR_MHZ = 1_000L

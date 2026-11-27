package dev.hornet.observability.core

import java.math.BigDecimal

class ClassificadorThrottling(
    private val configuracao: ConfiguracaoThrottling = ConfiguracaoThrottling(),
) : ThrottlingClassifier {

    override fun classificar(
        amostra: AmostraTelemetria,
        avaliacaoTemperatura: Avaliacao,
    ): ResultadoThrottling {
        val statusTemperatura = avaliacaoTemperatura.status
        if (statusTemperatura == StatusLeitura.INVALIDA) {
            return ResultadoThrottling(
                EstadoThrottling.INDETERMINADO,
                "Temperatura da bateria inválida.",
            )
        }

        val statusTermico = amostra.statusTermicoValido()
        if (statusTermico != null && statusTermico >= configuracao.statusTermicoMinimo) {
            return ResultadoThrottling(
                EstadoThrottling.THROTTLING_CONFIRMADO,
                "O Android reportou nível térmico $statusTermico.",
            )
        }

        val celsius = amostra.temperaturaBateria.celsius
        val emAquecimento = celsius >= configuracao.temperaturaMinimaAquecimentoCelsius
        val razaoTeto = amostra.razaoTeto()
        if (emAquecimento && razaoTeto != null && razaoTeto < configuracao.razaoTetoImposto) {
            val estado = if (celsius >= configuracao.temperaturaMinimaConfirmacaoCelsius) {
                EstadoThrottling.THROTTLING_CONFIRMADO
            } else {
                EstadoThrottling.THROTTLING_PROVAVEL
            }
            return ResultadoThrottling(
                estado,
                "Teto de frequência imposto a ${formatarRazao(razaoTeto)} do máximo " +
                    "e bateria a ${formatarDecimal(celsius)} °C.",
            )
        }

        val razaoFrequencia = amostra.razaoFrequencia()
        val usoCpu = amostra.usoCpuValido()
        if (
            emAquecimento &&
            razaoFrequencia != null && razaoFrequencia < configuracao.razaoFrequenciaBaixa &&
            usoCpu != null && usoCpu >= configuracao.usoCpuAltoPercentual
        ) {
            return ResultadoThrottling(
                EstadoThrottling.THROTTLING_PROVAVEL,
                "CPU a ${formatarRazao(razaoFrequencia)} da frequência máxima, " +
                    "com ${formatarDecimal(usoCpu)}% de uso e bateria a ${formatarDecimal(celsius)} °C.",
            )
        }

        val motivoAquecimento = when {
            statusTemperatura == StatusLeitura.PICO ->
                "Pico de temperatura da bateria a ${formatarDecimal(celsius)} °C."
            statusTemperatura == StatusLeitura.LIMIAR_EXCEDIDO ->
                "A bateria atingiu o limiar do detector a ${formatarDecimal(celsius)} °C."
            emAquecimento -> "A bateria atingiu a faixa de aquecimento a ${formatarDecimal(celsius)} °C."
            else -> null
        }

        return if (motivoAquecimento != null) {
            ResultadoThrottling(EstadoThrottling.AQUECENDO, motivoAquecimento)
        } else {
            ResultadoThrottling(EstadoThrottling.NORMAL, "Os sinais disponíveis são compatíveis com operação normal.")
        }
    }

    private fun formatarDecimal(valor: Double): String = valor.toString().replace('.', ',')

    private fun formatarRazao(razao: Double): String =
        BigDecimal.valueOf(razao).movePointRight(2).stripTrailingZeros().toPlainString().replace('.', ',') + "%"
}

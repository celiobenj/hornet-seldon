package dev.hornet.observability.core

/** Limiares do classificador definidos em docs/classificador-throttling.md. */
data class ConfiguracaoThrottling(
    val temperaturaMinimaAquecimentoCelsius: Double = 38.0,
    val temperaturaMinimaConfirmacaoCelsius: Double = 40.0,
    val razaoTetoImposto: Double = 0.90,
    val razaoFrequenciaBaixa: Double = 0.70,
    val usoCpuAltoPercentual: Double = 70.0,
    val statusTermicoMinimo: Int = 2,
) {
    init {
        require(temperaturaMinimaAquecimentoCelsius.isFinite() && temperaturaMinimaAquecimentoCelsius > 0.0) {
            "A temperatura mínima de aquecimento precisa ser finita e positiva"
        }
        require(
            temperaturaMinimaConfirmacaoCelsius.isFinite() &&
                temperaturaMinimaConfirmacaoCelsius >= temperaturaMinimaAquecimentoCelsius,
        ) {
            "A temperatura mínima de confirmação precisa ser finita e pelo menos a de aquecimento"
        }
        require(razaoTetoImposto > 0.0 && razaoTetoImposto <= 1.0) {
            "A razão do teto imposto precisa ser maior que 0 e no máximo 1"
        }
        require(razaoFrequenciaBaixa > 0.0 && razaoFrequenciaBaixa <= 1.0) {
            "A razão de frequência baixa precisa ser maior que 0 e no máximo 1"
        }
        require(usoCpuAltoPercentual > 0.0 && usoCpuAltoPercentual <= 100.0) {
            "O percentual de uso alto da CPU precisa ser maior que 0 e no máximo 100"
        }
        require(statusTermicoMinimo in 1..AmostraTelemetria.STATUS_TERMICO_MAXIMO) {
            "O status térmico mínimo precisa estar entre 1 e 6"
        }
    }
}

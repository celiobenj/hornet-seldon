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

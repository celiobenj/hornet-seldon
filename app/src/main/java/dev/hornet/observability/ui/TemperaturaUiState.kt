package dev.hornet.observability.ui

import dev.hornet.observability.core.Avaliacao

data class TemperaturaUiState(
    val atual: Avaliacao? = null,
    val historico: List<Avaliacao> = emptyList(),
    val limiarCelsius: Double = 45.0,
    val indisponivel: Boolean = false,
)

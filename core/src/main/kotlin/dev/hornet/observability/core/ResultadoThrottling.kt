package dev.hornet.observability.core

/** Resultado da classificação: o estado e um motivo em português que identifica a regra aplicada. */
data class ResultadoThrottling(
    val estado: EstadoThrottling,
    val motivo: String,
)

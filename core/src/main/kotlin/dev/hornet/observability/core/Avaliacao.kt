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

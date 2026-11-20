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

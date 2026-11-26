package dev.hornet.observability.core

/** Estado do aparelho quanto ao thermal throttling (redução de desempenho por calor). */
enum class EstadoThrottling {
    /** Não há temperatura válida para classificar. */
    INDETERMINADO,

    /** Nenhuma evidência de aquecimento nem de limitação de frequência. */
    NORMAL,

    /** A temperatura subiu de forma anormal ou atingiu o limiar de aquecimento. */
    AQUECENDO,

    /** Há indícios combinados de limitação de frequência e aquecimento, sem confirmação do sistema. */
    THROTTLING_PROVAVEL,

    /** O status térmico do Android ou um teto de frequência com bateria quente confirmam a limitação. */
    THROTTLING_CONFIRMADO,
}

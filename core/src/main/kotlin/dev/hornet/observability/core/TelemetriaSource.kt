package dev.hornet.observability.core

/** Porta de saída da v2: de onde vêm as amostras de telemetria. No app, é implementada com o Android. */
interface TelemetriaSource {
    /** Amostra atual, ou null se a temperatura da bateria estiver indisponível. */
    fun lerAtual(): AmostraTelemetria?
}

package dev.hornet.observability.core

/** Porta de saída: de onde vêm as leituras. No app, é implementada com o BatteryManager. */
interface TemperatureSource {
    /** Leitura atual, ou null se estiver indisponível. */
    fun lerAtual(): Temperatura?
}

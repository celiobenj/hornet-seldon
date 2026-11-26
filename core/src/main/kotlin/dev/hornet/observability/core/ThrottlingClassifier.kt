package dev.hornet.observability.core

/** Porta de entrada da v2: classifica uma amostra, uma por vez, sem guardar estado. */
interface ThrottlingClassifier {
    /**
     * Classifica a amostra usando a Avaliacao que o AnomalyDetector produziu para a temperatura
     * da mesma amostra. Sinal ausente nunca causa exceção: a regra que depende dele é ignorada.
     */
    fun classificar(
        amostra: AmostraTelemetria,
        avaliacaoTemperatura: Avaliacao,
    ): ResultadoThrottling
}

package dev.hornet.observability.core

/**
 * Todos os sinais lidos no mesmo instante (contrato v2).
 *
 * Sinal que o aparelho não fornece fica null. O classificador ignora as regras que dependem dele.
 *
 * frequenciaAtualKhz, frequenciaMaximaKhz e tetoFrequenciaKhz pertencem ao mesmo grupo de núcleos
 * (policy). O adaptador escolhe o grupo de maior razão atual/máximo, para que as razões fiquem
 * coerentes em CPUs com núcleos de máximos diferentes.
 *
 * usoCpuPercentual é o uso do sistema quando o aparelho permite lê-lo. Quando não permite, é o uso
 * do próprio processo (plano B de docs/validacao/uso-cpu.md), que só reflete a carga gerada pelo app.
 *
 * statusTermico segue as constantes do PowerManager: 0 (NONE) até 6 (SHUTDOWN).
 */
data class AmostraTelemetria(
    val instanteMs: Long,
    val temperaturaBateria: Temperatura,
    val frequenciaAtualKhz: Long? = null,
    val frequenciaMaximaKhz: Long? = null,
    val tetoFrequenciaKhz: Long? = null,
    val usoCpuPercentual: Double? = null,
    val statusTermico: Int? = null,
) {

    /** Frequência atual dividida pelo máximo, sem arredondar e sem limitar a 1. Null se faltar dado. */
    fun razaoFrequencia(): Double? = razaoSobreMaximo(frequenciaAtualKhz)

    /** Teto de frequência dividido pelo máximo, sem arredondar e sem limitar a 1. Null se faltar dado. */
    fun razaoTeto(): Double? = razaoSobreMaximo(tetoFrequenciaKhz)

    /** Uso da CPU de 0 a 100 (inclusive), ou null se ausente, NaN, infinito ou fora da faixa. */
    fun usoCpuValido(): Double? =
        usoCpuPercentual?.takeIf { it.isFinite() && it >= 0.0 && it <= 100.0 }

    /** Status térmico de 0 a 6 (inclusive), ou null se ausente ou fora da faixa. */
    fun statusTermicoValido(): Int? =
        statusTermico?.takeIf { it in STATUS_TERMICO_MINIMO..STATUS_TERMICO_MAXIMO }

    /** Frequência nula, zero ou negativa conta como ausente, inclusive o máximo. */
    private fun razaoSobreMaximo(numeradorKhz: Long?): Double? {
        val maximoKhz = frequenciaMaximaKhz
        if (numeradorKhz == null || maximoKhz == null) return null
        if (numeradorKhz <= 0L || maximoKhz <= 0L) return null
        return numeradorKhz.toDouble() / maximoKhz.toDouble()
    }

    companion object {
        const val STATUS_TERMICO_MINIMO = 0
        const val STATUS_TERMICO_MAXIMO = 6
    }
}

package dev.hornet.observability.data

import android.os.Process
import android.os.SystemClock

/**
 * Calcula o uso percentual de CPU do proprio processo entre duas leituras (Plano B).
 *
 * Formula validada em docs/validacao/uso-cpu.md:
 * deltaCpu / (deltaTempo * processadores) * 100.
 * Retorna null quando algum delta ou a quantidade de processadores for invalida.
 */
fun calcularUsoCpuProcessoPercentual(
    deltaCpuMs: Long,
    deltaTempoMs: Long,
    processadores: Int,
): Double? {
    if (deltaCpuMs < 0L || deltaTempoMs <= 0L || processadores <= 0) {
        return null
    }
    val capacidadeTotalMs = deltaTempoMs.toDouble() * processadores.toDouble()
    if (!capacidadeTotalMs.isFinite() || capacidadeTotalMs <= 0.0) {
        return null
    }
    val percentual = (deltaCpuMs.toDouble() / capacidadeTotalMs) * 100.0
    if (!percentual.isFinite()) {
        return null
    }
    return percentual.coerceIn(0.0, 100.0)
}

/**
 * Adaptador Android para estimar o uso da CPU seguindo a decisao "go com plano B"
 * registrada em docs/validacao/uso-cpu.md.
 *
 * Como /proc/stat e /proc/loadavg falham com EACCES em aplicativos comuns sem root,
 * este adaptador mede apenas o uso de CPU do proprio processo do app usando
 * Process.getElapsedCpuTime(), SystemClock.elapsedRealtime() e availableProcessors().
 * A primeira leitura apenas registra a referencia inicial e devolve null.
 * Falhas de leitura tambem devolvem null, sem lancar excecao.
 */
class UsoCpuSource(
    private val lerTempoCpuProcessoMs: () -> Long = { Process.getElapsedCpuTime() },
    private val lerTempoDecorridoMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val lerQuantidadeProcessadores: () -> Int = { Runtime.getRuntime().availableProcessors() },
) {

    /**
     * Deixa explicito que a metrica segue o Plano B (docs/validacao/uso-cpu.md):
     * reflete apenas a carga do proprio processo do aplicativo, nao do sistema inteiro.
     */
    val medeSomenteProprioProcesso: Boolean = true

    private var ultimoTempoCpuProcessoMs: Long? = null
    private var ultimoTempoDecorridoMs: Long? = null

    /**
     * Retorna o uso percentual de CPU do proprio processo (0.0 a 100.0) desde a leitura anterior.
     * Na primeira leitura valida, grava a referencia e retorna null.
     */
    @Synchronized
    fun lerAtual(): Double? {
        return try {
            val tempoCpuAtualMs = lerTempoCpuProcessoMs()
            val tempoDecorridoAtualMs = lerTempoDecorridoMs()
            if (tempoCpuAtualMs < 0L || tempoDecorridoAtualMs < 0L) {
                return null
            }

            val tempoCpuAnteriorMs = ultimoTempoCpuProcessoMs
            val tempoDecorridoAnteriorMs = ultimoTempoDecorridoMs
            ultimoTempoCpuProcessoMs = tempoCpuAtualMs
            ultimoTempoDecorridoMs = tempoDecorridoAtualMs

            if (tempoCpuAnteriorMs == null || tempoDecorridoAnteriorMs == null) {
                return null
            }

            val processadores = lerQuantidadeProcessadores()
            calcularUsoCpuProcessoPercentual(
                deltaCpuMs = tempoCpuAtualMs - tempoCpuAnteriorMs,
                deltaTempoMs = tempoDecorridoAtualMs - tempoDecorridoAnteriorMs,
                processadores = processadores,
            )
        } catch (_: Exception) {
            null
        }
    }
}

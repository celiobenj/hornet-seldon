package dev.hornet.observability.carga

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Ocupa a CPU de propósito, em várias threads, por um tempo limitado (padrão: 60 segundos).
 *
 * Serve para provocar aquecimento de forma controlada e ver os sinais mudarem na tela.
 * Para sozinho no fim do tempo, com parar() ou quando o escopo (o ViewModel) é encerrado.
 */
class GeradorDeCarga(
    private val escopo: CoroutineScope,
    private val despachante: CoroutineDispatcher = Dispatchers.Default,
    private val quantidadeThreads: Int = Runtime.getRuntime().availableProcessors(),
    private val duracaoMs: Long = DURACAO_PADRAO_MS,
) {

    private val _ativo = MutableStateFlow(false)
    val ativo: StateFlow<Boolean> = _ativo.asStateFlow()

    @Volatile
    private var tarefa: Job? = null

    fun iniciar() {
        if (tarefa?.isActive == true) return

        val fimNs = System.nanoTime() + duracaoMs * NS_POR_MS
        val novaTarefa = escopo.launch(despachante, start = CoroutineStart.LAZY) {
            try {
                coroutineScope {
                    repeat(quantidadeThreads.coerceAtLeast(1)) {
                        launch { ocuparCpu(fimNs) }
                    }
                }
            } finally {
                // Só a tarefa atual desliga o indicador; uma tarefa antiga cancelada não interfere.
                if (tarefa === coroutineContext[Job]) _ativo.value = false
            }
        }
        tarefa = novaTarefa
        _ativo.value = true
        novaTarefa.start()
    }

    fun parar() {
        tarefa?.cancel()
        tarefa = null
        _ativo.value = false
    }

    /** Conta sem parar até o fim do tempo, conferindo de tempos em tempos se foi cancelado. */
    private suspend fun ocuparCpu(fimNs: Long) {
        var acumulado = 1.0
        while (System.nanoTime() < fimNs) {
            repeat(PASSOS_POR_VERIFICACAO) {
                acumulado = acumulado * 1.000_000_1 + 1.0
            }
            coroutineContext.ensureActive()
        }
        resultadoDescartado = acumulado
    }

    private companion object {
        const val DURACAO_PADRAO_MS = 60_000L
        const val NS_POR_MS = 1_000_000L
        const val PASSOS_POR_VERIFICACAO = 100_000

        // Guarda o resultado da conta para o compilador não eliminar o laço.
        @Volatile
        var resultadoDescartado = 0.0
    }
}

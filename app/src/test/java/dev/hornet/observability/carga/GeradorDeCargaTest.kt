package dev.hornet.observability.carga

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeradorDeCargaTest {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @After
    fun encerrar() {
        escopo.cancel()
    }

    @Test
    fun pararDesligaACargaAntesDoFimDoTempo() {
        val gerador = GeradorDeCarga(escopo, quantidadeThreads = 2, duracaoMs = 60_000L)

        gerador.iniciar()
        assertTrue(gerador.ativo.value)

        gerador.parar()
        assertFalse(gerador.ativo.value)
    }

    @Test
    fun cargaTerminaSozinhaNoFimDoTempo() {
        val gerador = GeradorDeCarga(escopo, quantidadeThreads = 2, duracaoMs = 100L)

        gerador.iniciar()

        assertTrue(esperarAte(limiteMs = 5_000L) { !gerador.ativo.value })
    }

    @Test
    fun podeGerarDeNovoDepoisDeParar() {
        val gerador = GeradorDeCarga(escopo, quantidadeThreads = 1, duracaoMs = 60_000L)

        gerador.iniciar()
        gerador.parar()
        gerador.iniciar()

        // A tarefa cancelada não pode desligar o indicador da tarefa nova.
        Thread.sleep(200L)
        assertTrue(gerador.ativo.value)
        gerador.parar()
    }

    private fun esperarAte(limiteMs: Long, condicao: () -> Boolean): Boolean {
        val fim = System.currentTimeMillis() + limiteMs
        while (System.currentTimeMillis() < fim) {
            if (condicao()) return true
            Thread.sleep(20L)
        }
        return condicao()
    }
}

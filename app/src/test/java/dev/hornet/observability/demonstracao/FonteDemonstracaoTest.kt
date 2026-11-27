package dev.hornet.observability.demonstracao

import dev.hornet.observability.core.AmostraTelemetria
import dev.hornet.observability.core.BaselineDetector
import dev.hornet.observability.core.ClassificadorThrottling
import dev.hornet.observability.core.EstadoThrottling
import dev.hornet.observability.core.Temperatura
import dev.hornet.observability.core.TelemetriaSource
import dev.hornet.observability.ui.EstadoThrottlingTela
import dev.hornet.observability.ui.TelemetriaViewModel
import dev.hornet.observability.ui.mapearParaTela
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** O roteiro passa pelo detector e pelo classificador reais, como no app. */
class FonteDemonstracaoTest {

    @Test
    fun roteiroMostraOsQuatroEstadosNaOrdem() {
        val estados = estadosDoRoteiro(FonteDemonstracao())

        assertEquals(
            listOf(
                EstadoThrottling.NORMAL,
                EstadoThrottling.AQUECENDO,
                EstadoThrottling.THROTTLING_PROVAVEL,
                EstadoThrottling.THROTTLING_CONFIRMADO,
            ),
            estados.semRepeticoesSeguidas(),
        )
    }

    @Test
    fun cadaFaseTemOMesmoEstadoEmTodasAsAmostras() {
        val fases = estadosDoRoteiro(FonteDemonstracao()).chunked(FonteDemonstracao.AMOSTRAS_POR_FASE)

        assertEquals(4, fases.size)
        fases.forEach { fase -> assertEquals(1, fase.toSet().size) }
    }

    @Test
    fun roteiroRecomecaDepoisDaUltimaAmostra() {
        val fonte = FonteDemonstracao()
        val primeira = fonte.lerAtual()
        repeat(FonteDemonstracao.TAMANHO_ROTEIRO - 1) { fonte.lerAtual() }

        val depoisDoFim = fonte.lerAtual()

        assertEquals(primeira.temperaturaBateria, depoisDoFim.temperaturaBateria)
        assertEquals(primeira.statusTermico, depoisDoFim.statusTermico)
    }

    @Test
    fun segundaVoltaRepeteAMesmaSequenciaDeEstados() {
        val viewModel = viewModelComDemonstracao()
        viewModel.iniciarDemonstracao()
        val estados = mutableListOf(viewModel.estado.value.resultado?.estado)
        repeat(FonteDemonstracao.TAMANHO_ROTEIRO * 2 - 1) {
            viewModel.amostrar()
            estados += viewModel.estado.value.resultado?.estado
        }

        val primeiraVolta = estados.take(FonteDemonstracao.TAMANHO_ROTEIRO)
        val segundaVolta = estados.drop(FonteDemonstracao.TAMANHO_ROTEIRO)
        assertEquals(primeiraVolta, segundaVolta)
    }

    @Test
    fun telaMostraSimulacaoEOsEstadosDaTelaNaOrdem() {
        val viewModel = viewModelComDemonstracao()

        viewModel.iniciarDemonstracao()
        val telas = mutableListOf(mapearParaTela(viewModel.estado.value))
        repeat(FonteDemonstracao.TAMANHO_ROTEIRO - 1) {
            viewModel.amostrar()
            telas += mapearParaTela(viewModel.estado.value)
        }

        assertTrue(telas.all { it.simulacao })
        assertEquals(
            listOf(
                EstadoThrottlingTela.NORMAL,
                EstadoThrottlingTela.AQUECENDO,
                EstadoThrottlingTela.THROTTLING_PROVAVEL,
                EstadoThrottlingTela.THROTTLING_CONFIRMADO,
            ),
            telas.map { it.estado }.semRepeticoesSeguidas(),
        )
    }

    @Test
    fun sairDaDemonstracaoVoltaAFonteRealSemDadosSimulados() {
        val viewModel = viewModelComDemonstracao()
        viewModel.iniciarDemonstracao()
        repeat(5) { viewModel.amostrar() }

        viewModel.pararDemonstracao()
        assertFalse(viewModel.estado.value.simulacao)
        assertTrue(viewModel.estado.value.historico.isEmpty())

        viewModel.amostrar()
        assertEquals(30.0, viewModel.estado.value.amostra?.temperaturaBateria?.celsius ?: 0.0, 0.0001)
        assertEquals(1, viewModel.estado.value.historico.size)
    }

    @Test
    fun entrarNaDemonstracaoDescartaOHistoricoReal() {
        val viewModel = viewModelComDemonstracao()
        repeat(3) { viewModel.amostrar() }

        viewModel.iniciarDemonstracao()

        assertTrue(viewModel.estado.value.simulacao)
        assertEquals(1, viewModel.estado.value.historico.size)
        assertEquals(31.0, viewModel.estado.value.amostra?.temperaturaBateria?.celsius ?: 0.0, 0.0001)
    }

    private fun estadosDoRoteiro(fonte: FonteDemonstracao): List<EstadoThrottling> {
        val detector = BaselineDetector()
        val classificador = ClassificadorThrottling()
        return List(FonteDemonstracao.TAMANHO_ROTEIRO) {
            val amostra = fonte.lerAtual()
            classificador.classificar(amostra, detector.avaliar(amostra.temperaturaBateria)).estado
        }
    }

    private fun viewModelComDemonstracao() = TelemetriaViewModel(
        fonte = FonteRealFalsa,
        detector = BaselineDetector(),
        classificador = ClassificadorThrottling(),
    )

    private fun <T> List<T>.semRepeticoesSeguidas(): List<T> =
        filterIndexed { indice, valor -> indice == 0 || this[indice - 1] != valor }

    /** Fonte real falsa: sempre 30 °C, para distinguir dos dados da demonstração. */
    private object FonteRealFalsa : TelemetriaSource {
        override fun lerAtual(): AmostraTelemetria = AmostraTelemetria(
            instanteMs = 0L,
            temperaturaBateria = Temperatura.deCelsius(30.0),
        )
    }
}

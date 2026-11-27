package dev.hornet.observability.ui

import dev.hornet.observability.core.AmostraTelemetria
import dev.hornet.observability.core.AnomalyDetector
import dev.hornet.observability.core.Avaliacao
import dev.hornet.observability.core.EstadoThrottling
import dev.hornet.observability.core.ResultadoThrottling
import dev.hornet.observability.core.StatusLeitura
import dev.hornet.observability.core.Temperatura
import dev.hornet.observability.core.TelemetriaSource
import dev.hornet.observability.core.ThrottlingClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetriaViewModelTest {

    private val eventos = mutableListOf<String>()
    private val detector = DetectorFalso(eventos)
    private val classificador = ClassificadorFalso(eventos)

    @Test
    fun cicloLeAvaliaEClassificaNestaOrdem() {
        val viewModel = criarViewModel { amostra(30.0) }

        viewModel.amostrar()

        assertEquals(listOf("ler", "avaliar 30.0", "classificar 30.0"), eventos)
    }

    @Test
    fun classificadorRecebeAAvaliacaoDoDetectorParaAMesmaAmostra() {
        val viewModel = criarViewModel { amostra(36.0, usoCpu = 80.0) }

        viewModel.amostrar()

        val estado = viewModel.estado.value
        assertSame(detector.ultimaAvaliacao, classificador.ultimaAvaliacao)
        assertSame(estado.amostra, classificador.ultimaAmostra)
        assertEquals(StatusLeitura.PICO, estado.avaliacao?.status)
        assertEquals(EstadoThrottling.AQUECENDO, estado.resultado?.estado)
        assertFalse(estado.indisponivel)
    }

    @Test
    fun fonteNulaMarcaIndisponivelSemChamarDetectorNemClassificador() {
        val viewModel = criarViewModel { null }

        viewModel.amostrar()

        val estado = viewModel.estado.value
        assertTrue(estado.indisponivel)
        assertNull(estado.resultado)
        assertEquals(listOf("ler"), eventos)
    }

    @Test
    fun falhaNaFonteNaoDerrubaOApp() {
        val viewModel = criarViewModel { throw IllegalStateException("falha simulada") }

        viewModel.amostrar()

        assertTrue(viewModel.estado.value.indisponivel)
    }

    @Test
    fun historicosGuardamNoMaximoSessentaLeiturasAlinhadas() {
        var decimos = 200
        val viewModel = criarViewModel {
            decimos += 1
            amostra(decimos / 10.0, frequenciaAtualKhz = if (decimos % 2 == 0) 1_000_000L else null)
        }

        repeat(70) { viewModel.amostrar() }

        val estado = viewModel.estado.value
        assertEquals(60, estado.historico.size)
        assertEquals(60, estado.historicoRazaoFrequencia.size)
        assertEquals(21.1, estado.historico.first().temperatura.celsius, 0.0001)
        // Leitura sem frequência fica null no histórico, e não zero.
        assertNull(estado.historicoRazaoFrequencia.first())
        assertEquals(0.5, estado.historicoRazaoFrequencia.last() ?: 0.0, 0.0001)
    }

    @Test
    fun simularPicoReclassificaAUltimaAmostraComMaisSeisGraus() {
        val viewModel = criarViewModel { amostra(30.0, usoCpu = 40.0) }
        viewModel.amostrar()

        viewModel.simularPico()

        val estado = viewModel.estado.value
        assertEquals(2, estado.historico.size)
        assertEquals(36.0, estado.amostra?.temperaturaBateria?.celsius ?: 0.0, 0.0001)
        assertEquals(40.0, estado.amostra?.usoCpuPercentual ?: 0.0, 0.0001)
        assertEquals(StatusLeitura.PICO, estado.avaliacao?.status)
        assertEquals(EstadoThrottling.AQUECENDO, estado.resultado?.estado)
    }

    @Test
    fun simularPicoSemLeituraNaoFazNada() {
        val viewModel = criarViewModel { null }

        viewModel.simularPico()

        assertTrue(viewModel.estado.value.historico.isEmpty())
        assertTrue(eventos.isEmpty())
    }

    private fun criarViewModel(ler: () -> AmostraTelemetria?) = TelemetriaViewModel(
        fonte = object : TelemetriaSource {
            override fun lerAtual(): AmostraTelemetria? {
                eventos += "ler"
                return ler()
            }
        },
        detector = detector,
        classificador = classificador,
    )

    private fun amostra(
        celsius: Double,
        usoCpu: Double? = null,
        frequenciaAtualKhz: Long? = null,
    ) = AmostraTelemetria(
        instanteMs = 0L,
        temperaturaBateria = Temperatura.deCelsius(celsius),
        frequenciaAtualKhz = frequenciaAtualKhz,
        frequenciaMaximaKhz = 2_000_000L,
        usoCpuPercentual = usoCpu,
    )

    /** Marca PICO a partir de 36 °C e registra cada chamada. */
    private class DetectorFalso(private val eventos: MutableList<String>) : AnomalyDetector {
        var ultimaAvaliacao: Avaliacao? = null

        override fun avaliar(temperatura: Temperatura): Avaliacao {
            eventos += "avaliar ${temperatura.celsius}"
            return Avaliacao(
                temperatura = temperatura,
                status = if (temperatura.celsius >= 36.0) StatusLeitura.PICO else StatusLeitura.NORMAL,
            ).also { ultimaAvaliacao = it }
        }

        override fun reiniciar() = Unit
    }

    /** Responde AQUECENDO para leituras anômalas e NORMAL para as demais. */
    private class ClassificadorFalso(private val eventos: MutableList<String>) : ThrottlingClassifier {
        var ultimaAmostra: AmostraTelemetria? = null
        var ultimaAvaliacao: Avaliacao? = null

        override fun classificar(
            amostra: AmostraTelemetria,
            avaliacaoTemperatura: Avaliacao,
        ): ResultadoThrottling {
            eventos += "classificar ${amostra.temperaturaBateria.celsius}"
            ultimaAmostra = amostra
            ultimaAvaliacao = avaliacaoTemperatura
            val estado = if (avaliacaoTemperatura.anomala) {
                EstadoThrottling.AQUECENDO
            } else {
                EstadoThrottling.NORMAL
            }
            return ResultadoThrottling(estado, "Motivo falso.")
        }
    }
}

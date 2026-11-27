package dev.hornet.observability.ui

import dev.hornet.observability.core.AmostraTelemetria
import dev.hornet.observability.core.BaselineDetector
import dev.hornet.observability.core.ClassificadorThrottling
import dev.hornet.observability.core.Temperatura
import dev.hornet.observability.core.TelemetriaSource
import org.junit.Assert.assertEquals
import org.junit.Test

/** Usa o BaselineDetector e o ClassificadorThrottling reais; só a fonte é falsa. */
class TelemetriaViewModelIntegracaoTest {

    @Test
    fun statusTermicoModeradoMostraThrottlingConfirmado() {
        val tela = telaApos(amostra(35.0, statusTermico = 2))

        assertEquals(EstadoThrottlingTela.THROTTLING_CONFIRMADO, tela.estado)
        assertEquals("Moderado (2)", tela.statusTermico)
    }

    @Test
    fun bateriaFriaSemOutrosSinaisMostraNormal() {
        val tela = telaApos(amostra(31.0))

        assertEquals(EstadoThrottlingTela.NORMAL, tela.estado)
    }

    @Test
    fun bateriaA38GrausMostraAquecendo() {
        val tela = telaApos(amostra(38.0))

        assertEquals(EstadoThrottlingTela.AQUECENDO, tela.estado)
    }

    @Test
    fun cargaAltaComFrequenciaBaixaEBateriaQuenteMostraThrottlingProvavel() {
        val tela = telaApos(
            amostra(38.0, frequenciaAtualKhz = 1_380_000L, usoCpu = 70.0),
        )

        assertEquals(EstadoThrottlingTela.THROTTLING_PROVAVEL, tela.estado)
    }

    @Test
    fun temperaturaInvalidaMostraDadosInsuficientes() {
        val tela = telaApos(amostra(61.0, statusTermico = 6))

        assertEquals(EstadoThrottlingTela.DADOS_INSUFICIENTES, tela.estado)
    }

    private fun telaApos(amostra: AmostraTelemetria): EstadoTelaMultiSinal {
        val viewModel = TelemetriaViewModel(
            fonte = object : TelemetriaSource {
                override fun lerAtual(): AmostraTelemetria = amostra
            },
            detector = BaselineDetector(),
            classificador = ClassificadorThrottling(),
        )
        viewModel.amostrar()
        return mapearParaTela(viewModel.estado.value)
    }

    private fun amostra(
        celsius: Double,
        frequenciaAtualKhz: Long? = null,
        usoCpu: Double? = null,
        statusTermico: Int? = null,
    ) = AmostraTelemetria(
        instanteMs = 0L,
        temperaturaBateria = Temperatura.deCelsius(celsius),
        frequenciaAtualKhz = frequenciaAtualKhz,
        frequenciaMaximaKhz = 2_000_000L,
        usoCpuPercentual = usoCpu,
        statusTermico = statusTermico,
    )
}

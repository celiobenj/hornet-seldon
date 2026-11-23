package dev.hornet.observability.ui

import dev.hornet.observability.core.AnomalyDetector
import dev.hornet.observability.core.Avaliacao
import dev.hornet.observability.core.StatusLeitura
import dev.hornet.observability.core.Temperatura
import dev.hornet.observability.core.TemperatureSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperaturaViewModelTest {

    @Test
    fun acumulaHistoricoAteOLimiteDeSessentaLeituras() {
        var decimos = 200
        val viewModel = criarViewModel(
            fonte = FonteFalsa {
                decimos += 1
                Temperatura.deDecimosDeCelsius(decimos)
            },
        )

        repeat(70) { viewModel.amostrar() }

        assertEquals(60, viewModel.estado.value.historico.size)
        assertEquals(21.1, viewModel.estado.value.historico.first().temperatura.celsius, 0.0001)
        assertEquals(27.0, viewModel.estado.value.atual?.temperatura?.celsius ?: 0.0, 0.0001)
    }

    @Test
    fun fonteNulaMarcaTemperaturaComoIndisponivel() {
        val viewModel = criarViewModel(fonte = FonteFalsa { null })

        viewModel.amostrar()

        assertTrue(viewModel.estado.value.indisponivel)
        assertNull(viewModel.estado.value.atual)
    }

    @Test
    fun simularPicoAvaliaMaisSeisGrausDaUltimaLeituraNaoAnomala() {
        val viewModel = criarViewModel(fonte = FonteFalsa { Temperatura.deCelsius(30.0) })
        viewModel.amostrar()

        viewModel.simularPico()

        assertEquals(2, viewModel.estado.value.historico.size)
        assertEquals(36.0, viewModel.estado.value.atual?.temperatura?.celsius ?: 0.0, 0.0001)
        assertEquals(StatusLeitura.PICO, viewModel.estado.value.atual?.status)
        assertFalse(viewModel.estado.value.indisponivel)
    }

    @Test
    fun simularPicoUsaLeituraAtualQuandoHistoricoNaoTemLeituraNormal() {
        val viewModel = criarViewModel(fonte = FonteFalsa { Temperatura.deCelsius(36.0) })
        viewModel.amostrar()

        viewModel.simularPico()

        assertEquals(2, viewModel.estado.value.historico.size)
        assertEquals(42.0, viewModel.estado.value.atual?.temperatura?.celsius ?: 0.0, 0.0001)
        assertEquals(StatusLeitura.PICO, viewModel.estado.value.atual?.status)
    }

    private fun criarViewModel(fonte: TemperatureSource): TemperaturaViewModel =
        TemperaturaViewModel(fonte, DetectorFalso())

    private class FonteFalsa(
        private val ler: () -> Temperatura?,
    ) : TemperatureSource {
        override fun lerAtual(): Temperatura? = ler()
    }

    private class DetectorFalso : AnomalyDetector {
        override fun avaliar(temperatura: Temperatura): Avaliacao = Avaliacao(
            temperatura = temperatura,
            status = if (temperatura.celsius >= 36.0) StatusLeitura.PICO else StatusLeitura.NORMAL,
        )

        override fun reiniciar() = Unit
    }
}

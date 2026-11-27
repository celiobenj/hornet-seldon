package dev.hornet.observability.ui

import dev.hornet.observability.core.AmostraTelemetria
import dev.hornet.observability.core.Avaliacao
import dev.hornet.observability.core.EstadoThrottling
import dev.hornet.observability.core.ResultadoThrottling
import dev.hornet.observability.core.StatusLeitura
import dev.hornet.observability.core.Temperatura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapeadorTelaMultiSinalTest {

    @Test
    fun mapeiaAmostraCompletaParaATela() {
        val tela = mapearParaTela(
            estadoCom(
                AmostraTelemetria(
                    instanteMs = 0L,
                    temperaturaBateria = Temperatura.deCelsius(38.4),
                    frequenciaAtualKhz = 1_380_000L,
                    frequenciaMaximaKhz = 2_000_000L,
                    tetoFrequenciaKhz = 1_780_000L,
                    usoCpuPercentual = 72.5,
                    statusTermico = 2,
                ),
                ResultadoThrottling(EstadoThrottling.THROTTLING_PROVAVEL, "Teto reduzido."),
            ),
        )

        assertEquals("${"%.1f".format(38.4)} °C", tela.temperaturaTexto)
        assertEquals(1380L, tela.frequenciaAtualMhz)
        assertEquals(2000L, tela.frequenciaMaximaMhz)
        assertEquals(0.69, tela.razaoFrequencia ?: 0.0, 0.0001)
        assertEquals(72.5, tela.usoCpuPercentual ?: 0.0, 0.0001)
        assertEquals("Moderado (2)", tela.statusTermico)
        assertEquals(EstadoThrottlingTela.THROTTLING_PROVAVEL, tela.estado)
        assertEquals("Teto reduzido.", tela.motivo)
    }

    @Test
    fun sinaisAusentesFicamNulosENuncaZero() {
        val tela = mapearParaTela(
            estadoCom(
                AmostraTelemetria(instanteMs = 0L, temperaturaBateria = Temperatura.deCelsius(30.0)),
                ResultadoThrottling(EstadoThrottling.NORMAL, "Sem evidências."),
            ),
        )

        assertNull(tela.frequenciaAtualMhz)
        assertNull(tela.frequenciaMaximaMhz)
        assertNull(tela.razaoFrequencia)
        assertNull(tela.usoCpuPercentual)
        assertNull(tela.statusTermico)
    }

    @Test
    fun sinaisInvalidosFicamNulos() {
        val tela = mapearParaTela(
            estadoCom(
                AmostraTelemetria(
                    instanteMs = 0L,
                    temperaturaBateria = Temperatura.deCelsius(30.0),
                    frequenciaAtualKhz = 0L,
                    frequenciaMaximaKhz = -1L,
                    usoCpuPercentual = Double.NaN,
                    statusTermico = 7,
                ),
                ResultadoThrottling(EstadoThrottling.NORMAL, "Sem evidências."),
            ),
        )

        assertNull(tela.frequenciaAtualMhz)
        assertNull(tela.frequenciaMaximaMhz)
        assertNull(tela.razaoFrequencia)
        assertNull(tela.usoCpuPercentual)
        assertNull(tela.statusTermico)
    }

    @Test
    fun usoZeroEStatusZeroSaoValoresValidos() {
        val tela = mapearParaTela(
            estadoCom(
                AmostraTelemetria(
                    instanteMs = 0L,
                    temperaturaBateria = Temperatura.deCelsius(30.0),
                    usoCpuPercentual = 0.0,
                    statusTermico = 0,
                ),
                ResultadoThrottling(EstadoThrottling.NORMAL, "Sem evidências."),
            ),
        )

        assertEquals(0.0, tela.usoCpuPercentual ?: -1.0, 0.0)
        assertEquals("Nenhum (0)", tela.statusTermico)
    }

    @Test
    fun temperaturaIndisponivelViraDadosInsuficientes() {
        val tela = mapearParaTela(TelemetriaUiState(indisponivel = true))

        assertEquals(EstadoThrottlingTela.DADOS_INSUFICIENTES, tela.estado)
        assertEquals("Temperatura da bateria indisponível.", tela.motivo)
        assertNull(tela.temperaturaTexto)
    }

    @Test
    fun antesDaPrimeiraLeituraMostraAguardando() {
        val tela = mapearParaTela(TelemetriaUiState())

        assertEquals(EstadoThrottlingTela.DADOS_INSUFICIENTES, tela.estado)
        assertEquals("Aguardando a primeira leitura.", tela.motivo)
    }

    @Test
    fun cadaEstadoDoCoreTemUmEstadoNaTela() {
        assertEquals(EstadoThrottlingTela.DADOS_INSUFICIENTES, estadoDaTela(EstadoThrottling.INDETERMINADO))
        assertEquals(EstadoThrottlingTela.NORMAL, estadoDaTela(EstadoThrottling.NORMAL))
        assertEquals(EstadoThrottlingTela.AQUECENDO, estadoDaTela(EstadoThrottling.AQUECENDO))
        assertEquals(
            EstadoThrottlingTela.THROTTLING_PROVAVEL,
            estadoDaTela(EstadoThrottling.THROTTLING_PROVAVEL),
        )
        assertEquals(
            EstadoThrottlingTela.THROTTLING_CONFIRMADO,
            estadoDaTela(EstadoThrottling.THROTTLING_CONFIRMADO),
        )
    }

    @Test
    fun converteKhzParaMhz() {
        assertEquals(2304L, khzParaMhz(2_304_000L))
        assertNull(khzParaMhz(null))
        assertNull(khzParaMhz(0L))
        assertNull(khzParaMhz(-5L))
    }

    private fun estadoCom(amostra: AmostraTelemetria, resultado: ResultadoThrottling) =
        TelemetriaUiState(
            amostra = amostra,
            avaliacao = Avaliacao(amostra.temperaturaBateria, StatusLeitura.NORMAL),
            resultado = resultado,
        )
}

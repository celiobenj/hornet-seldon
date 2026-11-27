package dev.hornet.observability.core

import dev.hornet.observability.core.EstadoThrottling.AQUECENDO
import dev.hornet.observability.core.EstadoThrottling.INDETERMINADO
import dev.hornet.observability.core.EstadoThrottling.NORMAL
import dev.hornet.observability.core.EstadoThrottling.THROTTLING_CONFIRMADO
import dev.hornet.observability.core.EstadoThrottling.THROTTLING_PROVAVEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassificadorThrottlingCenariosTest {

    private val classificador: ThrottlingClassifier = ClassificadorThrottling()

    @Test
    fun `CT-25 temperatura invalida tem prioridade sobre status termico alto`() {
        val amostra = criarAmostra(
            celsius = 61.0,
            atualKhz = 1_000_000L,
            maximaKhz = 2_000_000L,
            tetoKhz = 1_600_000L,
            uso = 90.0,
            statusTermico = 6,
        )

        val resultado = classificarCom(amostra, StatusLeitura.INVALIDA)

        assertEquals(INDETERMINADO, resultado.estado)
        assertTrue(resultado.motivo.contains("inválida", ignoreCase = true))
    }

    @Test
    fun `CT-26 frequencia baixa com uso baixo permanece normal`() {
        val amostra = criarAmostra(
            celsius = 35.0,
            atualKhz = 1_000_000L,
            maximaKhz = 2_000_000L,
            tetoKhz = 2_000_000L,
            uso = 10.0,
            statusTermico = 0,
        )

        val resultado = classificarCom(amostra, StatusLeitura.NORMAL)

        assertEquals(NORMAL, resultado.estado)
        assertTrue(resultado.motivo.isNotBlank())
    }

    @Test
    fun `CT-27 pico e temperatura absoluta geram aquecendo`() {
        val comPico = criarAmostra(celsius = 36.0)
        val naFaixa = criarAmostra(celsius = 38.0)

        assertEquals(AQUECENDO, classificarCom(comPico, StatusLeitura.PICO).estado)
        assertEquals(AQUECENDO, classificarCom(naFaixa, StatusLeitura.NORMAL).estado)
    }

    @Test
    fun `CT-28 teto reduzido com bateria quente gera provavel`() {
        val amostra = criarAmostra(
            celsius = 39.0,
            maximaKhz = 2_000_000L,
            tetoKhz = 1_780_000L,
        )

        val resultado = classificarCom(amostra, StatusLeitura.NORMAL)

        assertEquals(THROTTLING_PROVAVEL, resultado.estado)
        assertTrue(resultado.motivo.contains("teto", ignoreCase = true))
    }

    @Test
    fun `CT-29 status termico dois com bateria fria confirma`() {
        val amostra = criarAmostra(celsius = 35.0, statusTermico = 2)

        val resultado = classificarCom(amostra, StatusLeitura.NORMAL)

        assertEquals(THROTTLING_CONFIRMADO, resultado.estado)
        assertTrue(resultado.motivo.contains("2"))
    }

    @Test
    fun `CT-30 razao do teto igual ao limite nao impoe teto`() {
        val amostra = criarAmostra(
            celsius = 40.0,
            maximaKhz = 2_000_000L,
            tetoKhz = 1_800_000L,
        )

        val resultado = classificarCom(amostra, StatusLeitura.NORMAL)

        assertEquals(AQUECENDO, resultado.estado)
    }

    @Test
    fun `CT-31 razao de frequencia igual ao limite nao conta como baixa`() {
        val amostra = criarAmostra(
            celsius = 38.0,
            atualKhz = 1_400_000L,
            maximaKhz = 2_000_000L,
            uso = 70.0,
            statusTermico = 0,
        )

        val resultado = classificarCom(amostra, StatusLeitura.NORMAL)

        assertEquals(AQUECENDO, resultado.estado)
    }

    @Test
    fun `CT-32 uso exatamente no limite com frequencia baixa gera provavel`() {
        val amostra = criarAmostra(
            celsius = 38.0,
            atualKhz = 1_380_000L,
            maximaKhz = 2_000_000L,
            uso = 70.0,
            statusTermico = 0,
        )

        val resultado = classificarCom(amostra, StatusLeitura.NORMAL)

        assertEquals(THROTTLING_PROVAVEL, resultado.estado)
    }

    @Test
    fun `CT-33 temperatura no limite e logo abaixo`() {
        val noLimite = criarAmostra(celsius = 38.0)
        val abaixoDoLimite = criarAmostra(celsius = 37.9)

        assertEquals(AQUECENDO, classificarCom(noLimite, StatusLeitura.NORMAL).estado)
        assertEquals(NORMAL, classificarCom(abaixoDoLimite, StatusLeitura.NORMAL).estado)
    }

    @Test
    fun `CT-34 temperatura de confirmacao e logo abaixo com teto reduzido`() {
        val noLimite = criarAmostra(
            celsius = 40.0,
            maximaKhz = 2_000_000L,
            tetoKhz = 1_780_000L,
        )
        val abaixoDoLimite = criarAmostra(
            celsius = 39.9,
            maximaKhz = 2_000_000L,
            tetoKhz = 1_780_000L,
        )

        assertEquals(THROTTLING_CONFIRMADO, classificarCom(noLimite, StatusLeitura.NORMAL).estado)
        assertEquals(THROTTLING_PROVAVEL, classificarCom(abaixoDoLimite, StatusLeitura.NORMAL).estado)
    }

    @Test
    fun `CT-35 status termico um e dois`() {
        val statusUm = criarAmostra(celsius = 35.0, statusTermico = 1)
        val statusDois = criarAmostra(celsius = 35.0, statusTermico = 2)

        assertEquals(NORMAL, classificarCom(statusUm, StatusLeitura.NORMAL).estado)
        assertEquals(THROTTLING_CONFIRMADO, classificarCom(statusDois, StatusLeitura.NORMAL).estado)
    }

    @Test
    fun `CT-36 sem uso da cpu frequencia baixa nao gera provavel`() {
        val amostra = criarAmostra(
            celsius = 38.0,
            atualKhz = 1_380_000L,
            maximaKhz = 2_000_000L,
            statusTermico = 0,
        )

        assertEquals(AQUECENDO, classificarCom(amostra, StatusLeitura.NORMAL).estado)
    }

    @Test
    fun `CT-37 sem status termico nao confirma`() {
        val amostra = criarAmostra(celsius = 35.0)

        val resultado = classificarCom(amostra, StatusLeitura.NORMAL)

        assertEquals(NORMAL, resultado.estado)
        assertTrue(resultado.motivo.isNotBlank())
    }

    @Test
    fun `CT-38 sem frequencia`() {
        val semDados = criarAmostra(celsius = 35.0)
        val maximaZero = criarAmostra(
            celsius = 35.0,
            atualKhz = 1_000_000L,
            maximaKhz = 0L,
            tetoKhz = 1_600_000L,
        )
        val atualZero = criarAmostra(
            celsius = 35.0,
            atualKhz = 0L,
            maximaKhz = 2_000_000L,
            tetoKhz = 1_600_000L,
        )
        val semTeto = criarAmostra(
            celsius = 35.0,
            atualKhz = 1_000_000L,
            maximaKhz = 2_000_000L,
        )

        assertNull(semDados.razaoFrequencia())
        assertNull(semDados.razaoTeto())
        assertNull(maximaZero.razaoFrequencia())
        assertNull(maximaZero.razaoTeto())
        assertNull(atualZero.razaoFrequencia())
        assertEquals(0.8, atualZero.razaoTeto()!!, 1e-9)
        assertEquals(0.5, semTeto.razaoFrequencia()!!, 1e-9)
        assertNull(semTeto.razaoTeto())

        assertEquals(NORMAL, classificarCom(semDados, StatusLeitura.NORMAL).estado)
        assertEquals(NORMAL, classificarCom(maximaZero, StatusLeitura.NORMAL).estado)
        assertEquals(NORMAL, classificarCom(atualZero, StatusLeitura.NORMAL).estado)
        assertEquals(NORMAL, classificarCom(semTeto, StatusLeitura.NORMAL).estado)
    }

    @Test
    fun `CT-39 somente temperatura`() {
        val amostra = AmostraTelemetria(
            instanteMs = INSTANTE_MS,
            temperaturaBateria = Temperatura.deCelsius(35.0),
        )

        val resultado = classificador.classificar(amostra, Avaliacao(amostra.temperaturaBateria, StatusLeitura.NORMAL))

        assertEquals(NORMAL, resultado.estado)
        assertNull(amostra.usoCpuValido())
        assertNull(amostra.statusTermicoValido())
    }

    @Test
    fun `CT-40 unidades khz e decimos de celsius`() {
        val amostra = AmostraTelemetria(
            instanteMs = INSTANTE_MS,
            temperaturaBateria = Temperatura.deDecimosDeCelsius(345),
            frequenciaAtualKhz = 1_380_000L,
            frequenciaMaximaKhz = 2_000_000L,
            tetoFrequenciaKhz = 1_780_000L,
        )

        assertEquals(34.5, amostra.temperaturaBateria.celsius, 1e-9)
        assertEquals(0.69, amostra.razaoFrequencia()!!, 1e-9)
        assertEquals(0.89, amostra.razaoTeto()!!, 1e-9)
        assertEquals(NORMAL, classificarCom(amostra, StatusLeitura.NORMAL).estado)
    }

    @Test
    fun `CT-41 precedencia do status termico e economia de energia`() {
        val comStatus = criarAmostra(
            celsius = 35.0,
            atualKhz = 1_000_000L,
            maximaKhz = 2_000_000L,
            tetoKhz = 1_600_000L,
            uso = 90.0,
            statusTermico = 2,
        )
        val economia = criarAmostra(
            celsius = 35.0,
            atualKhz = 1_000_000L,
            maximaKhz = 2_000_000L,
            uso = 10.0,
            statusTermico = 0,
        )

        val confirmado = classificarCom(comStatus, StatusLeitura.NORMAL)
        assertEquals(THROTTLING_CONFIRMADO, confirmado.estado)
        assertTrue(confirmado.motivo.contains("Android", ignoreCase = true))
        assertEquals(NORMAL, classificarCom(economia, StatusLeitura.NORMAL).estado)
    }

    private fun criarAmostra(
        celsius: Double,
        atualKhz: Long? = null,
        maximaKhz: Long? = null,
        tetoKhz: Long? = null,
        uso: Double? = null,
        statusTermico: Int? = null,
    ) = AmostraTelemetria(
        instanteMs = INSTANTE_MS,
        temperaturaBateria = Temperatura.deCelsius(celsius),
        frequenciaAtualKhz = atualKhz,
        frequenciaMaximaKhz = maximaKhz,
        tetoFrequenciaKhz = tetoKhz,
        usoCpuPercentual = uso,
        statusTermico = statusTermico,
    )

    private fun classificarCom(
        amostra: AmostraTelemetria,
        status: StatusLeitura,
    ) = classificador.classificar(amostra, Avaliacao(amostra.temperaturaBateria, status))

    private companion object {
        const val INSTANTE_MS = 1_000L
    }
}

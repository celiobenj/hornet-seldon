package dev.hornet.observability.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmostraTelemetriaTest {

    private val tolerancia = 1e-9

    private fun amostra(
        atual: Long? = null,
        maxima: Long? = null,
        teto: Long? = null,
        uso: Double? = null,
        status: Int? = null,
    ) = AmostraTelemetria(
        instanteMs = 0L,
        temperaturaBateria = Temperatura.deCelsius(35.0),
        frequenciaAtualKhz = atual,
        frequenciaMaximaKhz = maxima,
        tetoFrequenciaKhz = teto,
        usoCpuPercentual = uso,
        statusTermico = status,
    )

    @Test
    fun `todos os sinais presentes geram as duas razoes`() {
        val a = amostra(atual = 1_380_000, maxima = 2_000_000, teto = 1_780_000, uso = 70.0, status = 2)
        assertEquals(0.69, a.razaoFrequencia()!!, tolerancia)
        assertEquals(0.89, a.razaoTeto()!!, tolerancia)
        assertEquals(70.0, a.usoCpuValido()!!, tolerancia)
        assertEquals(2, a.statusTermicoValido())
    }

    @Test
    fun `sem frequencia maxima as duas razoes ficam ausentes`() {
        val a = amostra(atual = 1_000_000, maxima = null, teto = 1_600_000)
        assertNull(a.razaoFrequencia())
        assertNull(a.razaoTeto())
    }

    @Test
    fun `frequencia maxima zero ou negativa deixa as razoes ausentes`() {
        assertNull(amostra(atual = 1_000_000, maxima = 0, teto = 1_600_000).razaoFrequencia())
        assertNull(amostra(atual = 1_000_000, maxima = 0, teto = 1_600_000).razaoTeto())
        assertNull(amostra(atual = 1_000_000, maxima = -1, teto = 1_600_000).razaoFrequencia())
    }

    @Test
    fun `frequencia atual nula zero ou negativa deixa so a razao de frequencia ausente`() {
        val semAtual = amostra(atual = null, maxima = 2_000_000, teto = 1_600_000)
        assertNull(semAtual.razaoFrequencia())
        assertEquals(0.8, semAtual.razaoTeto()!!, tolerancia)
        assertNull(amostra(atual = 0, maxima = 2_000_000).razaoFrequencia())
        assertNull(amostra(atual = -5, maxima = 2_000_000).razaoFrequencia())
    }

    @Test
    fun `teto nulo zero ou negativo deixa so a razao do teto ausente`() {
        val semTeto = amostra(atual = 1_000_000, maxima = 2_000_000, teto = null)
        assertEquals(0.5, semTeto.razaoFrequencia()!!, tolerancia)
        assertNull(semTeto.razaoTeto())
        assertNull(amostra(atual = 1_000_000, maxima = 2_000_000, teto = 0).razaoTeto())
        assertNull(amostra(atual = 1_000_000, maxima = 2_000_000, teto = -1).razaoTeto())
    }

    @Test
    fun `teto maior que o maximo nao e limitado a 1`() {
        val a = amostra(atual = 1_000_000, maxima = 2_000_000, teto = 2_200_000)
        assertEquals(1.1, a.razaoTeto()!!, tolerancia)
    }

    @Test
    fun `razao no limite de 0,90 e 0,70 e exata`() {
        assertEquals(0.9, amostra(maxima = 2_000_000, teto = 1_800_000).razaoTeto()!!, 0.0)
        assertEquals(0.7, amostra(atual = 1_400_000, maxima = 2_000_000).razaoFrequencia()!!, 0.0)
    }

    @Test
    fun `uso da CPU de 0 e de 100 sao validos`() {
        assertEquals(0.0, amostra(uso = 0.0).usoCpuValido()!!, tolerancia)
        assertEquals(100.0, amostra(uso = 100.0).usoCpuValido()!!, tolerancia)
    }

    @Test
    fun `uso da CPU fora de 0 a 100 nao e valido`() {
        assertNull(amostra(uso = 101.0).usoCpuValido())
        assertNull(amostra(uso = -1.0).usoCpuValido())
        assertNull(amostra(uso = 100.1).usoCpuValido())
    }

    @Test
    fun `uso da CPU ausente NaN ou infinito nao e valido`() {
        assertNull(amostra(uso = null).usoCpuValido())
        assertNull(amostra(uso = Double.NaN).usoCpuValido())
        assertNull(amostra(uso = Double.POSITIVE_INFINITY).usoCpuValido())
        assertNull(amostra(uso = Double.NEGATIVE_INFINITY).usoCpuValido())
    }

    @Test
    fun `status termico de 0 a 6 e valido`() {
        assertEquals(0, amostra(status = 0).statusTermicoValido())
        assertEquals(6, amostra(status = 6).statusTermicoValido())
    }

    @Test
    fun `status termico fora de 0 a 6 ou ausente nao e valido`() {
        assertNull(amostra(status = 7).statusTermicoValido())
        assertNull(amostra(status = -1).statusTermicoValido())
        assertNull(amostra(status = null).statusTermicoValido())
    }

    @Test
    fun `amostra so com temperatura nao lanca excecao`() {
        val a = amostra()
        assertNull(a.razaoFrequencia())
        assertNull(a.razaoTeto())
        assertNull(a.usoCpuValido())
        assertNull(a.statusTermicoValido())
    }
}

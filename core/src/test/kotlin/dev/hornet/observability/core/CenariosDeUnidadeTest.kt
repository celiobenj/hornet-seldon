package dev.hornet.observability.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CenariosDeUnidadeTest {

    private val tolerancia = 1e-9

    @Test
    fun `serie em Celsius e Fahrenheit produz os mesmos status`() {
        val serie = mutableListOf<Double>()
        repeat(5) {
            serie.add(30.0)
            serie.add(30.5)
            serie.add(31.0)
        }
        serie.add(36.0)
        serie.add(31.0)

        val statusEmCelsius = avaliarEmCelsius(serie)
        val statusEmFahrenheit = avaliarEmFahrenheitConvertido(serie)

        val esperado = List(10) { StatusLeitura.SEM_BASE } +
            List(5) { StatusLeitura.NORMAL } +
            StatusLeitura.PICO +
            StatusLeitura.NORMAL

        assertEquals(esperado, statusEmCelsius)
        assertEquals(esperado, statusEmFahrenheit)
    }

    @Test
    fun `valores de 86 a 95 em Celsius sao invalidos`() {
        val detector = BaselineDetector()

        (86..95).forEach { valor ->
            val avaliacao = detector.avaliar(Temperatura.deCelsius(valor.toDouble()))
            assertEquals(StatusLeitura.INVALIDA, avaliacao.status)
        }

        assertEquals(StatusLeitura.SEM_BASE, detector.avaliar(Temperatura.deCelsius(30.0)).status)
    }

    @Test
    fun `345 em Celsius e invalido e 345 decimos e valido`() {
        val detector = BaselineDetector()

        assertEquals(StatusLeitura.INVALIDA, detector.avaliar(Temperatura.deCelsius(345.0)).status)

        val avaliacao = detector.avaliar(Temperatura.deDecimosDeCelsius(345))
        assertEquals(34.5, avaliacao.temperatura.celsius, tolerancia)
        assertEquals(StatusLeitura.SEM_BASE, avaliacao.status)
    }

    private fun avaliarEmCelsius(valores: List<Double>): List<StatusLeitura> {
        val detector = BaselineDetector()
        return valores.map { detector.avaliar(Temperatura.deCelsius(it)).status }
    }

    private fun avaliarEmFahrenheitConvertido(valores: List<Double>): List<StatusLeitura> {
        val detector = BaselineDetector()
        return valores.map { valor ->
            val fahrenheit = Temperatura.deCelsius(valor).fahrenheit
            detector.avaliar(Temperatura.deFahrenheit(fahrenheit)).status
        }
    }
}

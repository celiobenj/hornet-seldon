package dev.hornet.observability.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperaturaConversaoTest {

    private val tolerancia = 1e-9

    @Test
    fun `conversao de Fahrenheit para Celsius`() {
        assertEquals(35.0, Temperatura.deFahrenheit(95.0).celsius, tolerancia)
        assertEquals(45.0, Temperatura.deFahrenheit(113.0).celsius, tolerancia)
        assertEquals(0.0, Temperatura.deFahrenheit(32.0).celsius, tolerancia)
        assertEquals(100.0, Temperatura.deFahrenheit(212.0).celsius, tolerancia)
    }

    @Test
    fun `conversao de decimos de Celsius`() {
        assertEquals(34.5, Temperatura.deDecimosDeCelsius(345).celsius, tolerancia)
        assertEquals(45.0, Temperatura.deDecimosDeCelsius(450).celsius, tolerancia)
        assertEquals(0.0, Temperatura.deDecimosDeCelsius(0).celsius, tolerancia)
    }

    @Test
    fun `ida e volta entre Celsius e Fahrenheit`() {
        val temperatura = Temperatura.deCelsius(35.0)

        assertEquals(95.0, temperatura.fahrenheit, tolerancia)
        assertEquals(35.0, Temperatura.deFahrenheit(temperatura.fahrenheit).celsius, tolerancia)
    }
}

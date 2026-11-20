package dev.hornet.observability.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperaturaBasicaTest {

    private val tolerancia = 1e-9

    @Test
    fun `95 F equivale a 35 C`() {
        assertEquals(35.0, Temperatura.deFahrenheit(95.0).celsius, tolerancia)
    }

    @Test
    fun `113 F equivale a 45 C`() {
        assertEquals(45.0, Temperatura.deFahrenheit(113.0).celsius, tolerancia)
    }

    @Test
    fun `345 decimos equivalem a 34,5 C`() {
        assertEquals(34.5, Temperatura.deDecimosDeCelsius(345).celsius, tolerancia)
    }

    @Test
    fun `35 C equivale a 95 F`() {
        assertEquals(95.0, Temperatura.deCelsius(35.0).fahrenheit, tolerancia)
    }
}

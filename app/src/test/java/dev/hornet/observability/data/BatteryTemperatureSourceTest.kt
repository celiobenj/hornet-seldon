package dev.hornet.observability.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryTemperatureSourceTest {

    @Test
    fun converteDecimosDeCelsius() {
        val fonte = BatteryTemperatureSource { 345 }

        assertEquals(34.5, fonte.lerAtual()?.celsius ?: Double.NaN, 0.0001)
    }

    @Test
    fun retornaNuloQuandoLeituraIndisponivel() {
        val fonte = BatteryTemperatureSource { null }

        assertNull(fonte.lerAtual())
    }

    @Test
    fun converteValorMinimoEmIndisponivel() {
        val fonte = BatteryTemperatureSource { Int.MIN_VALUE }

        assertNull(fonte.lerAtual())
    }
}

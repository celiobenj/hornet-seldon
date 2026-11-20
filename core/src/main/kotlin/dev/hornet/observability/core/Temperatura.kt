package dev.hornet.observability.core

/**
 * Temperatura guardada sempre em graus Celsius.
 * Use as fábricas para não confundir a unidade de entrada.
 */
@JvmInline
value class Temperatura(val celsius: Double) {

    /** Valor equivalente em graus Fahrenheit. */
    val fahrenheit: Double
        get() = celsius * 9.0 / 5.0 + 32.0

    companion object {
        fun deCelsius(valor: Double): Temperatura = Temperatura(valor)

        fun deFahrenheit(valor: Double): Temperatura = Temperatura((valor - 32.0) * 5.0 / 9.0)

        /** Formato do Android (BatteryManager.EXTRA_TEMPERATURE): 345 significa 34,5 graus Celsius. */
        fun deDecimosDeCelsius(valor: Int): Temperatura = Temperatura(valor / 10.0)
    }
}

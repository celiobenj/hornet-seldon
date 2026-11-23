package dev.hornet.observability.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dev.hornet.observability.core.TemperatureSource
import dev.hornet.observability.core.Temperatura

class BatteryTemperatureSource(
    private val lerDecimos: () -> Int?,
) : TemperatureSource {

    override fun lerAtual(): Temperatura? =
        lerDecimos()
            ?.takeUnless { it == Int.MIN_VALUE }
            ?.let { Temperatura.deDecimosDeCelsius(it) }

    companion object {
        fun deContexto(context: Context): BatteryTemperatureSource = BatteryTemperatureSource {
            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            )
            intent
                ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                ?.takeUnless { it == Int.MIN_VALUE }
        }
    }
}

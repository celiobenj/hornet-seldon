package dev.hornet.observability.data

import android.content.Context
import dev.hornet.observability.core.AmostraTelemetria
import dev.hornet.observability.core.TelemetriaSource
import dev.hornet.observability.core.TemperatureSource

/**
 * Implementacao Android da porta TelemetriaSource do core (contrato v2).
 *
 * Compoe a AmostraTelemetria a partir da temperatura da bateria e dos adaptadores
 * de frequencia da CPU, uso da CPU (Plano B) e status termico, carimbando instanteMs.
 * Apenas a temperatura e obrigatoria: se ela falhar ou estiver ausente, retorna null.
 * Falhas nos demais adaptadores resultam apenas em campos nulos, sem lancar excecao.
 */
class TelemetriaAndroidSource(
    private val fonteTemperatura: TemperatureSource,
    private val fonteFrequencia: FrequenciaCpuSource = FrequenciaCpuSource(),
    private val fonteUsoCpu: UsoCpuSource = UsoCpuSource(),
    private val fonteStatusTermico: StatusTermicoSource,
    private val lerInstanteMs: () -> Long = { System.currentTimeMillis() },
) : TelemetriaSource {

    override fun lerAtual(): AmostraTelemetria? {
        val temperatura = lerComSeguranca { fonteTemperatura.lerAtual() } ?: return null
        val instanteMs = lerComSeguranca { lerInstanteMs() } ?: return null
        val frequencia = lerComSeguranca { fonteFrequencia.lerAtual() }
        val usoCpuPercentual = lerComSeguranca { fonteUsoCpu.lerAtual() }
        val statusTermico = lerComSeguranca { fonteStatusTermico.lerAtual() }

        return AmostraTelemetria(
            instanteMs = instanteMs,
            temperaturaBateria = temperatura,
            frequenciaAtualKhz = frequencia?.frequenciaAtualKhz,
            frequenciaMaximaKhz = frequencia?.frequenciaMaximaKhz,
            tetoFrequenciaKhz = frequencia?.tetoFrequenciaKhz,
            usoCpuPercentual = usoCpuPercentual,
            statusTermico = statusTermico,
        )
    }

    private inline fun <T> lerComSeguranca(leitura: () -> T?): T? {
        return try {
            leitura()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun deContexto(context: Context): TelemetriaAndroidSource {
            val contextoApp = context.applicationContext ?: context
            return TelemetriaAndroidSource(
                fonteTemperatura = BatteryTemperatureSource.deContexto(contextoApp),
                fonteFrequencia = FrequenciaCpuSource(),
                fonteUsoCpu = UsoCpuSource(),
                fonteStatusTermico = StatusTermicoSource.deContexto(contextoApp),
            )
        }
    }
}

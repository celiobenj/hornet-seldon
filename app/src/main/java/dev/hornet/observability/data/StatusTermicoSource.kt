package dev.hornet.observability.data

import android.content.Context
import android.os.Build
import android.os.PowerManager
import dev.hornet.observability.core.AmostraTelemetria

/**
 * Adaptador Android para leitura do status termico do aparelho via PowerManager.
 *
 * Usa PowerManager.getCurrentThermalStatus() no Android 10 ou superior (API 29+)
 * e retorna null em versoes anteriores ou em caso de falha, sem lancar excecao.
 */
class StatusTermicoSource(
    private val versaoSdk: Int,
    private val lerStatus: () -> Int?,
) {

    /**
     * Retorna o codigo termico atual do Android (0 = NONE ate 6 = SHUTDOWN),
     * ou null abaixo da API 29, fora da faixa valida ou se a leitura falhar.
     */
    fun lerAtual(): Int? {
        if (versaoSdk < API_MINIMA_STATUS_TERMICO) {
            return null
        }
        return try {
            lerStatus()?.takeIf {
                it in AmostraTelemetria.STATUS_TERMICO_MINIMO..AmostraTelemetria.STATUS_TERMICO_MAXIMO
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val API_MINIMA_STATUS_TERMICO = Build.VERSION_CODES.Q

        fun deContexto(context: Context): StatusTermicoSource {
            val contextoApp = context.applicationContext ?: context
            return StatusTermicoSource(
                versaoSdk = Build.VERSION.SDK_INT,
                lerStatus = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val powerManager = contextoApp.getSystemService(PowerManager::class.java)
                        powerManager?.currentThermalStatus
                    } else {
                        null
                    }
                },
            )
        }
    }
}

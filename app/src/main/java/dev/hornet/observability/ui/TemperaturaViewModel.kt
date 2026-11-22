package dev.hornet.observability.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hornet.observability.core.AnomalyDetector
import dev.hornet.observability.core.Avaliacao
import dev.hornet.observability.core.StatusLeitura
import dev.hornet.observability.core.Temperatura
import dev.hornet.observability.core.TemperatureSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TemperaturaViewModel(
    private val fonte: TemperatureSource,
    private val detector: AnomalyDetector,
) : ViewModel() {

    private val _estado = MutableStateFlow(TemperaturaUiState())
    val estado: StateFlow<TemperaturaUiState> = _estado.asStateFlow()

    private var tarefaAmostragem: Job? = null

    fun iniciar() {
        if (tarefaAmostragem?.isActive == true) return

        tarefaAmostragem = viewModelScope.launch {
            while (isActive) {
                amostrar()
                delay(INTERVALO_AMOSTRAGEM_MS)
            }
        }
    }

    fun amostrar() {
        val temperatura = fonte.lerAtual()
        if (temperatura == null) {
            _estado.value = _estado.value.copy(atual = null, indisponivel = true)
            return
        }

        registrar(detector.avaliar(temperatura))
    }

    fun simularPico() {
        val estadoAtual = _estado.value
        val leituraBase = estadoAtual.historico.lastOrNull {
            it.status == StatusLeitura.NORMAL
        } ?: estadoAtual.atual ?: return
        val temperaturaSimulada = Temperatura.deCelsius(
            leituraBase.temperatura.celsius + AUMENTO_SIMULACAO_CELSIUS,
        )
        registrar(detector.avaliar(temperaturaSimulada))
    }

    private fun registrar(avaliacao: Avaliacao) {
        val historico = (_estado.value.historico + avaliacao).takeLast(LIMITE_HISTORICO)
        _estado.value = _estado.value.copy(
            atual = avaliacao,
            historico = historico,
            indisponivel = false,
        )
    }

    private companion object {
        const val INTERVALO_AMOSTRAGEM_MS = 5_000L
        const val LIMITE_HISTORICO = 60
        const val AUMENTO_SIMULACAO_CELSIUS = 6.0
    }
}

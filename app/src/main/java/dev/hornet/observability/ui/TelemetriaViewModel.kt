package dev.hornet.observability.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hornet.observability.carga.GeradorDeCarga
import dev.hornet.observability.core.AmostraTelemetria
import dev.hornet.observability.core.AnomalyDetector
import dev.hornet.observability.core.Avaliacao
import dev.hornet.observability.core.ResultadoThrottling
import dev.hornet.observability.core.StatusLeitura
import dev.hornet.observability.core.Temperatura
import dev.hornet.observability.core.TelemetriaSource
import dev.hornet.observability.core.ThrottlingClassifier
import dev.hornet.observability.demonstracao.FonteDemonstracao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** O que o ViewModel publica: os dados do core, ainda sem formatação para a tela. */
data class TelemetriaUiState(
    val amostra: AmostraTelemetria? = null,
    val avaliacao: Avaliacao? = null,
    val resultado: ResultadoThrottling? = null,
    val historico: List<Avaliacao> = emptyList(),
    val historicoRazaoFrequencia: List<Double?> = emptyList(),
    val indisponivel: Boolean = false,
    /** Verdadeiro no modo demonstração: os dados são roteirizados, não lidos do aparelho. */
    val simulacao: Boolean = false,
)

/**
 * A cada 5 segundos: lê a amostra, avalia a temperatura com o detector, classifica a amostra
 * e publica o resultado. A leitura roda em thread de IO para não travar a tela.
 *
 * No modo demonstração, a fonte real é trocada por uma fonte roteirizada. O estado vive no
 * ViewModel, então girar a tela não perde o histórico, a carga nem o modo demonstração.
 */
class TelemetriaViewModel(
    private val fonte: TelemetriaSource,
    private val detector: AnomalyDetector,
    private val classificador: ThrottlingClassifier,
    private val despachanteIo: CoroutineDispatcher = Dispatchers.IO,
    private val criarFonteDemonstracao: () -> TelemetriaSource = { FonteDemonstracao() },
) : ViewModel() {

    private val _estado = MutableStateFlow(TelemetriaUiState())
    val estado: StateFlow<TelemetriaUiState> = _estado.asStateFlow()

    // O detector guarda a janela de leituras, então só uma thread pode usá-lo por vez.
    private val trava = Any()
    private var tarefaAmostragem: Job? = null

    // Fonte do modo demonstração; null quando a fonte real está em uso.
    private var fonteDemonstracao: TelemetriaSource? = null

    /**
     * Liga o modo demonstração: zera o detector e os históricos, para os dados simulados não se
     * misturarem com os reais, e já mostra a primeira amostra do roteiro.
     */
    fun iniciarDemonstracao() = synchronized(trava) {
        if (fonteDemonstracao != null) return@synchronized
        fonteDemonstracao = criarFonteDemonstracao()
        recomecar(simulacao = true)
        amostrar()
    }

    /** Volta à fonte real, também com detector e históricos zerados. */
    fun pararDemonstracao() = synchronized(trava) {
        if (fonteDemonstracao == null) return@synchronized
        fonteDemonstracao = null
        recomecar(simulacao = false)
    }

    private fun recomecar(simulacao: Boolean) {
        detector.reiniciar()
        _estado.value = TelemetriaUiState(simulacao = simulacao)
    }

    // Criado só quando usado. Como vive no viewModelScope, a carga para se o ViewModel for encerrado.
    private val gerador by lazy { GeradorDeCarga(viewModelScope) }

    /** Verdadeiro enquanto o gerador de carga estiver ocupando a CPU. */
    val cargaAtiva: StateFlow<Boolean> by lazy { gerador.ativo }

    fun gerarCarga() = gerador.iniciar()

    fun pararCarga() = gerador.parar()

    fun iniciar() {
        if (tarefaAmostragem?.isActive == true) return

        tarefaAmostragem = viewModelScope.launch {
            while (isActive) {
                withContext(despachanteIo) { amostrar() }
                delay(INTERVALO_AMOSTRAGEM_MS)
            }
        }
    }

    /** Faz um ciclo completo: ler, avaliar, classificar e publicar. */
    fun amostrar() = synchronized(trava) {
        val amostra = try {
            (fonteDemonstracao ?: fonte).lerAtual()
        } catch (_: Exception) {
            null
        }
        if (amostra == null) {
            _estado.value = _estado.value.copy(
                amostra = null,
                avaliacao = null,
                resultado = null,
                indisponivel = true,
            )
            return@synchronized
        }

        val avaliacao = detector.avaliar(amostra.temperaturaBateria)
        registrar(amostra, avaliacao)
    }

    /**
     * Repete a última amostra com a temperatura 6 °C acima da última leitura normal, para
     * demonstrar o detector e o classificador sem esquentar o aparelho.
     */
    fun simularPico() = synchronized(trava) {
        val estadoAtual = _estado.value
        val amostraBase = estadoAtual.amostra ?: return@synchronized
        val leituraBase = estadoAtual.historico.lastOrNull {
            it.status == StatusLeitura.NORMAL
        } ?: estadoAtual.avaliacao ?: return@synchronized
        val amostraSimulada = amostraBase.copy(
            temperaturaBateria = Temperatura.deCelsius(
                leituraBase.temperatura.celsius + AUMENTO_SIMULACAO_CELSIUS,
            ),
        )
        registrar(amostraSimulada, detector.avaliar(amostraSimulada.temperaturaBateria))
    }

    private fun registrar(amostra: AmostraTelemetria, avaliacao: Avaliacao) {
        val resultado = classificador.classificar(amostra, avaliacao)
        val historico = (_estado.value.historico + avaliacao).takeLast(LIMITE_HISTORICO)
        val historicoRazao = (_estado.value.historicoRazaoFrequencia + amostra.razaoFrequencia())
            .takeLast(LIMITE_HISTORICO)
        _estado.value = _estado.value.copy(
            amostra = amostra,
            avaliacao = avaliacao,
            resultado = resultado,
            historico = historico,
            historicoRazaoFrequencia = historicoRazao,
            indisponivel = false,
        )
    }

    private companion object {
        const val INTERVALO_AMOSTRAGEM_MS = 5_000L
        const val LIMITE_HISTORICO = 60
        const val AUMENTO_SIMULACAO_CELSIUS = 6.0
    }
}

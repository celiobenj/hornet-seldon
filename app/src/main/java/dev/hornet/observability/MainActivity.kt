package dev.hornet.observability

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hornet.observability.core.AmostraTelemetria
import dev.hornet.observability.core.Avaliacao
import dev.hornet.observability.core.BaselineDetector
import dev.hornet.observability.core.EstadoThrottling
import dev.hornet.observability.core.ResultadoThrottling
import dev.hornet.observability.core.StatusLeitura
import dev.hornet.observability.core.Temperatura
import dev.hornet.observability.core.ThrottlingClassifier
import dev.hornet.observability.data.TelemetriaAndroidSource
import dev.hornet.observability.ui.TelaMultiSinal
import dev.hornet.observability.ui.TelemetriaUiState
import dev.hornet.observability.ui.TelemetriaViewModel
import dev.hornet.observability.ui.mapearParaTela
import dev.hornet.observability.ui.theme.HornetDesafio01Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HornetDesafio01Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TelaPrincipal(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * Classificador provisório, usado até o ClassificadorThrottling (issue B2, #38) chegar ao develop.
 * Sempre responde INDETERMINADO, então a tela mostra "Dados insuficientes" com este motivo.
 * Para ligar o real, troque ClassificadorPendente por ClassificadorThrottling() na fábrica abaixo.
 */
private object ClassificadorPendente : ThrottlingClassifier {
    override fun classificar(
        amostra: AmostraTelemetria,
        avaliacaoTemperatura: Avaliacao,
    ): ResultadoThrottling = ResultadoThrottling(
        estado = EstadoThrottling.INDETERMINADO,
        motivo = "Classificador de throttling ainda não integrado.",
    )
}

@Composable
private fun TelaPrincipal(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current.applicationContext
    val fabrica = remember(contexto) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TelemetriaViewModel::class.java))
                return TelemetriaViewModel(
                    fonte = TelemetriaAndroidSource.deContexto(contexto),
                    detector = BaselineDetector(),
                    classificador = ClassificadorPendente,
                ) as T
            }
        }
    }
    val viewModel: TelemetriaViewModel = viewModel(factory = fabrica)
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.iniciar()
    }

    ConteudoPrincipal(
        estado = estado,
        onSimularPico = viewModel::simularPico,
        modifier = modifier,
    )
}

@Composable
private fun ConteudoPrincipal(
    estado: TelemetriaUiState,
    onSimularPico: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estadoTela = remember(estado) { mapearParaTela(estado) }
    TelaMultiSinal(
        estado = estadoTela,
        historico = estado.historico,
        historicoRazaoFrequencia = estado.historicoRazaoFrequencia,
        modifier = modifier,
        acoes = {
            Button(
                onClick = onSimularPico,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = "Simular pico",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun ConteudoPrincipalPreview() {
    val avaliacao = Avaliacao(Temperatura.deCelsius(31.7), StatusLeitura.NORMAL)
    HornetDesafio01Theme {
        ConteudoPrincipal(
            estado = TelemetriaUiState(
                amostra = AmostraTelemetria(
                    instanteMs = 0L,
                    temperaturaBateria = avaliacao.temperatura,
                    frequenciaAtualKhz = 1_200_000L,
                    frequenciaMaximaKhz = 2_304_000L,
                    statusTermico = 0,
                ),
                avaliacao = avaliacao,
                resultado = ResultadoThrottling(EstadoThrottling.NORMAL, "Nenhum sinal de aquecimento."),
                historico = listOf(
                    Avaliacao(Temperatura.deCelsius(30.0), StatusLeitura.NORMAL),
                    Avaliacao(Temperatura.deCelsius(36.0), StatusLeitura.PICO),
                    avaliacao,
                ),
            ),
            onSimularPico = {},
        )
    }
}

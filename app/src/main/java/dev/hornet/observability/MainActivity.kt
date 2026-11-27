package dev.hornet.observability

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import dev.hornet.observability.core.ClassificadorThrottling
import dev.hornet.observability.core.EstadoThrottling
import dev.hornet.observability.core.ResultadoThrottling
import dev.hornet.observability.core.StatusLeitura
import dev.hornet.observability.core.Temperatura
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
                    classificador = ClassificadorThrottling(),
                ) as T
            }
        }
    }
    val viewModel: TelemetriaViewModel = viewModel(factory = fabrica)
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val cargaAtiva by viewModel.cargaAtiva.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.iniciar()
    }

    ConteudoPrincipal(
        estado = estado,
        cargaAtiva = cargaAtiva,
        onSimularPico = viewModel::simularPico,
        onGerarCarga = viewModel::gerarCarga,
        onPararCarga = viewModel::pararCarga,
        onIniciarDemonstracao = viewModel::iniciarDemonstracao,
        onPararDemonstracao = viewModel::pararDemonstracao,
        modifier = modifier,
    )
}

@Composable
private fun ConteudoPrincipal(
    estado: TelemetriaUiState,
    cargaAtiva: Boolean,
    onSimularPico: () -> Unit,
    onGerarCarga: () -> Unit,
    onPararCarga: () -> Unit,
    onIniciarDemonstracao: () -> Unit,
    onPararDemonstracao: () -> Unit,
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
            if (cargaAtiva) {
                OutlinedButton(onClick = onPararCarga) {
                    Text(
                        text = "Parar carga",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    text = "Carga ativa: a CPU está ocupada de propósito por até 60 s.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Button(onClick = onGerarCarga) {
                    Text(
                        text = "Gerar carga por 60 s",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (estado.simulacao) {
                OutlinedButton(onClick = onPararDemonstracao) {
                    Text(
                        text = "Sair do modo demonstração",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                Button(onClick = onIniciarDemonstracao) {
                    Text(
                        text = "Modo demonstração",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
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
            cargaAtiva = false,
            onSimularPico = {},
            onGerarCarga = {},
            onPararCarga = {},
            onIniciarDemonstracao = {},
            onPararDemonstracao = {},
        )
    }
}

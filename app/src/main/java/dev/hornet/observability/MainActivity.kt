package dev.hornet.observability

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hornet.observability.core.Avaliacao
import dev.hornet.observability.core.BaselineDetector
import dev.hornet.observability.core.StatusLeitura
import dev.hornet.observability.core.Temperatura
import dev.hornet.observability.data.BatteryTemperatureSource
import dev.hornet.observability.ui.TelaTemperatura
import dev.hornet.observability.ui.TemperaturaUiState
import dev.hornet.observability.ui.TemperaturaViewModel
import dev.hornet.observability.ui.theme.HornetDesafio01Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HornetDesafio01Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TelaPrincipalTemperatura(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun TelaPrincipalTemperatura(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current.applicationContext
    val fabrica = remember(contexto) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TemperaturaViewModel::class.java))
                return TemperaturaViewModel(
                    fonte = BatteryTemperatureSource.deContexto(contexto),
                    detector = BaselineDetector(),
                ) as T
            }
        }
    }
    val viewModel: TemperaturaViewModel = viewModel(factory = fabrica)
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.iniciar()
    }

    TelaTemperatura(
        estado = estado,
        onSimularPico = viewModel::simularPico,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun TelaPrincipalTemperaturaPreview() {
    HornetDesafio01Theme {
        TelaTemperatura(
            estado = TemperaturaUiState(
                atual = Avaliacao(
                    temperatura = Temperatura.deCelsius(31.7),
                    status = StatusLeitura.NORMAL,
                ),
                historico = listOf(
                    Avaliacao(Temperatura.deCelsius(30.0), StatusLeitura.NORMAL),
                    Avaliacao(Temperatura.deCelsius(36.0), StatusLeitura.PICO),
                    Avaliacao(Temperatura.deCelsius(31.7), StatusLeitura.NORMAL),
                ),
            ),
            onSimularPico = {},
        )
    }
}

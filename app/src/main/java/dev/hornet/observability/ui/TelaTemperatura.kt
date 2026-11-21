package dev.hornet.observability.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.hornet.observability.core.StatusLeitura

@Composable
fun TelaTemperatura(
    estado: TemperaturaUiState,
    onSimularPico: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val leituraAtual = estado.atual
    val temperaturaTexto = if (estado.indisponivel || leituraAtual == null) {
        "Indisponível"
    } else {
        "${"%.1f".format(leituraAtual.temperatura.celsius)} °C"
    }
    val descricaoTemperatura = if (estado.indisponivel || leituraAtual == null) {
        "Temperatura atual indisponível"
    } else {
        "Temperatura atual: ${"%.1f".format(leituraAtual.temperatura.celsius)} graus Celsius"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Temperatura atual",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = temperaturaTexto,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.semantics {
                contentDescription = descricaoTemperatura
            },
        )
        GraficoTemperatura(
            leituras = estado.historico,
            limiarCelsius = estado.limiarCelsius,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = leituraAtual?.let { textoStatus(it.status) } ?: "Aguardando leitura",
            color = leituraAtual?.let { corStatus(it.status) } ?: Color.Gray,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Limiar: ${"%.1f".format(estado.limiarCelsius)} °C",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Últimas leituras",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        estado.historico.takeLast(10).asReversed().forEach { avaliacao ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${"%.1f".format(avaliacao.temperatura.celsius)} °C",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = textoStatus(avaliacao.status),
                    color = corStatus(avaliacao.status),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Button(
            onClick = onSimularPico,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(
                text = "Simular pico",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun textoStatus(status: StatusLeitura): String = when (status) {
    StatusLeitura.SEM_BASE -> "Aguardando base"
    StatusLeitura.NORMAL -> "Normal"
    StatusLeitura.PICO -> "Pico detectado"
    StatusLeitura.LIMIAR_EXCEDIDO -> "Limiar excedido"
    StatusLeitura.INVALIDA -> "Leitura inválida"
}

private fun corStatus(status: StatusLeitura): Color = when (status) {
    StatusLeitura.SEM_BASE -> Color(0xFF616161)
    StatusLeitura.NORMAL -> Color(0xFF2E7D32)
    StatusLeitura.PICO, StatusLeitura.LIMIAR_EXCEDIDO, StatusLeitura.INVALIDA -> Color(0xFFC62828)
}

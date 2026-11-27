package dev.hornet.observability.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hornet.observability.core.Avaliacao
import dev.hornet.observability.core.StatusLeitura
import dev.hornet.observability.core.Temperatura
import dev.hornet.observability.ui.theme.HornetDesafio01Theme

/**
 * Tela da v2: um card por sinal, o destaque do estado do throttling e o gráfico de temperatura.
 *
 * O histórico do gráfico vem separado do modelo porque o GraficoTemperatura usa a Avaliacao do core.
 * acoes recebe os botões de teste (por exemplo "Simular pico"), mostrados no fim da tela.
 */
@Composable
fun TelaMultiSinal(
    estado: EstadoTelaMultiSinal,
    historico: List<Avaliacao>,
    modifier: Modifier = Modifier,
    limiarCelsius: Double = 45.0,
    historicoRazaoFrequencia: List<Double?> = emptyList(),
    acoes: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DestaqueEstado(estado = estado.estado, motivo = estado.motivo)
        CardSinal(
            titulo = "Temperatura da bateria",
            valor = estado.temperaturaTexto ?: INDISPONIVEL,
        )
        CardFrequencia(estado = estado)
        CardSinal(
            titulo = "Uso da CPU",
            valor = estado.usoCpuPercentual?.let { "${"%.0f".format(it)} %" } ?: INDISPONIVEL,
            // Sem acesso ao uso do sistema, o adaptador mede só este app (docs/validacao/uso-cpu.md).
            observacao = "Em muitos aparelhos, mede só o uso deste app.",
        )
        CardSinal(
            titulo = "Status térmico do Android",
            valor = estado.statusTermico ?: INDISPONIVEL,
        )
        Text(
            text = "Histórico da temperatura",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        GraficoTemperatura(
            leituras = historico,
            limiarCelsius = limiarCelsius,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Frequência da CPU (% do máximo)",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        GraficoRazaoFrequencia(
            razoes = historicoRazaoFrequencia,
            modifier = Modifier.fillMaxWidth(),
        )
        acoes()
    }
}

@Composable
private fun DestaqueEstado(estado: EstadoThrottlingTela, motivo: String) {
    val cor = corEstado(estado)
    val descricao = descricaoEstado(estado, motivo)
    // O leitor de tela lê o card inteiro como uma frase só e anuncia quando o estado muda.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = descricao
                liveRegion = LiveRegionMode.Polite
            },
        colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = textoEstado(estado),
                color = cor,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (motivo.isNotBlank()) {
                Text(
                    text = motivo,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun CardSinal(titulo: String, valor: String, observacao: String? = null) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (observacao != null) {
                Text(
                    text = observacao,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CardFrequencia(estado: EstadoTelaMultiSinal) {
    val atual = estado.frequenciaAtualMhz
    val maxima = estado.frequenciaMaximaMhz
    val razao = estado.razaoFrequencia
    val valor = when {
        atual != null && maxima != null -> "$atual de $maxima MHz"
        atual != null -> "$atual MHz (máximo indisponível)"
        else -> INDISPONIVEL
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Frequência da CPU",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (razao != null && razao.isFinite()) {
                LinearProgressIndicator(
                    progress = { razao.coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
                Text(
                    text = "${"%.0f".format(razao * 100)} % do máximo",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "Percentual do máximo $INDISPONIVEL",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun textoEstado(estado: EstadoThrottlingTela): String = when (estado) {
    EstadoThrottlingTela.NORMAL -> "Normal"
    EstadoThrottlingTela.AQUECENDO -> "Aquecendo"
    EstadoThrottlingTela.THROTTLING_PROVAVEL -> "Throttling provável"
    EstadoThrottlingTela.THROTTLING_CONFIRMADO -> "Throttling confirmado"
    EstadoThrottlingTela.DADOS_INSUFICIENTES -> "Dados insuficientes"
}

/** Frase lida pelo leitor de tela no destaque do estado. */
internal fun descricaoEstado(estado: EstadoThrottlingTela, motivo: String): String = buildString {
    append("Estado do throttling: ${textoEstado(estado)}.")
    if (motivo.isNotBlank()) {
        append(" Motivo: ${motivo.trim().removeSuffix(".")}.")
    }
}

/**
 * Cor do estado. No modo escuro usa tons claros, porque os tons escuros do modo claro
 * ficam com pouco contraste sobre fundo escuro.
 */
@Composable
private fun corEstado(estado: EstadoThrottlingTela): Color {
    val escuro = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (escuro) {
        when (estado) {
            EstadoThrottlingTela.NORMAL -> Color(0xFF81C784)
            EstadoThrottlingTela.AQUECENDO -> Color(0xFFFFB74D)
            EstadoThrottlingTela.THROTTLING_PROVAVEL -> Color(0xFFFF8A65)
            EstadoThrottlingTela.THROTTLING_CONFIRMADO -> Color(0xFFEF9A9A)
            EstadoThrottlingTela.DADOS_INSUFICIENTES -> Color(0xFFBDBDBD)
        }
    } else {
        when (estado) {
            EstadoThrottlingTela.NORMAL -> Color(0xFF2E7D32)
            EstadoThrottlingTela.AQUECENDO -> Color(0xFF8A5300)
            EstadoThrottlingTela.THROTTLING_PROVAVEL -> Color(0xFFBF360C)
            EstadoThrottlingTela.THROTTLING_CONFIRMADO -> Color(0xFFC62828)
            EstadoThrottlingTela.DADOS_INSUFICIENTES -> Color(0xFF616161)
        }
    }
}

private const val INDISPONIVEL = "indisponível"

// Dados falsos, usados só nas previews.

private fun historicoFalso(vararg celsius: Double): List<Avaliacao> =
    celsius.map { valor ->
        Avaliacao(
            temperatura = Temperatura.deCelsius(valor),
            status = if (valor >= 45.0) StatusLeitura.LIMIAR_EXCEDIDO else StatusLeitura.NORMAL,
        )
    }

private val HISTORICO_FRIO = historicoFalso(31.0, 31.2, 31.1, 31.4, 31.3, 31.5, 31.4, 31.6)
private val HISTORICO_QUENTE = historicoFalso(34.0, 35.1, 36.0, 36.8, 37.5, 38.3, 39.2, 40.1)

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun TelaMultiSinalNormalPreview() {
    TelaMultiSinal(
        estado = EstadoTelaMultiSinal(
            temperaturaTexto = "31,6 °C",
            frequenciaAtualMhz = 1000,
            frequenciaMaximaMhz = 2000,
            razaoFrequencia = 0.5,
            usoCpuPercentual = 12.0,
            statusTermico = "Nenhum (0)",
            estado = EstadoThrottlingTela.NORMAL,
            motivo = "Nenhum sinal indica aquecimento ou limitação de frequência.",
        ),
        historico = HISTORICO_FRIO,
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun TelaMultiSinalAquecendoPreview() {
    TelaMultiSinal(
        estado = EstadoTelaMultiSinal(
            temperaturaTexto = "38,3 °C",
            frequenciaAtualMhz = 1800,
            frequenciaMaximaMhz = 2000,
            razaoFrequencia = 0.9,
            usoCpuPercentual = 55.0,
            statusTermico = "Leve (1)",
            estado = EstadoThrottlingTela.AQUECENDO,
            motivo = "A bateria atingiu 38,3 °C.",
        ),
        historico = HISTORICO_QUENTE.dropLast(2),
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun TelaMultiSinalThrottlingProvavelPreview() {
    TelaMultiSinal(
        estado = EstadoTelaMultiSinal(
            temperaturaTexto = "38,3 °C",
            frequenciaAtualMhz = 1380,
            frequenciaMaximaMhz = 2000,
            razaoFrequencia = 0.69,
            usoCpuPercentual = 85.0,
            statusTermico = "Nenhum (0)",
            estado = EstadoThrottlingTela.THROTTLING_PROVAVEL,
            motivo = "CPU com uso de 85 % em 69 % da frequência máxima e bateria a 38,3 °C.",
        ),
        historico = HISTORICO_QUENTE.dropLast(2),
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun TelaMultiSinalThrottlingConfirmadoPreview() {
    TelaMultiSinal(
        estado = EstadoTelaMultiSinal(
            temperaturaTexto = "40,1 °C",
            frequenciaAtualMhz = 1200,
            frequenciaMaximaMhz = 2000,
            razaoFrequencia = 0.6,
            usoCpuPercentual = 92.0,
            statusTermico = "Moderado (2)",
            estado = EstadoThrottlingTela.THROTTLING_CONFIRMADO,
            motivo = "O Android reportou nível térmico 2 (moderado).",
        ),
        historico = HISTORICO_QUENTE,
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun TelaMultiSinalDadosInsuficientesPreview() {
    TelaMultiSinal(
        estado = EstadoTelaMultiSinal(
            estado = EstadoThrottlingTela.DADOS_INSUFICIENTES,
            motivo = "Ainda não há temperatura válida para classificar.",
        ),
        historico = emptyList(),
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun TelaMultiSinalSinaisIndisponiveisPreview() {
    TelaMultiSinal(
        estado = EstadoTelaMultiSinal(
            temperaturaTexto = "33,0 °C",
            frequenciaAtualMhz = 1500,
            frequenciaMaximaMhz = 2000,
            razaoFrequencia = 0.75,
            usoCpuPercentual = null,
            statusTermico = null,
            estado = EstadoThrottlingTela.NORMAL,
            motivo = "Uso da CPU e status térmico indisponíveis neste aparelho.",
        ),
        historico = HISTORICO_FRIO,
    )
}

@Preview(showBackground = true, heightDp = 1000, backgroundColor = 0xFF121212)
@Composable
private fun TelaMultiSinalModoEscuroPreview() {
    HornetDesafio01Theme(darkTheme = true, dynamicColor = false) {
        Surface {
            TelaMultiSinal(
                estado = EstadoTelaMultiSinal(
                    temperaturaTexto = "38,3 °C",
                    frequenciaAtualMhz = 1380,
                    frequenciaMaximaMhz = 2000,
                    razaoFrequencia = 0.69,
                    usoCpuPercentual = 85.0,
                    statusTermico = "Nenhum (0)",
                    estado = EstadoThrottlingTela.THROTTLING_PROVAVEL,
                    motivo = "CPU com uso de 85 % em 69 % da frequência máxima e bateria a 38,3 °C.",
                ),
                historico = HISTORICO_QUENTE.dropLast(2),
            )
        }
    }
}

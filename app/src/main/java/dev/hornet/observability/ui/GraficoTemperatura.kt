package dev.hornet.observability.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hornet.observability.core.Avaliacao
import dev.hornet.observability.core.StatusLeitura
import dev.hornet.observability.core.Temperatura

@Composable
fun GraficoTemperatura(
    leituras: List<Avaliacao>,
    limiarCelsius: Double = LIMIAR_PADRAO_CELSIUS,
    modifier: Modifier = Modifier,
) {
    val serie = leituras.takeLast(LIMITE_LEITURAS)
    val valoresValidos = serie.map { it.temperatura.celsius }.filter { it.isFinite() }
    val minimo = minOf(MINIMO_PADRAO_CELSIUS, valoresValidos.minOrNull() ?: MINIMO_PADRAO_CELSIUS)
    val maximo = maxOf(limiarCelsius + MARGEM_SUPERIOR_CELSIUS, valoresValidos.maxOrNull() ?: limiarCelsius + MARGEM_SUPERIOR_CELSIUS)
    val amplitude = maximo - minimo
    val quantidadeAnomalias = serie.count { it.anomala }
    val temperaturaAtual = serie.lastOrNull()?.temperatura?.celsius?.takeIf { it.isFinite() }
    val descricao = buildString {
        append("Gráfico com ${serie.size} leituras.")
        if (temperaturaAtual == null) {
            append(" Temperatura atual indisponível.")
        } else {
            append(" Temperatura atual: ${"%.1f".format(temperaturaAtual)} graus Celsius.")
        }
        val rotuloAnomalias = if (quantidadeAnomalias == 1) "anomalia" else "anomalias"
        append(" $quantidadeAnomalias $rotuloAnomalias.")
    }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${"%.1f".format(maximo)} °C",
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = "Limiar: ${"%.1f".format(limiarCelsius)} °C",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 6.dp)
                .semantics { contentDescription = descricao },
        ) {
            val yLimiar = size.height * ((maximo - limiarCelsius) / amplitude).toFloat()
            drawLine(
                color = Color(0xFF757575),
                start = Offset(0f, yLimiar),
                end = Offset(size.width, yLimiar),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())),
            )

            val caminho = Path()
            var iniciouCaminho = false
            serie.forEachIndexed { indice, avaliacao ->
                val valor = avaliacao.temperatura.celsius
                if (valor.isFinite()) {
                    val x = coordenadaX(indice, serie.size, size.width)
                    val y = size.height * ((maximo - valor) / amplitude).toFloat()
                    if (!iniciouCaminho) {
                        caminho.moveTo(x, y)
                        iniciouCaminho = true
                    } else {
                        caminho.lineTo(x, y)
                    }
                }
            }
            if (iniciouCaminho) {
                drawPath(
                    path = caminho,
                    color = COR_SERIE,
                    style = Stroke(width = 2.5.dp.toPx()),
                )
            }

            serie.forEachIndexed { indice, avaliacao ->
                val valor = avaliacao.temperatura.celsius
                if (valor.isFinite()) {
                    val x = coordenadaX(indice, serie.size, size.width)
                    val y = size.height * ((maximo - valor) / amplitude).toFloat()
                    if (avaliacao.anomala) {
                        val raio = 5.dp.toPx()
                        val losango = Path().apply {
                            moveTo(x, y - raio)
                            lineTo(x + raio, y)
                            lineTo(x, y + raio)
                            lineTo(x - raio, y)
                            close()
                        }
                        drawPath(path = losango, color = COR_ANOMALIA)
                    } else {
                        drawCircle(
                            color = COR_SERIE,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y),
                        )
                    }
                }
            }
        }
        Text(
            text = "${"%.1f".format(minimo)} °C",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/** Posição horizontal da leitura; também usada pelo GraficoRazaoFrequencia. */
internal fun coordenadaX(indice: Int, quantidade: Int, largura: Float): Float =
    if (quantidade <= 1) largura / 2f else largura * indice / (quantidade - 1)

private const val LIMITE_LEITURAS = 60
private const val MINIMO_PADRAO_CELSIUS = 25.0
private const val LIMIAR_PADRAO_CELSIUS = 45.0
private const val MARGEM_SUPERIOR_CELSIUS = 2.0
private val COR_SERIE = Color(0xFF1565C0)
private val COR_ANOMALIA = Color(0xFFC62828)

@Preview(showBackground = true)
@Composable
private fun GraficoTemperaturaPreview() {
    val leituras = listOf(29.8, 30.1, 30.3, 36.0, 31.1, 31.3, 46.0, 32.0).mapIndexed { indice, celsius ->
        Avaliacao(
            temperatura = Temperatura.deCelsius(celsius),
            status = when (indice) {
                3 -> StatusLeitura.PICO
                6 -> StatusLeitura.LIMIAR_EXCEDIDO
                else -> StatusLeitura.NORMAL
            },
        )
    }
    GraficoTemperatura(leituras = leituras)
}

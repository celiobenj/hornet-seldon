package dev.hornet.observability.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
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

/**
 * Gráfico da frequência da CPU em percentual do máximo (0 a 100%), nas últimas 60 leituras.
 *
 * Razão null (sinal indisponível) não vira zero: a linha é interrompida naquele ponto.
 * Valores acima de 1 (teto maior que o máximo) ficam presos no topo do gráfico.
 */
@Composable
fun GraficoRazaoFrequencia(
    razoes: List<Double?>,
    modifier: Modifier = Modifier,
) {
    val serie = razoes.takeLast(LIMITE_LEITURAS)
        .map { razao -> razao?.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) }
    val atual = serie.lastOrNull()
    val descricao = buildString {
        append("Gráfico da frequência da CPU com ${serie.size} leituras.")
        if (atual == null) {
            append(" Frequência atual indisponível.")
        } else {
            append(" Frequência atual: ${"%.0f".format(atual * 100)} por cento do máximo.")
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "100 %",
            style = MaterialTheme.typography.labelSmall,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 6.dp)
                .semantics { contentDescription = descricao },
        ) {
            val yMetade = size.height / 2f
            drawLine(
                color = Color(0xFF757575),
                start = Offset(0f, yMetade),
                end = Offset(size.width, yMetade),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())),
            )

            // Cada trecho contínuo de leituras válidas vira uma linha; um null começa um trecho novo.
            val caminho = Path()
            var emTrecho = false
            serie.forEachIndexed { indice, razao ->
                if (razao == null) {
                    emTrecho = false
                    return@forEachIndexed
                }
                val x = coordenadaX(indice, serie.size, size.width)
                val y = size.height * (1f - razao.toFloat())
                if (emTrecho) caminho.lineTo(x, y) else caminho.moveTo(x, y)
                emTrecho = true
                drawCircle(color = COR_FREQUENCIA, radius = 3.dp.toPx(), center = Offset(x, y))
            }
            drawPath(path = caminho, color = COR_FREQUENCIA, style = Stroke(width = 2.5.dp.toPx()))
        }
        Text(
            text = "0 %",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private const val LIMITE_LEITURAS = 60
private val COR_FREQUENCIA = Color(0xFF6A1B9A)

@Preview(showBackground = true)
@Composable
private fun GraficoRazaoFrequenciaPreview() {
    GraficoRazaoFrequencia(
        razoes = listOf(0.45, 0.52, 0.9, 1.0, 0.95, null, null, 0.7, 0.62, 0.58),
    )
}

package dev.hornet.observability.core

import java.util.Random

/**
 * Conjunto de leituras de temperatura geradas artificialmente para testar o detector.
 *
 * @property leituras sequencia ordenada de temperaturas simuladas em graus Celsius.
 * @property indicesDePico posicoes da lista em que um salto de temperatura foi injetado.
 */
data class SerieSintetica(
    val leituras: List<Temperatura>,
    val indicesDePico: Set<Int>,
)

/**
 * Gerador deterministico de series sinteticas de temperatura da bateria.
 *
 * Usa uma semente fixa no gerador pseudoaleatorio para que os testes sempre
 * reproduzam exatamente os mesmos valores.
 */
object GeradorSerieSintetica {

    /**
     * Gera uma serie sintetica com ruido gaussiano arredondado para decimos de grau Celsius
     * (mesma resolucao do sensor do Android) e picos injetados nos indices informados.
     */
    fun gerar(
        semente: Long = 42,
        tamanho: Int = 200,
        base: Double = 30.0,
        ruido: Double = 0.3,
        salto: Double = 6.0,
        indicesDePico: List<Int> = listOf(40, 80, 120, 150, 180),
    ): SerieSintetica {
        val random = Random(semente)
        val picosNoIntervalo = indicesDePico.filter { it in 0 until tamanho }.toSet()

        val leituras = List(tamanho) { indice ->
            val deslocamento = random.nextGaussian() * ruido
            val valorBruto = if (indice in picosNoIntervalo) {
                base + salto + deslocamento
            } else {
                base + deslocamento
            }
            val valorArredondado = Math.round(valorBruto * 10) / 10.0
            Temperatura.deCelsius(valorArredondado)
        }

        return SerieSintetica(
            leituras = leituras,
            indicesDePico = picosNoIntervalo,
        )
    }
}

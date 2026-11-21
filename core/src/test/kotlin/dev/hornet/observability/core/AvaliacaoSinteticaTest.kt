package dev.hornet.observability.core

import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Avaliacao da metrica de sucesso do detector sobre a serie sintetica padrao
 * (caso CT-17 de docs/testes.md e secao 6 de docs/escopo.md).
 */
class AvaliacaoSinteticaTest {

    @Test
    fun `detector atinge meta de recall e taxa de falsos positivos na serie sintetica padrao`() {
        val serie = GeradorSerieSintetica.gerar()
        val detector = BaselineDetector()
        val avaliacoes = serie.leituras.map { leitura -> detector.avaliar(leitura) }

        val picosDetectados = serie.indicesDePico.count { indice ->
            avaliacoes[indice].status == StatusLeitura.PICO
        }
        val recall = picosDetectados / 5.0

        val indicesNaoPicoAposCalibracao = avaliacoes.indices.filter { indice ->
            indice >= 10 && indice !in serie.indicesDePico
        }
        val falsosPositivos = indicesNaoPicoAposCalibracao.count { indice ->
            val status = avaliacoes[indice].status
            status == StatusLeitura.PICO || status == StatusLeitura.LIMIAR_EXCEDIDO
        }
        val totalNaoPicoAposCalibracao = indicesNaoPicoAposCalibracao.size
        val taxaFalsosPositivos = falsosPositivos.toDouble() / totalNaoPicoAposCalibracao

        println(
            "Recall: ${"%.2f".format(Locale.US, recall * 100)}% " +
                "($picosDetectados de 5 picos detectados, valor: $recall)",
        )
        println(
            "Taxa de falsos positivos: ${"%.2f".format(Locale.US, taxaFalsosPositivos * 100)}% " +
                "($falsosPositivos de $totalNaoPicoAposCalibracao leituras, valor: $taxaFalsosPositivos)",
        )

        assertTrue(
            "Recall abaixo da meta de 0.9: $recall ($picosDetectados de 5 picos detectados)",
            recall >= 0.9,
        )
        assertTrue(
            "Taxa de falsos positivos acima da meta de 0.05: $taxaFalsosPositivos " +
                "($falsosPositivos de $totalNaoPicoAposCalibracao leituras)",
            taxaFalsosPositivos <= 0.05,
        )
    }
}

package dev.hornet.observability.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitarios do gerador de serie sintetica (caso CT-18 de docs/testes.md).
 */
class SerieSinteticaTest {

    @Test
    fun `mesma semente gera series identicas`() {
        val primeira = GeradorSerieSintetica.gerar(semente = 42)
        val segunda = GeradorSerieSintetica.gerar(semente = 42)

        assertEquals(primeira, segunda)
    }

    @Test
    fun `serie padrao tem tamanho 200 e 5 indices de pico`() {
        val serie = GeradorSerieSintetica.gerar()

        assertEquals(200, serie.leituras.size)
        assertEquals(setOf(40, 80, 120, 150, 180), serie.indicesDePico)
    }

    @Test
    fun `indices de pico ficam acima de base mais 4 graus`() {
        val base = 30.0
        val serie = GeradorSerieSintetica.gerar(base = base)

        for (indice in serie.indicesDePico) {
            val valor = serie.leituras[indice].celsius
            assertTrue(
                "Esperava leitura de pico no indice $indice acima de ${base + 4.0}, mas veio $valor",
                valor > base + 4.0,
            )
        }
    }

    @Test
    fun `demais leituras ficam entre base menos 2 e base mais 2 graus`() {
        val base = 30.0
        val serie = GeradorSerieSintetica.gerar(base = base)

        for ((indice, leitura) in serie.leituras.withIndex()) {
            if (indice in serie.indicesDePico) continue
            val valor = leitura.celsius
            assertTrue(
                "Esperava leitura normal no indice $indice entre ${base - 2.0} e ${base + 2.0}, mas veio $valor",
                valor in (base - 2.0)..(base + 2.0),
            )
        }
    }
}

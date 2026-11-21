package dev.hornet.observability.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class JanelaMovelTest {

    private val tolerancia = 1e-9

    @Test
    fun `calcula media e desvio populacional de leituras alternadas`() {
        val janela = JanelaMovel(capacidade = 10)
        repeat(5) {
            janela.adicionar(30.0)
            janela.adicionar(31.0)
        }

        assertEquals(10, janela.tamanho)
        assertTrue(janela.cheia)
        assertEquals(30.5, janela.media(), tolerancia)
        assertEquals(0.5, janela.desvioPopulacional(), tolerancia)
    }

    @Test
    fun `substitui a leitura mais antiga quando a janela enche`() {
        val janela = JanelaMovel(capacidade = 2)
        janela.adicionar(30.0)
        assertEquals(1, janela.tamanho)
        assertFalse(janela.cheia)

        janela.adicionar(32.0)
        janela.adicionar(36.0)

        assertEquals(2, janela.tamanho)
        assertTrue(janela.cheia)
        assertEquals(34.0, janela.media(), tolerancia)
        assertEquals(2.0, janela.desvioPopulacional(), tolerancia)
    }

    @Test
    fun `limpa a janela e permite preencher de novo`() {
        val janela = JanelaMovel(capacidade = 2)
        janela.adicionar(30.0)
        janela.adicionar(32.0)

        janela.limpar()

        assertEquals(0, janela.tamanho)
        assertFalse(janela.cheia)

        janela.adicionar(36.0)

        assertEquals(36.0, janela.media(), tolerancia)
        assertEquals(0.0, janela.desvioPopulacional(), tolerancia)
    }

    @Test
    fun `rejeita capacidade zero`() {
        assertThrows(IllegalArgumentException::class.java) { JanelaMovel(capacidade = 0) }
    }

    @Test
    fun `rejeita calculo de estatisticas sem leituras`() {
        val janela = JanelaMovel(capacidade = 2)

        assertThrows(IllegalStateException::class.java) { janela.media() }
        assertThrows(IllegalStateException::class.java) { janela.desvioPopulacional() }
    }
}

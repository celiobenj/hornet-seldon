package dev.hornet.observability.core

import kotlin.math.sqrt

internal class JanelaMovel(private val capacidade: Int) {

    init {
        require(capacidade > 0) { "A capacidade da janela precisa ser positiva" }
    }

    private val leituras = ArrayDeque<Double>()

    val tamanho: Int
        get() = leituras.size

    val cheia: Boolean
        get() = tamanho == capacidade

    fun adicionar(valor: Double) {
        if (cheia) leituras.removeFirst()
        leituras.addLast(valor)
    }

    fun media(): Double {
        check(leituras.isNotEmpty()) { "A janela precisa ter pelo menos uma leitura" }
        return leituras.average()
    }

    fun desvioPopulacional(): Double {
        val mediaJanela = media()
        val somaQuadrados = leituras.sumOf { leitura ->
            val diferenca = leitura - mediaJanela
            diferenca * diferenca
        }
        return sqrt(somaQuadrados / tamanho)
    }

    fun limpar() {
        leituras.clear()
    }
}

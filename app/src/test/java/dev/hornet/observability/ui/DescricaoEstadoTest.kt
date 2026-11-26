package dev.hornet.observability.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DescricaoEstadoTest {

    @Test
    fun descreveEstadoComMotivo() {
        val descricao = descricaoEstado(
            EstadoThrottlingTela.THROTTLING_CONFIRMADO,
            "O Android reportou nível térmico 2 (moderado).",
        )

        assertEquals(
            "Estado do throttling: Throttling confirmado. Motivo: O Android reportou nível térmico 2 (moderado).",
            descricao,
        )
    }

    @Test
    fun omiteMotivoVazio() {
        val descricao = descricaoEstado(EstadoThrottlingTela.DADOS_INSUFICIENTES, " ")

        assertEquals("Estado do throttling: Dados insuficientes.", descricao)
    }
}

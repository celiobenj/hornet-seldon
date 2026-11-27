package dev.hornet.observability.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class StatusTermicoSourceTest {

    @Test
    fun retornaNuloAbaixoDaApi29SemAcionarLeitor() {
        var leitorAcionado = false
        val fonte = StatusTermicoSource(
            versaoSdk = StatusTermicoSource.API_MINIMA_STATUS_TERMICO - 1,
            lerStatus = {
                leitorAcionado = true
                2
            },
        )

        assertNull(fonte.lerAtual())
        assertFalse(leitorAcionado)
    }

    @Test
    fun retornaCodigosValidosNaApi29OuSuperior() {
        val statusZero = StatusTermicoSource(versaoSdk = 29, lerStatus = { 0 })
        val statusModerado = StatusTermicoSource(versaoSdk = 31, lerStatus = { 2 })
        val statusMaximo = StatusTermicoSource(versaoSdk = 34, lerStatus = { 6 })

        assertEquals(0, statusZero.lerAtual())
        assertEquals(2, statusModerado.lerAtual())
        assertEquals(6, statusMaximo.lerAtual())
    }

    @Test
    fun retornaNuloParaCodigoForaDaFaixaValida() {
        assertNull(StatusTermicoSource(versaoSdk = 29, lerStatus = { -1 }).lerAtual())
        assertNull(StatusTermicoSource(versaoSdk = 29, lerStatus = { 7 }).lerAtual())
        assertNull(StatusTermicoSource(versaoSdk = 31, lerStatus = { Int.MIN_VALUE }).lerAtual())
        assertNull(StatusTermicoSource(versaoSdk = 31, lerStatus = { Int.MAX_VALUE }).lerAtual())
    }

    @Test
    fun retornaNuloQuandoLeitorDevolveNulo() {
        val fonte = StatusTermicoSource(versaoSdk = 31, lerStatus = { null })

        assertNull(fonte.lerAtual())
    }

    @Test
    fun retornaNuloSemExcecaoQuandoLeitorFalha() {
        val falhaPermissao = StatusTermicoSource(
            versaoSdk = 31,
            lerStatus = { throw SecurityException("Sem permissao para PowerManager") },
        )
        val falhaInesperada = StatusTermicoSource(
            versaoSdk = 31,
            lerStatus = { throw RuntimeException("Servico termico indisponivel") },
        )

        assertNull(falhaPermissao.lerAtual())
        assertNull(falhaInesperada.lerAtual())
    }
}

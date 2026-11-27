package dev.hornet.observability.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsoCpuSourceTest {

    private val tolerancia = 0.0001

    @Test
    fun calculaPercentualDeUsoEmCenarioNormal() {
        assertEquals(
            5.0,
            calcularUsoCpuProcessoPercentual(deltaCpuMs = 200L, deltaTempoMs = 1_000L, processadores = 4)!!,
            tolerancia,
        )
        assertEquals(
            75.0,
            calcularUsoCpuProcessoPercentual(deltaCpuMs = 3_000L, deltaTempoMs = 1_000L, processadores = 4)!!,
            tolerancia,
        )
        assertEquals(
            0.0,
            calcularUsoCpuProcessoPercentual(deltaCpuMs = 0L, deltaTempoMs = 1_000L, processadores = 4)!!,
            tolerancia,
        )
    }

    @Test
    fun limitaPercentualEmCemQuandoDeltaCpuSuperaCapacidade() {
        val resultado = calcularUsoCpuProcessoPercentual(
            deltaCpuMs = 5_000L,
            deltaTempoMs = 1_000L,
            processadores = 4,
        )

        assertEquals(100.0, resultado!!, tolerancia)
    }

    @Test
    fun calculaRetornaNuloQuandoSemDiferencaDeTempo() {
        assertNull(calcularUsoCpuProcessoPercentual(deltaCpuMs = 0L, deltaTempoMs = 0L, processadores = 4))
        assertNull(calcularUsoCpuProcessoPercentual(deltaCpuMs = 100L, deltaTempoMs = 0L, processadores = 4))
    }

    @Test
    fun calculaRetornaNuloQuandoContadorVoltaAtrasOuProcessadoresInvalidos() {
        assertNull(calcularUsoCpuProcessoPercentual(deltaCpuMs = -50L, deltaTempoMs = 1_000L, processadores = 4))
        assertNull(calcularUsoCpuProcessoPercentual(deltaCpuMs = 100L, deltaTempoMs = -1_000L, processadores = 4))
        assertNull(calcularUsoCpuProcessoPercentual(deltaCpuMs = 100L, deltaTempoMs = 1_000L, processadores = 0))
        assertNull(calcularUsoCpuProcessoPercentual(deltaCpuMs = 100L, deltaTempoMs = 1_000L, processadores = -2))
    }

    @Test
    fun primeiraLeituraRetornaNuloEIndicaPlanoB() {
        val fonte = UsoCpuSource(
            lerTempoCpuProcessoMs = { 100L },
            lerTempoDecorridoMs = { 1_000L },
            lerQuantidadeProcessadores = { 4 },
        )

        assertTrue(fonte.medeSomenteProprioProcesso)
        assertNull(fonte.lerAtual())
    }

    @Test
    fun duasLeiturasConsecutivasNormaisCalculamPercentualEsperado() {
        val amostrasCpu = ArrayDeque(listOf(100L, 1_100L, 3_100L))
        val amostrasTempo = ArrayDeque(listOf(1_000L, 2_000L, 3_000L))
        val fonte = UsoCpuSource(
            lerTempoCpuProcessoMs = { amostrasCpu.removeFirst() },
            lerTempoDecorridoMs = { amostrasTempo.removeFirst() },
            lerQuantidadeProcessadores = { 4 },
        )

        assertNull(fonte.lerAtual())
        assertEquals(25.0, fonte.lerAtual()!!, tolerancia)
        assertEquals(50.0, fonte.lerAtual()!!, tolerancia)
    }

    @Test
    fun duasLeiturasSemDiferencaDeTempoRetornamNulo() {
        val amostrasCpu = ArrayDeque(listOf(100L, 100L, 250L))
        val amostrasTempo = ArrayDeque(listOf(1_000L, 1_000L, 1_000L))
        val fonte = UsoCpuSource(
            lerTempoCpuProcessoMs = { amostrasCpu.removeFirst() },
            lerTempoDecorridoMs = { amostrasTempo.removeFirst() },
            lerQuantidadeProcessadores = { 4 },
        )

        assertNull(fonte.lerAtual())
        assertNull(fonte.lerAtual())
        assertNull(fonte.lerAtual())
    }

    @Test
    fun contadorDeCpuQueVoltaAtrasRetornaNuloERecuperaDepois() {
        val amostrasCpu = ArrayDeque(listOf(500L, 400L, 800L))
        val amostrasTempo = ArrayDeque(listOf(1_000L, 2_000L, 3_000L))
        val fonte = UsoCpuSource(
            lerTempoCpuProcessoMs = { amostrasCpu.removeFirst() },
            lerTempoDecorridoMs = { amostrasTempo.removeFirst() },
            lerQuantidadeProcessadores = { 4 },
        )

        assertNull(fonte.lerAtual())
        assertNull(fonte.lerAtual())
        assertEquals(10.0, fonte.lerAtual()!!, tolerancia)
    }

    @Test
    fun contadorDeTempoQueVoltaAtrasRetornaNuloERecuperaDepois() {
        val amostrasCpu = ArrayDeque(listOf(500L, 600L, 1_000L))
        val amostrasTempo = ArrayDeque(listOf(2_000L, 1_500L, 2_500L))
        val fonte = UsoCpuSource(
            lerTempoCpuProcessoMs = { amostrasCpu.removeFirst() },
            lerTempoDecorridoMs = { amostrasTempo.removeFirst() },
            lerQuantidadeProcessadores = { 4 },
        )

        assertNull(fonte.lerAtual())
        assertNull(fonte.lerAtual())
        assertEquals(10.0, fonte.lerAtual()!!, tolerancia)
    }

    @Test
    fun leituraNegativaRetornaNuloSemPerderReferenciaValidaAnterior() {
        val amostrasCpu = ArrayDeque(listOf(100L, -1L, 500L))
        val amostrasTempo = ArrayDeque(listOf(1_000L, 2_000L, 3_000L))
        val fonte = UsoCpuSource(
            lerTempoCpuProcessoMs = { amostrasCpu.removeFirst() },
            lerTempoDecorridoMs = { amostrasTempo.removeFirst() },
            lerQuantidadeProcessadores = { 4 },
        )

        assertNull(fonte.lerAtual())
        assertNull(fonte.lerAtual())
        // Delta entre (100, 1000) e (500, 3000) com 4 processadores: 400 / 8000 * 100 = 5.0%.
        assertEquals(5.0, fonte.lerAtual()!!, tolerancia)
    }

    @Test
    fun retornaNuloQuandoQuantidadeDeProcessadoresInvalida() {
        val amostrasCpu = ArrayDeque(listOf(100L, 500L))
        val amostrasTempo = ArrayDeque(listOf(1_000L, 2_000L))
        val fonte = UsoCpuSource(
            lerTempoCpuProcessoMs = { amostrasCpu.removeFirst() },
            lerTempoDecorridoMs = { amostrasTempo.removeFirst() },
            lerQuantidadeProcessadores = { 0 },
        )

        assertNull(fonte.lerAtual())
        assertNull(fonte.lerAtual())
    }

    @Test
    fun retornaNuloSemExcecaoQuandoLeitoresLancamExcecao() {
        val falhaCpu = UsoCpuSource(
            lerTempoCpuProcessoMs = { throw SecurityException("Acesso negado") },
            lerTempoDecorridoMs = { 1_000L },
            lerQuantidadeProcessadores = { 4 },
        )
        val falhaTempo = UsoCpuSource(
            lerTempoCpuProcessoMs = { 100L },
            lerTempoDecorridoMs = { throw IllegalStateException("Relogio indisponivel") },
            lerQuantidadeProcessadores = { 4 },
        )
        val amostrasCpu = ArrayDeque(listOf(100L, 500L))
        val amostrasTempo = ArrayDeque(listOf(1_000L, 2_000L))
        val falhaProcessadores = UsoCpuSource(
            lerTempoCpuProcessoMs = { amostrasCpu.removeFirst() },
            lerTempoDecorridoMs = { amostrasTempo.removeFirst() },
            lerQuantidadeProcessadores = { throw RuntimeException("Falha ao consultar nucleos") },
        )

        assertNull(falhaCpu.lerAtual())
        assertNull(falhaTempo.lerAtual())
        assertNull(falhaProcessadores.lerAtual())
        assertNull(falhaProcessadores.lerAtual())
    }
}

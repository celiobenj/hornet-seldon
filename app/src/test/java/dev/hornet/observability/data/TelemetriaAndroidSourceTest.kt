package dev.hornet.observability.data

import dev.hornet.observability.core.Temperatura
import dev.hornet.observability.core.TemperatureSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TelemetriaAndroidSourceTest {

    @get:Rule
    val pastaTemporaria = TemporaryFolder()

    @Test
    fun montaAmostraCompletaIntegrandoTodosOsAdaptadores() {
        val dirCpu = pastaTemporaria.newFolder("cpu_integracao")
        val pastaPolicy = File(dirCpu, "cpufreq/policy0").apply { mkdirs() }
        File(pastaPolicy, "scaling_cur_freq").writeText("1804800\n")
        File(pastaPolicy, "cpuinfo_max_freq").writeText("2000000\n")
        File(pastaPolicy, "scaling_max_freq").writeText("1900000\n")

        val amostrasCpu = ArrayDeque(listOf(100L, 1_100L))
        val amostrasTempo = ArrayDeque(listOf(1_000L, 2_000L))
        var instanteAtual = 10_000L

        val fonte = TelemetriaAndroidSource(
            fonteTemperatura = BatteryTemperatureSource { 345 },
            fonteFrequencia = FrequenciaCpuSource(diretorioCpu = dirCpu),
            fonteUsoCpu = UsoCpuSource(
                lerTempoCpuProcessoMs = { amostrasCpu.removeFirst() },
                lerTempoDecorridoMs = { amostrasTempo.removeFirst() },
                lerQuantidadeProcessadores = { 4 },
            ),
            fonteStatusTermico = StatusTermicoSource(versaoSdk = 31, lerStatus = { 2 }),
            lerInstanteMs = { instanteAtual },
        )

        val primeira = fonte.lerAtual()
        assertNotNull(primeira)
        assertEquals(10_000L, primeira!!.instanteMs)
        assertEquals(34.5, primeira.temperaturaBateria.celsius, 0.0001)
        assertEquals(1_804_800L, primeira.frequenciaAtualKhz)
        assertEquals(2_000_000L, primeira.frequenciaMaximaKhz)
        assertEquals(1_900_000L, primeira.tetoFrequenciaKhz)
        assertNull(primeira.usoCpuPercentual)
        assertEquals(2, primeira.statusTermico)

        instanteAtual = 15_000L
        val segunda = fonte.lerAtual()
        assertNotNull(segunda)
        assertEquals(15_000L, segunda!!.instanteMs)
        assertEquals(25.0, segunda.usoCpuPercentual!!, 0.0001)
    }

    @Test
    fun retornaNuloQuandoTemperaturaIndisponivel() {
        val fonte = TelemetriaAndroidSource(
            fonteTemperatura = object : TemperatureSource {
                override fun lerAtual(): Temperatura? = null
            },
            fonteFrequencia = FrequenciaCpuSource(diretorioCpu = File(pastaTemporaria.root, "ausente")),
            fonteUsoCpu = UsoCpuSource(
                lerTempoCpuProcessoMs = { 100L },
                lerTempoDecorridoMs = { 1_000L },
                lerQuantidadeProcessadores = { 4 },
            ),
            fonteStatusTermico = StatusTermicoSource(versaoSdk = 31, lerStatus = { 0 }),
            lerInstanteMs = { 1_000L },
        )

        assertNull(fonte.lerAtual())
    }

    @Test
    fun retornaNuloSemExcecaoQuandoTemperaturaOuRelogioFalham() {
        val temperaturaComErro = TelemetriaAndroidSource(
            fonteTemperatura = object : TemperatureSource {
                override fun lerAtual(): Temperatura? {
                    throw SecurityException("Falha ao ler bateria")
                }
            },
            fonteFrequencia = FrequenciaCpuSource(diretorioCpu = File(pastaTemporaria.root, "ausente")),
            fonteUsoCpu = UsoCpuSource(
                lerTempoCpuProcessoMs = { 100L },
                lerTempoDecorridoMs = { 1_000L },
                lerQuantidadeProcessadores = { 4 },
            ),
            fonteStatusTermico = StatusTermicoSource(versaoSdk = 31, lerStatus = { 0 }),
            lerInstanteMs = { 1_000L },
        )
        val relogioComErro = TelemetriaAndroidSource(
            fonteTemperatura = BatteryTemperatureSource { 350 },
            fonteFrequencia = FrequenciaCpuSource(diretorioCpu = File(pastaTemporaria.root, "ausente")),
            fonteUsoCpu = UsoCpuSource(
                lerTempoCpuProcessoMs = { 100L },
                lerTempoDecorridoMs = { 1_000L },
                lerQuantidadeProcessadores = { 4 },
            ),
            fonteStatusTermico = StatusTermicoSource(versaoSdk = 31, lerStatus = { 0 }),
            lerInstanteMs = { throw IllegalStateException("Relogio indisponivel") },
        )

        assertNull(temperaturaComErro.lerAtual())
        assertNull(relogioComErro.lerAtual())
    }

    @Test
    fun retornaAmostraComOpcionaisNulosQuandoDemaisLeitoresFalhamComExcecao() {
        val dirCpu = pastaTemporaria.newFolder("cpu_erro")
        File(dirCpu, "cpufreq/policy0").mkdirs()

        val fonte = TelemetriaAndroidSource(
            fonteTemperatura = BatteryTemperatureSource { 360 },
            fonteFrequencia = FrequenciaCpuSource(
                diretorioCpu = dirCpu,
                lerTexto = { throw SecurityException("EACCES") },
            ),
            fonteUsoCpu = UsoCpuSource(
                lerTempoCpuProcessoMs = { throw SecurityException("Sem permissao") },
                lerTempoDecorridoMs = { throw IllegalStateException("Erro") },
                lerQuantidadeProcessadores = { throw RuntimeException("Erro") },
            ),
            fonteStatusTermico = StatusTermicoSource(
                versaoSdk = 31,
                lerStatus = { throw SecurityException("Bloqueado") },
            ),
            lerInstanteMs = { 20_000L },
        )

        val amostra = fonte.lerAtual()

        assertNotNull(amostra)
        assertEquals(20_000L, amostra!!.instanteMs)
        assertEquals(36.0, amostra.temperaturaBateria.celsius, 0.0001)
        assertNull(amostra.frequenciaAtualKhz)
        assertNull(amostra.frequenciaMaximaKhz)
        assertNull(amostra.tetoFrequenciaKhz)
        assertNull(amostra.usoCpuPercentual)
        assertNull(amostra.statusTermico)
    }

    @Test
    fun retornaAmostraComOpcionaisNulosQuandoLeitoresRetornamValoresInvalidosOuAusentes() {
        val dirCpu = pastaTemporaria.newFolder("cpu_invalido")
        val pastaPolicy = File(dirCpu, "cpufreq/policy0").apply { mkdirs() }
        File(pastaPolicy, "scaling_cur_freq").writeText("abc\n")
        File(pastaPolicy, "cpuinfo_max_freq").writeText("0\n")
        File(pastaPolicy, "scaling_max_freq").writeText("-500\n")

        val fonte = TelemetriaAndroidSource(
            fonteTemperatura = BatteryTemperatureSource { 350 },
            fonteFrequencia = FrequenciaCpuSource(diretorioCpu = dirCpu),
            fonteUsoCpu = UsoCpuSource(
                lerTempoCpuProcessoMs = { -10L },
                lerTempoDecorridoMs = { -20L },
                lerQuantidadeProcessadores = { 0 },
            ),
            fonteStatusTermico = StatusTermicoSource(versaoSdk = 31, lerStatus = { 99 }),
            lerInstanteMs = { 30_000L },
        )

        val amostra = fonte.lerAtual()

        assertNotNull(amostra)
        assertEquals(30_000L, amostra!!.instanteMs)
        assertEquals(35.0, amostra.temperaturaBateria.celsius, 0.0001)
        assertNull(amostra.frequenciaAtualKhz)
        assertNull(amostra.frequenciaMaximaKhz)
        assertNull(amostra.tetoFrequenciaKhz)
        assertNull(amostra.usoCpuPercentual)
        assertNull(amostra.statusTermico)
    }
}

package dev.hornet.observability.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class FrequenciaCpuSourceTest {

    @get:Rule
    val pastaTemporaria = TemporaryFolder()

    @Test
    fun analisaTextoValidoComQuebraDeLinhaEEspacos() {
        assertEquals(1_804_800L, analisarKhz("1804800\n"))
        assertEquals(2_304_000L, analisarKhz("  2304000 \r\n"))
    }

    @Test
    fun retornaNuloParaTextoVazioOuEmBranco() {
        assertNull(analisarKhz(""))
        assertNull(analisarKhz("   "))
        assertNull(analisarKhz("\n\t\r"))
    }

    @Test
    fun retornaNuloParaTextoComLetrasOuDecimal() {
        assertNull(analisarKhz("abc"))
        assertNull(analisarKhz("1804800kHz"))
        assertNull(analisarKhz("1804.8"))
        assertNull(analisarKhz("freq=1804800"))
    }

    @Test
    fun retornaNuloParaValorNegativo() {
        assertNull(analisarKhz("-1"))
        assertNull(analisarKhz("-1804800\n"))
    }

    @Test
    fun retornaNuloParaZero() {
        assertNull(analisarKhz("0"))
        assertNull(analisarKhz("0\n"))
        assertNull(analisarKhz(" 0 "))
    }

    @Test
    fun retornaNuloParaNumeroMuitoGrande() {
        assertNull(analisarKhz("99999999999999999999999999"))
        assertNull(analisarKhz("9223372036854775808\n"))
    }

    @Test
    fun escolheMaiorFrequenciaEntreNucleosComValoresValidosENulos() {
        val nucleos = listOf(
            LeituraFrequenciaCpu(frequenciaAtualKhz = null),
            LeituraFrequenciaCpu(frequenciaAtualKhz = 1_200_000L),
            LeituraFrequenciaCpu(frequenciaAtualKhz = 1_804_800L),
            LeituraFrequenciaCpu(frequenciaAtualKhz = null),
            LeituraFrequenciaCpu(frequenciaAtualKhz = 979_200L),
        )

        val escolhido = selecionarGrupoFrequencia(nucleos)

        assertEquals(1_804_800L, escolhido.frequenciaAtualKhz)
        assertNull(escolhido.frequenciaMaximaKhz)
        assertNull(escolhido.tetoFrequenciaKhz)
    }

    @Test
    fun escolheMaiorFrequenciaEntreGruposComMesmoMaximoENulos() {
        val grupos = listOf(
            LeituraFrequenciaCpu(),
            LeituraFrequenciaCpu(
                frequenciaAtualKhz = null,
                frequenciaMaximaKhz = 2_000_000L,
                tetoFrequenciaKhz = 2_000_000L,
            ),
            LeituraFrequenciaCpu(
                frequenciaAtualKhz = 1_400_000L,
                frequenciaMaximaKhz = 2_000_000L,
                tetoFrequenciaKhz = 2_000_000L,
            ),
            LeituraFrequenciaCpu(
                frequenciaAtualKhz = 1_800_000L,
                frequenciaMaximaKhz = 2_000_000L,
                tetoFrequenciaKhz = 1_900_000L,
            ),
        )

        val escolhido = selecionarGrupoFrequencia(grupos)

        assertEquals(1_800_000L, escolhido.frequenciaAtualKhz)
        assertEquals(2_000_000L, escolhido.frequenciaMaximaKhz)
        assertEquals(1_900_000L, escolhido.tetoFrequenciaKhz)
    }

    @Test
    fun escolheGrupoComMaiorRazaoAtualSobreMaximoMantendoTresValoresDoMesmoGrupo() {
        val policy0 = LeituraFrequenciaCpu(
            frequenciaAtualKhz = 1_804_800L,
            frequenciaMaximaKhz = 1_804_800L,
            tetoFrequenciaKhz = 1_804_800L,
        )
        val policy6 = LeituraFrequenciaCpu(
            frequenciaAtualKhz = 2_169_600L,
            frequenciaMaximaKhz = 2_304_000L,
            tetoFrequenciaKhz = 2_304_000L,
        )

        val escolhido = selecionarGrupoFrequencia(listOf(policy0, policy6))

        assertEquals(policy0, escolhido)
    }

    @Test
    fun desempataRazaoIgualPelaMaiorFrequenciaAtual() {
        val policy0 = LeituraFrequenciaCpu(
            frequenciaAtualKhz = 1_804_800L,
            frequenciaMaximaKhz = 1_804_800L,
            tetoFrequenciaKhz = 1_804_800L,
        )
        val policy6 = LeituraFrequenciaCpu(
            frequenciaAtualKhz = 2_304_000L,
            frequenciaMaximaKhz = 2_304_000L,
            tetoFrequenciaKhz = 2_304_000L,
        )

        val escolhido = selecionarGrupoFrequencia(listOf(policy0, policy6))

        assertEquals(policy6, escolhido)
    }

    @Test
    fun retornaLeituraVaziaQuandoNenhumGrupoTemValorValido() {
        val gruposInvalidos = listOf(
            LeituraFrequenciaCpu(),
            LeituraFrequenciaCpu(frequenciaAtualKhz = 0L, frequenciaMaximaKhz = -1L, tetoFrequenciaKhz = 0L),
        )

        assertEquals(LeituraFrequenciaCpu(), selecionarGrupoFrequencia(emptyList()))
        assertEquals(LeituraFrequenciaCpu(), selecionarGrupoFrequencia(gruposInvalidos))
    }

    @Test
    fun escolheGrupoComMenorRazaoDeTetoQuandoFrequenciaAtualAusente() {
        val semRestricao = LeituraFrequenciaCpu(
            frequenciaAtualKhz = null,
            frequenciaMaximaKhz = 2_000_000L,
            tetoFrequenciaKhz = 2_000_000L,
        )
        val comTetoReduzido = LeituraFrequenciaCpu(
            frequenciaAtualKhz = null,
            frequenciaMaximaKhz = 2_000_000L,
            tetoFrequenciaKhz = 1_600_000L,
        )

        val escolhido = selecionarGrupoFrequencia(listOf(semRestricao, comTetoReduzido))

        assertEquals(comTetoReduzido, escolhido)
    }

    @Test
    fun lePoliciesDeDiretorioSimuladoEPriorizaPolicySobreNucleos() {
        val dirCpu = pastaTemporaria.newFolder("cpu")
        criarGrupo(
            pasta = File(dirCpu, "cpufreq/policy0"),
            atual = "1804800\n",
            maxima = "1804800\n",
            teto = "1804800\n",
        )
        criarGrupo(
            pasta = File(dirCpu, "cpufreq/policy6"),
            atual = "2169600\n",
            maxima = "2304000\n",
            teto = "2304000\n",
        )
        // Diretorio cpu0 alternativo nao deve sobrescrever quando policyN tem dados validos.
        criarGrupo(
            pasta = File(dirCpu, "cpu0/cpufreq"),
            atual = "500000\n",
            maxima = "500000\n",
            teto = "500000\n",
        )

        val fonte = FrequenciaCpuSource(diretorioCpu = dirCpu)
        val leitura = fonte.lerAtual()

        assertEquals(1_804_800L, leitura.frequenciaAtualKhz)
        assertEquals(1_804_800L, leitura.frequenciaMaximaKhz)
        assertEquals(1_804_800L, leitura.tetoFrequenciaKhz)
    }

    @Test
    fun usaFallbackParaNucleosQuandoPoliciesAusentes() {
        val dirCpu = pastaTemporaria.newFolder("cpu_sem_policy")
        criarGrupo(
            pasta = File(dirCpu, "cpu0/cpufreq"),
            atual = "texto_invalido\n",
            maxima = null,
            teto = null,
        )
        criarGrupo(
            pasta = File(dirCpu, "cpu1/cpufreq"),
            atual = "1200000\n",
            maxima = null,
            teto = null,
        )
        criarGrupo(
            pasta = File(dirCpu, "cpu2/cpufreq"),
            atual = "1804800\n",
            maxima = null,
            teto = null,
        )

        val fonte = FrequenciaCpuSource(diretorioCpu = dirCpu)
        val leitura = fonte.lerAtual()

        assertEquals(1_804_800L, leitura.frequenciaAtualKhz)
        assertNull(leitura.frequenciaMaximaKhz)
        assertNull(leitura.tetoFrequenciaKhz)
    }

    @Test
    fun retornaCamposNulosSemExcecaoQuandoDiretorioAusente() {
        val dirInexistente = File(pastaTemporaria.root, "diretorio_inexistente")
        val fonte = FrequenciaCpuSource(diretorioCpu = dirInexistente)

        val leitura = fonte.lerAtual()

        assertEquals(LeituraFrequenciaCpu(), leitura)
    }

    @Test
    fun mantemDemaisCamposQuandoApenasUmArquivoAusenteOuInvalido() {
        val dirCpu = pastaTemporaria.newFolder("cpu_parcial")
        criarGrupo(
            pasta = File(dirCpu, "cpufreq/policy0"),
            atual = null,
            maxima = "1804800\n",
            teto = "1500000\n",
        )
        criarGrupo(
            pasta = File(dirCpu, "cpufreq/policy1"),
            atual = "nao_numero\n",
            maxima = "-100\n",
            teto = "0\n",
        )

        val fonte = FrequenciaCpuSource(diretorioCpu = dirCpu)
        val leitura = fonte.lerAtual()

        assertNull(leitura.frequenciaAtualKhz)
        assertEquals(1_804_800L, leitura.frequenciaMaximaKhz)
        assertEquals(1_500_000L, leitura.tetoFrequenciaKhz)
    }

    @Test
    fun retornaCamposNulosSemExcecaoQuandoFaltaPermissaoNaLeituraDeArquivos() {
        val dirCpu = pastaTemporaria.newFolder("cpu_sem_permissao")
        criarGrupo(
            pasta = File(dirCpu, "cpufreq/policy0"),
            atual = "1804800\n",
            maxima = "1804800\n",
            teto = "1804800\n",
        )

        val fonteComSecurityException = FrequenciaCpuSource(
            diretorioCpu = dirCpu,
            lerTexto = { throw SecurityException("EACCES simulado") },
        )
        val fonteComIoException = FrequenciaCpuSource(
            diretorioCpu = dirCpu,
            lerTexto = { throw IOException("Permission denied") },
        )

        assertEquals(LeituraFrequenciaCpu(), fonteComSecurityException.lerAtual())
        assertEquals(LeituraFrequenciaCpu(), fonteComIoException.lerAtual())
    }

    @Test
    fun retornaCamposNulosSemExcecaoQuandoFaltaPermissaoAoListarDiretorios() {
        val diretorioBloqueado = object : File(pastaTemporaria.root, "sysfs_bloqueado") {
            override fun listFiles(): Array<File> {
                throw SecurityException("Listagem bloqueada por SELinux")
            }
        }
        val fonte = FrequenciaCpuSource(diretorioCpu = diretorioBloqueado)

        assertEquals(LeituraFrequenciaCpu(), fonte.lerAtual())
    }

    private fun criarGrupo(
        pasta: File,
        atual: String?,
        maxima: String?,
        teto: String?,
    ) {
        pasta.mkdirs()
        if (atual != null) {
            File(pasta, "scaling_cur_freq").writeText(atual)
        }
        if (maxima != null) {
            File(pasta, "cpuinfo_max_freq").writeText(maxima)
        }
        if (teto != null) {
            File(pasta, "scaling_max_freq").writeText(teto)
        }
    }
}

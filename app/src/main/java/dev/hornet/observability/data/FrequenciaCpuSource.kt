package dev.hornet.observability.data

import java.io.File

/**
 * Frequencias lidas de um mesmo grupo de nucleos (policy ou cpuN) em kHz.
 * Campos nulos indicam que o arquivo nao pode ser lido ou trouxe valor invalido.
 */
data class LeituraFrequenciaCpu(
    val frequenciaAtualKhz: Long? = null,
    val frequenciaMaximaKhz: Long? = null,
    val tetoFrequenciaKhz: Long? = null,
) {
    val possuiAlgumDado: Boolean
        get() = frequenciaAtualKhz != null || frequenciaMaximaKhz != null || tetoFrequenciaKhz != null
}

/**
 * Converte o texto lido de um arquivo do cpufreq para kHz.
 * Remove espacos e quebras de linha e retorna null para texto invalido, zero ou negativo.
 */
fun analisarKhz(texto: String): Long? =
    texto.trim().toLongOrNull()?.takeIf { it > 0L }

/**
 * Escolhe o grupo de nucleos representativo seguindo o contrato v2:
 * em CPUs com maximos diferentes, vence o grupo com maior razao atual/maximo
 * (desempatando pela maior frequencia atual), mantendo os tres valores do mesmo grupo.
 */
internal fun selecionarGrupoFrequencia(grupos: List<LeituraFrequenciaCpu>): LeituraFrequenciaCpu {
    val validos = grupos
        .map { grupo ->
            LeituraFrequenciaCpu(
                frequenciaAtualKhz = grupo.frequenciaAtualKhz?.takeIf { it > 0L },
                frequenciaMaximaKhz = grupo.frequenciaMaximaKhz?.takeIf { it > 0L },
                tetoFrequenciaKhz = grupo.tetoFrequenciaKhz?.takeIf { it > 0L },
            )
        }
        .filter { it.possuiAlgumDado }

    if (validos.isEmpty()) {
        return LeituraFrequenciaCpu()
    }

    val comRazaoAtual = validos.filter {
        it.frequenciaAtualKhz != null && it.frequenciaMaximaKhz != null
    }
    if (comRazaoAtual.isNotEmpty()) {
        return comRazaoAtual.maxWith(
            compareBy<LeituraFrequenciaCpu> {
                it.frequenciaAtualKhz!!.toDouble() / it.frequenciaMaximaKhz!!.toDouble()
            }
                .thenBy { it.frequenciaAtualKhz!! }
                .thenBy { it.frequenciaMaximaKhz!! }
                .thenBy { it.tetoFrequenciaKhz ?: 0L },
        )
    }

    val comAtual = validos.filter { it.frequenciaAtualKhz != null }
    if (comAtual.isNotEmpty()) {
        return comAtual.maxWith(
            compareBy<LeituraFrequenciaCpu> { it.frequenciaAtualKhz!! }
                .thenBy { it.tetoFrequenciaKhz ?: 0L }
                .thenBy { it.frequenciaMaximaKhz ?: 0L },
        )
    }

    val comRazaoTeto = validos.filter {
        it.tetoFrequenciaKhz != null && it.frequenciaMaximaKhz != null
    }
    if (comRazaoTeto.isNotEmpty()) {
        return comRazaoTeto.minWith(
            compareBy<LeituraFrequenciaCpu> {
                it.tetoFrequenciaKhz!!.toDouble() / it.frequenciaMaximaKhz!!.toDouble()
            }
                .thenByDescending { it.frequenciaMaximaKhz!! }
                .thenByDescending { it.tetoFrequenciaKhz!! },
        )
    }

    return validos.maxWith(
        compareBy<LeituraFrequenciaCpu> { it.tetoFrequenciaKhz ?: 0L }
            .thenBy { it.frequenciaMaximaKhz ?: 0L },
    )
}

/**
 * Adaptador Android para leitura de frequencia atual, teto e maximo de fabrica da CPU via sysfs.
 *
 * Conforme validado em docs/validacao/frequencia-e-status-termico.md, prioriza os grupos
 * em cpufreq/policyN para reduzir I/O e manter coerencia entre os tres valores do mesmo grupo,
 * com fallback para cpuN/cpufreq caso policyN nao esteja disponivel.
 * Falhas de leitura ou bloqueios de SELinux resultam em campos nulos, sem lancar excecao.
 */
class FrequenciaCpuSource(
    private val diretorioCpu: File = File(DIRETORIO_CPU_PADRAO),
    private val lerTexto: (File) -> String? = { it.readText() },
) {

    fun lerAtual(): LeituraFrequenciaCpu {
        val leiturasPolicy = lerGrupos(listarDiretoriosPolicy())
        if (leiturasPolicy.any { it.possuiAlgumDado }) {
            return selecionarGrupoFrequencia(leiturasPolicy)
        }

        val leiturasNucleos = lerGrupos(listarDiretoriosNucleos())
        return selecionarGrupoFrequencia(leiturasNucleos)
    }

    private fun lerGrupos(diretorios: List<File>): List<LeituraFrequenciaCpu> =
        diretorios.map { diretorio ->
            LeituraFrequenciaCpu(
                frequenciaAtualKhz = lerKhz(File(diretorio, ARQUIVO_FREQUENCIA_ATUAL)),
                frequenciaMaximaKhz = lerKhz(File(diretorio, ARQUIVO_FREQUENCIA_MAXIMA)),
                tetoFrequenciaKhz = lerKhz(File(diretorio, ARQUIVO_TETO_FREQUENCIA)),
            )
        }

    private fun listarDiretoriosPolicy(): List<File> =
        listarPastasNumeradas(File(diretorioCpu, SUBDIRETORIO_CPUFREQ), REGEX_POLICY)

    private fun listarDiretoriosNucleos(): List<File> =
        listarPastasNumeradas(diretorioCpu, REGEX_NUCLEO)
            .map { File(it, SUBDIRETORIO_CPUFREQ) }
            .filter { ehDiretorioSeguro(it) }

    private fun listarPastasNumeradas(pastaBase: File, padrao: Regex): List<File> {
        return try {
            pastaBase.listFiles()
                ?.filter { ehDiretorioSeguro(it) && padrao.matches(it.name) }
                ?.sortedBy { extrairIndice(it.name, padrao) }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun ehDiretorioSeguro(arquivo: File): Boolean {
        return try {
            arquivo.isDirectory
        } catch (_: Exception) {
            false
        }
    }

    private fun extrairIndice(nome: String, padrao: Regex): Int =
        padrao.matchEntire(nome)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE

    private fun lerKhz(arquivo: File): Long? {
        return try {
            lerTexto(arquivo)?.let(::analisarKhz)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val DIRETORIO_CPU_PADRAO = "/sys/devices/system/cpu"
        private const val SUBDIRETORIO_CPUFREQ = "cpufreq"
        private const val ARQUIVO_FREQUENCIA_ATUAL = "scaling_cur_freq"
        private const val ARQUIVO_FREQUENCIA_MAXIMA = "cpuinfo_max_freq"
        private const val ARQUIVO_TETO_FREQUENCIA = "scaling_max_freq"
        private val REGEX_POLICY = Regex("^policy(\\d+)$")
        private val REGEX_NUCLEO = Regex("^cpu(\\d+)$")
    }
}

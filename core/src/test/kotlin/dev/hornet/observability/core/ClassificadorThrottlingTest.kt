package dev.hornet.observability.core

import dev.hornet.observability.core.EstadoThrottling.AQUECENDO
import dev.hornet.observability.core.EstadoThrottling.INDETERMINADO
import dev.hornet.observability.core.EstadoThrottling.NORMAL
import dev.hornet.observability.core.EstadoThrottling.THROTTLING_CONFIRMADO
import dev.hornet.observability.core.EstadoThrottling.THROTTLING_PROVAVEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassificadorThrottlingTest {

    private val classificador: ThrottlingClassifier = ClassificadorThrottling()

    @Test
    fun `T-01 temperatura invalida tem prioridade sobre todos os sinais`() {
        val amostra = amostra(61.0, atual = 1_000_000, maxima = 2_000_000, teto = 1_600_000, uso = 90.0, status = 6)

        assertEquals(INDETERMINADO, classificar(amostra, StatusLeitura.INVALIDA).estado)
    }

    @Test
    fun `T-02 status termico no limite confirma throttling sozinho`() {
        assertEquals(THROTTLING_CONFIRMADO, classificar(amostra(35.0, status = 2)).estado)
    }

    @Test
    fun `T-03 status termico abaixo do limite com bateria fria resulta em normal`() {
        assertEquals(NORMAL, classificar(amostra(35.0, status = 1)).estado)
    }

    @Test
    fun `T-04 teto imposto e temperatura no limite confirmam throttling`() {
        val amostra = amostra(40.0, maxima = 2_000_000, teto = 1_780_000)

        assertEquals(THROTTLING_CONFIRMADO, classificar(amostra).estado)
    }

    @Test
    fun `T-05 teto imposto logo abaixo da temperatura de confirmacao resulta em provavel`() {
        val amostra = amostra(39.9, maxima = 2_000_000, teto = 1_780_000)

        assertEquals(THROTTLING_PROVAVEL, classificar(amostra).estado)
    }

    @Test
    fun `T-06 teto imposto com temperatura no limite de aquecimento resulta em provavel`() {
        val amostra = amostra(38.0, maxima = 2_000_000, teto = 1_780_000, status = 0)

        assertEquals(THROTTLING_PROVAVEL, classificar(amostra).estado)
    }

    @Test
    fun `T-07 teto imposto logo abaixo da temperatura de aquecimento resulta em normal`() {
        val amostra = amostra(37.9, maxima = 2_000_000, teto = 1_780_000, status = 0)

        assertEquals(NORMAL, classificar(amostra).estado)
    }

    @Test
    fun `T-08 teto igual ao limite de razao resulta em aquecimento pela temperatura`() {
        val amostra = amostra(40.0, maxima = 2_000_000, teto = 1_800_000)

        assertEquals(AQUECENDO, classificar(amostra).estado)
    }

    @Test
    fun `T-09 frequencia baixa e carga no limite com aquecimento resultam em provavel`() {
        val amostra = amostra(38.0, atual = 1_380_000, maxima = 2_000_000, uso = 70.0)

        assertEquals(THROTTLING_PROVAVEL, classificar(amostra).estado)
    }

    @Test
    fun `T-10 frequencia igual ao limite de razao resulta em aquecimento pela temperatura`() {
        val amostra = amostra(38.0, atual = 1_400_000, maxima = 2_000_000, uso = 70.0, status = 0)

        assertEquals(AQUECENDO, classificar(amostra).estado)
    }

    @Test
    fun `T-11 carga logo abaixo do limite resulta em aquecimento pela temperatura`() {
        val amostra = amostra(38.0, atual = 1_380_000, maxima = 2_000_000, uso = 69.9, status = 0)

        assertEquals(AQUECENDO, classificar(amostra).estado)
    }

    @Test
    fun `T-12 carga ausente com frequencia baixa resulta em aquecimento pela temperatura`() {
        val amostra = amostra(38.0, atual = 1_380_000, maxima = 2_000_000, status = 0)

        assertEquals(AQUECENDO, classificar(amostra).estado)
    }

    @Test
    fun `T-13 pico abaixo do limite de temperatura resulta em aquecimento`() {
        assertEquals(AQUECENDO, classificar(amostra(36.0), StatusLeitura.PICO).estado)
    }

    @Test
    fun `T-14 limiar do detector excedido sem outros sinais resulta em aquecimento`() {
        assertEquals(AQUECENDO, classificar(amostra(45.0), StatusLeitura.LIMIAR_EXCEDIDO).estado)
    }

    @Test
    fun `T-15 frequencia e carga baixas com bateria fria resultam em normal`() {
        val amostra = amostra(35.0, atual = 1_000_000, maxima = 2_000_000, teto = 2_000_000, uso = 10.0, status = 0)

        assertEquals(NORMAL, classificar(amostra).estado)
    }

    @Test
    fun `T-16 temperatura abaixo do limite durante calibracao resulta em normal`() {
        assertEquals(NORMAL, classificar(amostra(37.9), StatusLeitura.SEM_BASE).estado)
    }

    @Test
    fun `T-17 temperatura no limite durante calibracao resulta em aquecimento`() {
        assertEquals(AQUECENDO, classificar(amostra(38.0), StatusLeitura.SEM_BASE).estado)
    }

    @Test
    fun `T-18 maxima zero e status termico fora da faixa contam como ausentes`() {
        val amostra = amostra(40.0, atual = 1_000_000, maxima = 0, teto = 1_600_000, uso = 80.0, status = 7)

        assertEquals(AQUECENDO, classificar(amostra).estado)
    }

    @Test
    fun `T-19 carga acima de 100 por cento conta como ausente`() {
        val amostra = amostra(38.0, atual = 1_380_000, maxima = 2_000_000, uso = 101.0, status = 0)

        assertEquals(AQUECENDO, classificar(amostra).estado)
    }

    @Test
    fun `T-20 status termico tem prioridade sobre as regras de teto e carga`() {
        val amostra = amostra(41.0, atual = 1_000_000, maxima = 2_000_000, teto = 1_600_000, uso = 90.0, status = 2)
        val resultado = classificar(amostra)

        assertEquals(THROTTLING_CONFIRMADO, resultado.estado)
        conferirMotivo(resultado, "Android", """\b(?:2|MODERATE)\b""")
    }

    @Test
    fun `T-21 teto com bateria quente tem prioridade sobre teto com aquecimento e carga`() {
        val amostra = amostra(40.0, atual = 1_000_000, maxima = 2_000_000, teto = 1_600_000, uso = 90.0, status = 1)
        val resultado = classificar(amostra)

        assertEquals(THROTTLING_CONFIRMADO, resultado.estado)
        conferirMotivo(resultado, "teto", """\b80(?:[,.]0+)?\s*%""", """\b40(?:[,.]0+)?\s*°C""")
    }

    @Test
    fun `T-22 frequencia baixa e carga alta abaixo da temperatura de aquecimento resultam em normal`() {
        val amostra = amostra(37.9, atual = 1_380_000, maxima = 2_000_000, uso = 70.0)

        assertEquals(NORMAL, classificar(amostra).estado)
    }

    @Test
    fun `T-23 teto com aquecimento tem prioridade sobre frequencia baixa e carga alta`() {
        val amostra = amostra(38.0, atual = 1_380_000, maxima = 2_000_000, teto = 1_780_000, uso = 70.0)
        val resultado = classificar(amostra)

        assertEquals(THROTTLING_PROVAVEL, resultado.estado)
        conferirMotivo(resultado, "teto", """\b89(?:[,.]0+)?\s*%""", """\b38(?:[,.]0+)?\s*°C""")
    }

    @Test
    fun `classifica amostra valida com todos os sinais opcionais ausentes`() {
        val resultado = classificar(amostra(35.0))

        assertEquals(NORMAL, resultado.estado)
        assertTrue(resultado.motivo.isNotBlank())
    }

    @Test
    fun `uso infinito com frequencia baixa conta como ausente`() {
        val amostra = amostra(38.0, atual = 1_380_000, maxima = 2_000_000, uso = Double.POSITIVE_INFINITY)

        assertEquals(AQUECENDO, classificar(amostra).estado)
    }

    @Test
    fun `limiar de aquecimento personalizado muda o resultado de T-09`() {
        val configuracao = ConfiguracaoThrottling(temperaturaMinimaAquecimentoCelsius = 39.0)
        val classificadorPersonalizado = ClassificadorThrottling(configuracao)
        val amostra = amostra(38.0, atual = 1_380_000, maxima = 2_000_000, uso = 70.0)
        val avaliacao = Avaliacao(amostra.temperaturaBateria, StatusLeitura.NORMAL)

        assertEquals(NORMAL, classificadorPersonalizado.classificar(amostra, avaliacao).estado)
    }

    @Test
    fun `rejeita configuracao com limiares de temperatura invertidos`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfiguracaoThrottling(temperaturaMinimaAquecimentoCelsius = 41.0)
        }
    }

    @Test
    fun `rejeita configuracao com temperatura infinita`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfiguracaoThrottling(temperaturaMinimaConfirmacaoCelsius = Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `rejeita configuracao com razao de teto zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfiguracaoThrottling(razaoTetoImposto = 0.0)
        }
    }

    @Test
    fun `rejeita configuracao com razao de frequencia acima de um`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfiguracaoThrottling(razaoFrequenciaBaixa = 1.01)
        }
    }

    @Test
    fun `rejeita configuracao com limite de uso acima de 100 por cento`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfiguracaoThrottling(usoCpuAltoPercentual = 101.0)
        }
    }

    @Test
    fun `rejeita configuracao com nivel termico minimo fora da faixa`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfiguracaoThrottling(statusTermicoMinimo = 7)
        }
    }

    private fun amostra(
        celsius: Double,
        atual: Long? = null,
        maxima: Long? = null,
        teto: Long? = null,
        uso: Double? = null,
        status: Int? = null,
    ) = AmostraTelemetria(
        instanteMs = 0L,
        temperaturaBateria = Temperatura.deCelsius(celsius),
        frequenciaAtualKhz = atual,
        frequenciaMaximaKhz = maxima,
        tetoFrequenciaKhz = teto,
        usoCpuPercentual = uso,
        statusTermico = status,
    )

    private fun classificar(amostra: AmostraTelemetria, status: StatusLeitura = StatusLeitura.NORMAL): ResultadoThrottling =
        classificador.classificar(amostra, Avaliacao(amostra.temperaturaBateria, status))

    private fun conferirMotivo(resultado: ResultadoThrottling, vararg evidencias: String) {
        for (evidencia in evidencias) {
            assertTrue(
                "Motivo '${resultado.motivo}' deve conter a evidência '$evidencia'",
                Regex(evidencia, RegexOption.IGNORE_CASE).containsMatchIn(resultado.motivo),
            )
        }
    }
}

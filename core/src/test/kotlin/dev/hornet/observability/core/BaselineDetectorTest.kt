package dev.hornet.observability.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BaselineDetectorTest {

    private val detector = BaselineDetector()
    private val tolerancia = 1e-9

    @Test
    fun `V-01 calibra com dez leituras e classifica a decima primeira como normal`() {
        repeat(10) { conferirSemEstatisticas(30.0, StatusLeitura.SEM_BASE) }

        conferirEstatisticas(avaliar(30.0), StatusLeitura.NORMAL, 30.0, 0.1, 0.0)
    }

    @Test
    fun `V-02 detecta um pico de seis graus e aceita a leitura seguinte na base`() {
        preencherBaseConstante()

        conferirEstatisticas(avaliar(36.0), StatusLeitura.PICO, 30.0, 0.1, 60.0)
        conferirEstatisticas(avaliar(30.0), StatusLeitura.NORMAL, 30.0, 0.1, 0.0)
    }

    @Test
    fun `V-03 usa o piso do desvio e aceita dois decimos acima da base`() {
        preencherBaseConstante()

        conferirEstatisticas(avaliar(30.2), StatusLeitura.NORMAL, 30.0, 0.1, 2.0)
    }

    @Test
    fun `V-04 usa o piso do desvio e detecta pico de meio grau`() {
        preencherBaseConstante()

        conferirEstatisticas(avaliar(30.5), StatusLeitura.PICO, 30.0, 0.1, 5.0)
    }

    @Test
    fun `V-05 aceita leitura com z igual a k`() {
        preencherBaseAlternada()

        conferirEstatisticas(avaliar(32.0), StatusLeitura.NORMAL, 30.5, 0.5, 3.0)
    }

    @Test
    fun `V-06 detecta pico com z acima de k`() {
        preencherBaseAlternada()

        conferirEstatisticas(avaliar(32.1), StatusLeitura.PICO, 30.5, 0.5, 3.2)
    }

    @Test
    fun `V-07 aplica o limiar inclusivo desde a primeira leitura`() {
        conferirSemEstatisticas(45.0, StatusLeitura.LIMIAR_EXCEDIDO)
    }

    @Test
    fun `V-08 calibra com primeira leitura abaixo do limiar`() {
        conferirSemEstatisticas(44.9, StatusLeitura.SEM_BASE)
    }

    @Test
    fun `V-09 aceita o maximo da faixa e invalida valores acima dela`() {
        conferirSemEstatisticas(60.0, StatusLeitura.LIMIAR_EXCEDIDO)
        conferirSemEstatisticas(60.1, StatusLeitura.INVALIDA)
    }

    @Test
    fun `V-10 aceita o minimo da faixa e invalida valores abaixo dela`() {
        conferirSemEstatisticas(0.0, StatusLeitura.SEM_BASE)
        conferirSemEstatisticas(-0.1, StatusLeitura.INVALIDA)
    }

    @Test
    fun `V-11 invalida NaN e os dois infinitos`() {
        conferirSemEstatisticas(Double.NaN, StatusLeitura.INVALIDA)
        conferirSemEstatisticas(Double.POSITIVE_INFINITY, StatusLeitura.INVALIDA)
        conferirSemEstatisticas(Double.NEGATIVE_INFINITY, StatusLeitura.INVALIDA)
    }

    @Test
    fun `V-12 aceita uma queda brusca de temperatura`() {
        preencherBaseConstante()

        conferirEstatisticas(avaliar(24.0), StatusLeitura.NORMAL, 30.0, 0.1, -60.0)
    }

    @Test
    fun `usa a configuracao recebida e inclui leituras normais na janela`() {
        val detectorConfigurado = BaselineDetector(
            ConfiguracaoDetector(
                janela = 2,
                k = 2.0,
                pisoDesvioCelsius = 1.0,
                limiarCelsius = 10.0,
                faixaMinimaCelsius = -5.0,
                faixaMaximaCelsius = 12.0,
            ),
        )

        assertEquals(StatusLeitura.SEM_BASE, avaliar(-5.0, detectorConfigurado).status)
        assertEquals(StatusLeitura.INVALIDA, avaliar(-5.1, detectorConfigurado).status)
        assertEquals(StatusLeitura.SEM_BASE, avaliar(-4.0, detectorConfigurado).status)
        conferirEstatisticas(avaliar(-2.5, detectorConfigurado), StatusLeitura.NORMAL, -4.5, 1.0, 2.0)
        conferirEstatisticas(avaliar(-3.25, detectorConfigurado), StatusLeitura.NORMAL, -3.25, 1.0, 0.0)
        assertEquals(StatusLeitura.PICO, avaliar(0.0, detectorConfigurado).status)
        assertEquals(StatusLeitura.LIMIAR_EXCEDIDO, avaliar(10.0, detectorConfigurado).status)
        assertEquals(StatusLeitura.LIMIAR_EXCEDIDO, avaliar(12.0, detectorConfigurado).status)
        assertEquals(StatusLeitura.INVALIDA, avaliar(12.1, detectorConfigurado).status)
    }

    private fun avaliar(celsius: Double, detector: AnomalyDetector = this.detector): Avaliacao =
        detector.avaliar(Temperatura.deCelsius(celsius))

    private fun preencherBaseConstante() {
        repeat(11) { avaliar(30.0) }
    }

    private fun preencherBaseAlternada() {
        repeat(5) {
            avaliar(30.0)
            avaliar(31.0)
        }
    }

    private fun conferirSemEstatisticas(celsius: Double, status: StatusLeitura) {
        assertEquals(Avaliacao(Temperatura.deCelsius(celsius), status), avaliar(celsius))
    }

    private fun conferirEstatisticas(
        avaliacao: Avaliacao,
        status: StatusLeitura,
        media: Double,
        desvio: Double,
        zScore: Double,
    ) {
        assertEquals(status, avaliacao.status)
        assertEquals(media, requireNotNull(avaliacao.media), tolerancia)
        assertEquals(desvio, requireNotNull(avaliacao.desvio), tolerancia)
        assertEquals(zScore, requireNotNull(avaliacao.zScore), tolerancia)
    }
}

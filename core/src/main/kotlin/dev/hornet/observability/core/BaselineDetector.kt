package dev.hornet.observability.core

class BaselineDetector(
    private val configuracao: ConfiguracaoDetector = ConfiguracaoDetector(),
) : AnomalyDetector {

    private val janela = JanelaMovel(configuracao.janela)

    override fun avaliar(temperatura: Temperatura): Avaliacao {
        val celsius = temperatura.celsius
        if (!celsius.isFinite() || celsius !in configuracao.faixaMinimaCelsius..configuracao.faixaMaximaCelsius) {
            return Avaliacao(temperatura, StatusLeitura.INVALIDA)
        }
        if (celsius >= configuracao.limiarCelsius) {
            return Avaliacao(temperatura, StatusLeitura.LIMIAR_EXCEDIDO)
        }
        if (!janela.cheia) {
            janela.adicionar(celsius)
            return Avaliacao(temperatura, StatusLeitura.SEM_BASE)
        }
        return avaliarComBase(temperatura)
    }

    private fun avaliarComBase(temperatura: Temperatura): Avaliacao {
        val media = janela.media()
        val desvio = maxOf(janela.desvioPopulacional(), configuracao.pisoDesvioCelsius)
        val zScore = (temperatura.celsius - media) / desvio
        val status = if (zScore > configuracao.k) StatusLeitura.PICO else StatusLeitura.NORMAL

        if (status == StatusLeitura.NORMAL) janela.adicionar(temperatura.celsius)
        return Avaliacao(temperatura, status, media, desvio, zScore)
    }

    override fun reiniciar() {
        janela.limpar()
    }
}

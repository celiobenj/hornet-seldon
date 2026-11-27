package dev.hornet.observability.ui

/**
 * Estado do throttling como a tela o mostra.
 *
 * Enum próprio da interface, para a tela não depender do contrato do core. A E2 converte o
 * EstadoThrottling do core para este enum (INDETERMINADO vira DADOS_INSUFICIENTES).
 */
enum class EstadoThrottlingTela {
    NORMAL,
    AQUECENDO,
    THROTTLING_PROVAVEL,
    THROTTLING_CONFIRMADO,
    DADOS_INSUFICIENTES,
}

/**
 * Tudo o que a tela multi-sinal precisa mostrar, já pronto para exibir.
 *
 * Sinal que o aparelho não fornece fica null e aparece como "indisponível", nunca como zero.
 *
 * razaoFrequencia é a frequência atual dividida pela máxima, de 0 a 1. A tela limita a barra a
 * essa faixa, mas mostra o valor recebido no texto.
 *
 * statusTermico é o nome do nível térmico do Android, por exemplo "Moderado (2)".
 *
 * simulacao indica dados roteirizados do modo demonstração; a tela mostra o rótulo "Simulação".
 */
data class EstadoTelaMultiSinal(
    val temperaturaTexto: String? = null,
    val frequenciaAtualMhz: Long? = null,
    val frequenciaMaximaMhz: Long? = null,
    val razaoFrequencia: Double? = null,
    val usoCpuPercentual: Double? = null,
    val statusTermico: String? = null,
    val estado: EstadoThrottlingTela = EstadoThrottlingTela.DADOS_INSUFICIENTES,
    val motivo: String = "",
    val simulacao: Boolean = false,
)

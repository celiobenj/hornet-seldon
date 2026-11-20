package dev.hornet.observability.core

/** Porta de entrada: avalia leituras de temperatura, uma por vez. Guarda estado (a janela). */
interface AnomalyDetector {
    /** Classifica a leitura e atualiza o estado interno conforme as regras do detector. */
    fun avaliar(temperatura: Temperatura): Avaliacao

    /** Esquece todas as leituras anteriores. A próxima leitura volta a ser SEM_BASE. */
    fun reiniciar()
}

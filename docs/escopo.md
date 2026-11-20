# Escopo do projeto Hornet

Documento de referência. Escopo fechado pela equipe em 19/11/2026, com entrega e apresentação em 23/11/2026. Qualquer mudança deve ser combinada no grupo e registrada aqui.

## 1. Problema
Aparelhos Android reduzem o desempenho (thermal throttling) quando esquentam. Em partidas longas de jogos o aquecimento é gradual e a pessoa só percebe a queda de fluidez quando o sistema já está limitando o aparelho. Falta, dentro do próprio app, uma forma simples de saber se a temperatura da bateria saiu do comportamento recente ou passou de um limite seguro.

## 2. Público
Pessoas que exigem muito do celular, em especial gamers mobile. Secundário: qualquer pessoa afetada por superaquecimento.

## 3. Objetivo desta fase
Construir a **linha de base** de detecção de aquecimento anormal, sem modelo de IA. A linha de base é a referência para comparar um modelo no futuro, medindo falsos positivos.

## 4. Requisitos
**Funcionais**
- RF-01: ler a temperatura da bateria do aparelho a cada 5 segundos (adaptador Android).
- RF-02: representar a temperatura em °C e aceitar °F e décimos de °C por fábricas no core.
- RF-03: classificar cada leitura como INVALIDA, LIMIAR_EXCEDIDO, SEM_BASE, PICO ou NORMAL (regras em docs/contrato-core.md).
- RF-04: mostrar no app a temperatura atual, o status e as últimas leituras.
- RF-05: mostrar um gráfico da série histórica (últimas 60 leituras), com a linha do limiar e as leituras anômalas destacadas.
- RF-06: oferecer um botão "Simular pico" (leitura atual + 6 °C) para demonstração.

**Não funcionais**
- RNF-01: o `core` não depende de Android e seus testes rodam na JVM.
- RNF-02: o detector é determinístico: a mesma série de entrada gera os mesmos status.
- RNF-03: o CI executa build e testes a cada PR.
- RNF-04: o processamento acontece no aparelho, sem envio de dados a terceiros e sem permissões especiais.

## 5. Limites (fora do escopo)
- Modelo de IA ou aprendizado de máquina.
- Temperatura de CPU, GPU ou ambiente.
- Medir o throttling em si. A bateria é um indicador indireto do aquecimento do aparelho.
- Persistência de histórico, notificações, serviço em segundo plano e testes instrumentados.

## 6. Métrica de sucesso
Série sintética determinística: semente 42, 200 leituras em torno de 30 °C com ruído gaussiano de desvio 0,3 °C (arredondado a 0,1 °C, como o sensor), e 5 picos de +6 °C injetados nos índices 40, 80, 120, 150 e 180. Meta:
- detectar (status PICO) pelo menos 90% dos picos injetados;
- no máximo 5% de falsos positivos (PICO ou LIMIAR_EXCEDIDO) entre as leituras não injetadas após a calibração.

Um teste automatizado calcula as duas taxas e falha se a meta não for atingida. Os dados sintéticos provam o funcionamento do detector, não a eficácia em campo.

## 7. Decisões fechadas
| ID | Decisão |
|---|---|
| D-01 | Métrica única: temperatura da bateria, indicador indireto de aquecimento. |
| D-02 | Regra de pico: z-score sobre janela móvel; só subidas contam. |
| D-03 | Parâmetros: janela N = 10, k = 3, piso do desvio 0,1 °C, desvio populacional. |
| D-04 | Leituras que não são NORMAL ou SEM_BASE não entram na janela. |
| D-05 | Limiar absoluto de 45 °C, inclusivo, válido desde a primeira leitura. Escolha de projeto fundamentada nas fontes da seção 9. |
| D-06 | Faixa plausível de 0 a 60 °C. Fora dela, ou NaN e infinito, é leitura inválida e não anomalia. |
| D-07 | O core guarda °C e oferece fábricas para °C, °F e décimos de °C. O Android entrega décimos de °C. |
| D-08 | Intervalo de leitura do app: 5 segundos. |
| D-09 | App com leitura real, status, lista de leituras, gráfico em Canvas (60 leituras) e botão "Simular pico" (+6 °C). |
| D-10 | Dois módulos (`app` e `core`), hexagonal leve, duas portas: `AnomalyDetector` e `TemperatureSource`. |
| D-11 | Avaliação por série sintética (seção 6). |
| D-12 | Git: `develop` como branch padrão, `feature/*`, PR com uma aprovação, merge commit. |
| D-13 | Apresentação mostrando o README. Representante: Célio Benjamim. |

## 8. Limitações conhecidas (devem constar na apresentação)
- Aquecimento lento passa pela regra de pico, porque a janela acompanha a subida. Só o limiar cobre esse caso.
- Uma mudança permanente de patamar abaixo do limiar continua sinalizada como pico, porque leituras anômalas não entram na janela.
- A janela de 10 leituras é pequena: média e desvio são estimativas ruidosas, e a taxa real de falsos positivos fica acima do valor teórico da regra dos três desvios. Por isso a medimos.
- Não há filtragem de ruído além do piso do desvio. O sensor tem resolução de 0,1 °C.
- A bateria pode esquentar sem throttling e o contrário também.
- A temperatura ambiente não é considerada.
- Dados sintéticos não substituem dados de campo.

## 9. Referências (limiar e unidades)
- Battery University, BU-410, "Charging at High and Low Temperatures": para Li-ion, carga entre 0 °C e 45 °C e descarga entre -20 °C e 60 °C. https://batteryuniversity.com/article/bu-410-charging-at-high-and-low-temperatures
- Battery University, BU-502, "Discharging at High and Low Temperatures": carregar e descarregar a 45 °C reduz a vida útil em ciclos à metade, em comparação a 20 °C. https://batteryuniversity.com/article/bu-502-discharging-at-high-and-low-temperatures
- Wevolver, "How to charge a lithium-ion battery safely and efficiently": a maioria das químicas Li-ion deve ser carregada entre 0 °C e 45 °C, e acima de 45 °C a perda de capacidade se acelera. https://wevolver.com/article/how-to-charge-a-lithium-ion-battery-safely-and-efficiently
- Android, BatteryManager.EXTRA_TEMPERATURE: a documentação oficial diz apenas "inteiro com a temperatura atual". A unidade de décimos de °C é amplamente relatada pela comunidade e deve ser confirmada no aparelho (valor bruto da ordem de 300 a 400). https://developer.android.com/reference/android/os/BatteryManager

Observação: as fontes tratam da célula da bateria (carga e descarga), e não do throttling do sistema. Os valores 45 °C (limiar) e 0 a 60 °C (faixa) são escolhas de projeto fundamentadas nelas, não limites oficiais do Android.

## 10. Trabalho futuro
Leitura direta do estado térmico do sistema (PowerManager.getCurrentThermalStatus, Android 10 ou superior), temperatura ambiente, taxa de variação (°C por minuto), modelos de aprendizado de máquina comparados à linha de base, persistência de histórico e notificações.

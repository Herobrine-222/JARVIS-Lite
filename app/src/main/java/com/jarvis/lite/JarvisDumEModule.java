
package com.jarvis.lite;

import java.text.Normalizer;
import java.util.Locale;

/**
 * DUM-E — especialista em consultas informativas
 * sobre o aparelho.
 *
 * Responsabilidades:
 * - Consultar bateria e estado de carregamento.
 * - Consultar armazenamento interno.
 * - Consultar memória RAM quando encaminhado ao módulo.
 * - Consultar informações básicas do dispositivo.
 * - Oferecer um relatório informativo para pedidos
 *   de verificação do aparelho.
 *
 * Segurança e limites:
 * - Não altera configurações do dispositivo.
 * - Não executa comandos destrutivos.
 * - Não afirma ter detectado ou removido malware.
 * - Não realiza uma varredura antivírus completa.
 * - Reutiliza JarvisDeviceTools através do
 *   JarvisContextManager existente.
 *
 * Este módulo funciona localmente e não precisa
 * acessar a Internet para as consultas implementadas.
 */
public final class JarvisDumEModule
        implements JarvisVisionModule {

    private static final String NAME = "DUM-E";

    private static final String MESSAGE_CONTEXT_UNAVAILABLE =
            "O DUM-E não conseguiu acessar o contexto "
                    + "do aplicativo. Tente novamente.";

    private static final String MESSAGE_TOOLS_UNAVAILABLE =
            "As ferramentas locais do aparelho estão "
                    + "indisponíveis no momento.";

    private static final String MESSAGE_QUERY_FAILED =
            "Não consegui concluir essa consulta local "
                    + "do aparelho. Tente novamente.";

    private static final String MESSAGE_EMPTY_RESULT =
            "O sistema não retornou informações "
                    + "suficientes para essa consulta.";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Este módulo só recebe análises encaminhadas
     * explicitamente para o especialista DUM_E.
     */
    @Override
    public boolean supports(
            JarvisReasoningEngine.Analysis analysis
    ) {
        return analysis != null
                && analysis.getSpecialist()
                == JarvisReasoningEngine.Specialist.DUM_E;
    }

    /**
     * Processa uma solicitação informativa do aparelho.
     *
     * A intenção do classificador é priorizada.
     * O reconhecimento pelo texto funciona como alternativa
     * para manter o módulo tolerante a variações de linguagem.
     *
     * Retornar null significa que este módulo não reconheceu
     * a solicitação e permite que o núcleo continue o fluxo.
     */
    @Override
    public String process(
            String input,
            JarvisReasoningEngine.Analysis analysis,
            JarvisVisionCore core
    ) {
        if (input == null
                || input.trim().isEmpty()
                || core == null
                || !supports(analysis)) {
            return null;
        }

        try {
            return processSafely(input, analysis, core);
        } catch (RuntimeException exception) {
            /*
             * Impede que uma falha inesperada nesta consulta
             * interrompa a aplicação.
             *
             * Não expõe detalhes internos da exceção ao usuário.
             */
            return MESSAGE_QUERY_FAILED;
        }
    }

    /**
     * Executa o fluxo principal depois das validações iniciais.
     */
    private String processSafely(
            String input,
            JarvisReasoningEngine.Analysis analysis,
            JarvisVisionCore core
    ) {
        JarvisContextManager contextManager =
                core.getContextManager();

        if (contextManager == null) {
            return MESSAGE_CONTEXT_UNAVAILABLE;
        }

        /*
         * Reutiliza a instância mantida pelo contexto.
         * Não cria um segundo JarvisDeviceTools.
         */
        JarvisDeviceTools tools =
                contextManager.getDeviceTools();

        if (tools == null) {
            return MESSAGE_TOOLS_UNAVAILABLE;
        }

        JarvisReasoningEngine.Intent intent =
                analysis.getIntent();

        /*
         * 1. Prioridade para intenções explícitas.
         *
         * Assim, o módulo não precisa deduzir novamente
         * uma intenção que o classificador já identificou.
         */
        if (intent == JarvisReasoningEngine.Intent.BATTERY) {
            return safeResult(
                    tools.resumoBateria(),
                    "Não foi possível consultar a bateria."
            );
        }

        if (intent == JarvisReasoningEngine.Intent.STORAGE) {
            return safeResult(
                    tools.resumoArmazenamento(),
                    "Não foi possível consultar o armazenamento."
            );
        }

        if (intent == JarvisReasoningEngine.Intent.DEVICE_CHECK) {
            return buildInformativeDeviceCheck(tools);
        }

        /*
         * 2. Alternativa baseada no texto normalizado.
         *
         * Abrange sinônimos, pontuação, letras acentuadas
         * e formas comuns de fazer uma solicitação.
         */
        String normalized = normalize(input);

        if (normalized.isEmpty()) {
            return null;
        }

        /*
         * Verificação de segurança vem antes das consultas
         * individuais para preservar o sentido do pedido.
         */
        if (isDeviceCheckRequest(normalized)) {
            return buildInformativeDeviceCheck(tools);
        }

        /*
         * Informações gerais do aparelho.
         */
        if (isDeviceInfoRequest(normalized)) {
            return safeResult(
                    tools.obterInformacoesAparelho(),
                    MESSAGE_EMPTY_RESULT
            );
        }

        /*
         * RAM.
         *
         * O método de consulta já existe em JarvisDeviceTools.
         * Para funcionar no fluxo normal, o classificador
         * também precisa encaminhar esse tipo de pergunta
         * para DUM_E.
         */
        if (isRamRequest(normalized)) {
            return safeResult(
                    tools.resumoRam(),
                    "Não foi possível consultar a memória RAM."
            );
        }

        /*
         * Bateria.
         */
        if (isBatteryRequest(normalized)) {
            return safeResult(
                    tools.resumoBateria(),
                    "Não foi possível consultar a bateria."
            );
        }

        /*
         * Armazenamento.
         */
        if (isStorageRequest(normalized)) {
            return safeResult(
                    tools.resumoArmazenamento(),
                    "Não foi possível consultar o armazenamento."
            );
        }

        /*
         * A mensagem não pertence às funções disponíveis.
         * null permite que o JarvisVisionCore tente outro módulo.
         */
        return null;
    }

    /**
     * Monta uma resposta honesta para pedidos de verificação.
     *
     * O relatório consulta informações que JarvisDeviceTools
     * realmente disponibiliza: dispositivo, sistema, bateria,
     * RAM e armazenamento.
     *
     * Não afirma que aplicativos foram examinados ou que malware
     * foi detectado, removido ou descartado.
     */
    private String buildInformativeDeviceCheck(
            JarvisDeviceTools tools
    ) {
        String report = safeResult(
                tools.obterInformacoesAparelho(),
                "As informações detalhadas do aparelho "
                        + "estão indisponíveis."
        );

        return "Relatório informativo do aparelho\n\n"
                + report
                + "\n\nLimites desta verificação: consultei "
                + "somente as informações disponibilizadas "
                + "pelas ferramentas locais do aplicativo. "
                + "Isso não é uma varredura antivírus, não "
                + "confirma se existe malware e não garante "
                + "que o aparelho esteja livre de ameaças.";
    }

    /**
     * Reconhece formas comuns de solicitar uma verificação
     * informativa do aparelho ou perguntar sobre malware.
     */
    private boolean isDeviceCheckRequest(String text) {
        return containsAny(
                text,
                "verificacao do aparelho",
                "verificar o aparelho",
                "verificar meu aparelho",
                "verificar aparelho",
                "verificacao de seguranca",
                "verificar seguranca",
                "analisar seguranca do celular",
                "analisar a seguranca do celular",
                "verificacao basica",
                "fazer uma verificacao",
                "fazer verificacao",
                "ver se tem virus",
                "verificar se tem virus",
                "ver se meu celular tem virus",
                "celular com virus",
                "virus no celular",
                "procurar virus",
                "checar malware",
                "verificacao de malware",
                "verificar malware",
                "analisar malware",
                "meu aparelho esta seguro",
                "meu celular esta seguro"
        );
    }

    /**
     * Reconhece pedidos de informações gerais do dispositivo.
     */
    private boolean isDeviceInfoRequest(String text) {
        return containsAny(
                text,
                "informacoes do aparelho",
                "informacao do aparelho",
                "dados do aparelho",
                "informacoes do celular",
                "informacao do celular",
                "dados do celular",
                "resumo do aparelho",
                "detalhes do aparelho",
                "qual e meu aparelho",
                "modelo do meu celular",
                "modelo do celular",
                "qual android",
                "versao do android",
                "qual versao do android",
                "sistema do celular",
                "informacoes do dispositivo"
        );
    }

    /**
     * Reconhece consultas relacionadas à RAM.
     *
     * A checagem de palavra inteira evita que "ram"
     * corresponda acidentalmente a partes de outras palavras.
     */
    private boolean isRamRequest(String text) {
        return containsAny(
                text,
                "memoria ram",
                "ram disponivel",
                "ram utilizada",
                "ram usada",
                "ram total",
                "uso da ram",
                "uso de ram",
                "quanto de ram",
                "memoria de acesso aleatorio"
        ) || containsPhrase(text, "ram");
    }

    /**
     * Reconhece consultas de bateria, carga e temperatura.
     */
    private boolean isBatteryRequest(String text) {
        return containsAny(
                text,
                "bateria",
                "por cento de bateria",
                "porcentagem da bateria",
                "percentual da bateria",
                "nivel da bateria",
                "nivel de bateria",
                "status da bateria",
                "estado da bateria",
                "temperatura da bateria",
                "temperatura bateria",
                "temperatura do celular",
                "celular esta carregando",
                "esta carregando",
                "esta na tomada",
                "estado de carregamento",
                "carregamento do celular",
                "quanto de bateria",
                "quanto resta de bateria"
        );
    }

    /**
     * Reconhece consultas de armazenamento interno.
     */
    private boolean isStorageRequest(String text) {
        return containsAny(
                text,
                "armazenamento",
                "espaco livre",
                "espaco disponivel",
                "espaco no celular",
                "espaco no aparelho",
                "quanto de espaco",
                "quanto espaco",
                "quanto armazenamento",
                "memoria interna",
                "armazenamento interno",
                "armazenamento do celular",
                "armazenamento do aparelho",
                "espaco ocupado",
                "espaco utilizado",
                "quanto resta de espaco"
        );
    }

    /**
     * Normaliza acentos, letras maiúsculas, pontuação
     * e espaços repetidos.
     *
     * Exemplos:
     * "Verificação do aparelho!" -> "verificacao do aparelho"
     * "  Quanto   de BATERIA? " -> "quanto de bateria"
     */
    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        String normalized = Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        );

        normalized = normalized.replaceAll(
                "\\p{M}+",
                ""
        );

        normalized = normalized.toLowerCase(Locale.ROOT);

        /*
         * Converte pontuação e símbolos em espaços para evitar
         * que diferenças de escrita atrapalhem a identificação.
         */
        normalized = normalized.replaceAll(
                "[^a-z0-9]+",
                " "
        );

        return normalized.trim().replaceAll("\\s+", " ");
    }

    /**
     * Procura expressões conhecidas dentro do texto normalizado.
     */
    private boolean containsAny(
            String text,
            String... phrases
    ) {
        if (text == null || text.isEmpty() || phrases == null) {
            return false;
        }

        for (String phrase : phrases) {
            if (phrase == null || phrase.trim().isEmpty()) {
                continue;
            }

            String normalizedPhrase = normalize(phrase);

            if (!normalizedPhrase.isEmpty()
                    && text.contains(normalizedPhrase)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica a presença de uma expressão como palavras
     * completas, não como parte de outra palavra.
     */
    private boolean containsPhrase(
            String text,
            String phrase
    ) {
        if (text == null
                || text.isEmpty()
                || phrase == null
                || phrase.trim().isEmpty()) {
            return false;
        }

        String normalizedText = " " + text.trim() + " ";
        String normalizedPhrase = " "
                + normalize(phrase)
                + " ";

        return normalizedText.contains(normalizedPhrase);
    }

    /**
     * Evita retornar null ou texto vazio depois de uma consulta
     * que deveria produzir uma resposta.
     */
    private String safeResult(
            String result,
            String fallback
    ) {
        if (result == null || result.trim().isEmpty()) {
            if (fallback == null || fallback.trim().isEmpty()) {
                return MESSAGE_EMPTY_RESULT;
            }

            return fallback;
        }

        return result.trim();
    }
}

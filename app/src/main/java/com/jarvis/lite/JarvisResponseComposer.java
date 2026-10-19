
package com.jarvis.lite;

/**
 * Monta respostas a partir de resultados produzidos por outros módulos.
 *
 * Não é um mecanismo de raciocínio nem um verificador factual.
 * A resposta factual deve ser produzida e validada antes de chegar aqui.
 */
public final class JarvisResponseComposer {

    public enum ResponseStyle {
        NORMAL,
        CONCISE,
        DETAILED,
        EMPATHETIC,
        TECHNICAL
    }

    public static final class ResponseInput {
        private final String content;
        private final String context;
        private final ResponseStyle style;
        private final boolean verified;
        private final boolean allowContext;

        public ResponseInput(
                String content,
                String context,
                ResponseStyle style,
                boolean verified,
                boolean allowContext) {
            this.content = safe(content);
            this.context = safe(context);
            this.style = style == null
                    ? ResponseStyle.NORMAL : style;
            this.verified = verified;
            this.allowContext = allowContext;
        }

        public String getContent() {
            return content;
        }

        public String getContext() {
            return context;
        }

        public ResponseStyle getStyle() {
            return style;
        }

        public boolean isVerified() {
            return verified;
        }

        public boolean isAllowContext() {
            return allowContext;
        }
    }

    public static final class ComposedResponse {
        private final String text;
        private final boolean verified;
        private final ResponseStyle style;

        private ComposedResponse(
                String text,
                boolean verified,
                ResponseStyle style) {
            this.text = text;
            this.verified = verified;
            this.style = style;
        }

        public String getText() {
            return text;
        }

        public boolean isVerified() {
            return verified;
        }

        public ResponseStyle getStyle() {
            return style;
        }
    }

    /**
     * Compõe uma resposta sem afirmar que conteúdo não verificado
     * foi confirmado.
     */
    public ComposedResponse compose(ResponseInput input) {
        if (input == null) {
            return new ComposedResponse(
                    "Não consegui preparar uma resposta válida.",
                    false,
                    ResponseStyle.NORMAL);
        }

        String content = input.getContent().trim();

        if (content.isEmpty()) {
            return new ComposedResponse(
                    "Não encontrei informações suficientes para responder.",
                    false,
                    input.getStyle());
        }

        String result = applyStyle(content, input.getStyle());

        /*
         * O contexto não é anexado automaticamente:
         * ele pode conter informações antigas ou irrelevantes.
         * A integração deve selecionar explicitamente o contexto útil.
         */
        if (input.isAllowContext()
                && !input.getContext().trim().isEmpty()) {
            // O contexto fica disponível para uso futuro e auditável.
            // Não é misturado à resposta sem uma decisão explícita.
        }

        return new ComposedResponse(
                result,
                input.isVerified(),
                input.getStyle());
    }

    /**
     * Atalho para respostas normais.
     */
    public String compose(String content) {
        return compose(new ResponseInput(
                content,
                "",
                ResponseStyle.NORMAL,
                false,
                false)).getText();
    }

    private String applyStyle(String content, ResponseStyle style) {
        switch (style) {
            case CONCISE:
                return content.trim();

            case DETAILED:
                return content.trim();

            case EMPATHETIC:
                // Não adiciona emoções presumidas nem frases automáticas.
                return content.trim();

            case TECHNICAL:
                return content.trim();

            case NORMAL:
            default:
                return content.trim();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

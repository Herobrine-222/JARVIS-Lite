
package com.jarvis.lite;

/**
 * VISION — camada complementar de raciocínio determinístico.
 *
 * Reutiliza o classificador e o motor matemático existentes.
 * Não cria memória, não acessa a rede e não executa comandos Android.
 *
 * Responsabilidades:
 * - resolver expressões aritméticas reconhecidas;
 * - recusar interpretações matemáticas ambíguas;
 * - solicitar esclarecimentos quando a intenção for desconhecida;
 * - permitir que os outros especialistas tratem suas próprias intenções.
 *
 * Esta classe não é um modelo generativo de linguagem.
 */
public final class JarvisVisionReasoningModule
        implements JarvisVisionModule {

    private static final int MAX_INPUT_LENGTH = 2000;
    private static final int MAX_EXPRESSION_LENGTH = 500;

    private static final String CLARIFICATION_RESPONSE =
            "Ainda não consegui identificar com segurança o que você "
            + "quer fazer. Pode reformular o pedido em uma frase mais "
            + "curta ou explicar qual resultado espera? Não executei "
            + "nenhuma ação no aparelho.";

    private static final String MATH_UNAVAILABLE_RESPONSE =
            "Identifiquei uma possível expressão matemática, mas não "
            + "consegui interpretá-la com segurança. Tente escrever "
            + "a conta usando números, parênteses e os sinais +, -, *, "
            + "/ ou %.";

    @Override
    public String getName() {
        return "VISION-REASONING";
    }

    /**
     * Este módulo só recebe intenções matemáticas ou desconhecidas.
     * As outras intenções continuam com seus especialistas atuais.
     */
    @Override
    public boolean supports(
            JarvisReasoningEngine.Analysis analysis
    ) {
        if (analysis == null || analysis.getIntent() == null) {
            return false;
        }

        JarvisReasoningEngine.Intent intent =
                analysis.getIntent();

        return intent
                    == JarvisReasoningEngine.Intent.MATHEMATICS
                || intent
                    == JarvisReasoningEngine.Intent.UNKNOWN;
    }

    @Override
    public String process(
            String input,
            JarvisReasoningEngine.Analysis analysis,
            JarvisVisionCore core
    ) {
        if (input == null
                || input.trim().isEmpty()
                || input.length() > MAX_INPUT_LENGTH
                || analysis == null
                || !supports(analysis)) {
            return null;
        }

        switch (analysis.getIntent()) {
            case MATHEMATICS:
                return solveMathSafely(input);

            case UNKNOWN:
                return CLARIFICATION_RESPONSE;

            default:
                return null;
        }
    }

    /**
     * Extrai uma expressão simbólica simples da mensagem.
     *
     * Exemplo aceito:
     * "Quanto é (12 + 8) / 4?"
     *
     * Expressões escritas por extenso, como "dois elevado ao
     * quadrado", não são adivinhadas por este módulo.
     */
    private String extractSymbolicExpression(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        int firstDigit = -1;
        int lastDigit = -1;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c >= '0' && c <= '9') {
                if (firstDigit < 0) {
                    firstDigit = i;
                }

                lastDigit = i;
            }
        }

        if (firstDigit < 0 || lastDigit < firstDigit) {
            return null;
        }

        int start = firstDigit;
        int end = lastDigit + 1;

        // Preserva sinais unários e parênteses antes do primeiro número.
        while (start > 0
                && isExpressionCharacter(input.charAt(start - 1))) {
            start--;
        }

        // Preserva espaços e operadores até o final da expressão.
        while (end < input.length()
                && isExpressionCharacter(input.charAt(end))) {
            end++;
        }

        String candidate =
                input.substring(start, end).trim();

        if (candidate.isEmpty()
                || candidate.length() > MAX_EXPRESSION_LENGTH) {
            return null;
        }

        boolean hasOperator = false;

        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);

            if (c >= '0' && c <= '9') {
                continue;
            }

            if (Character.isWhitespace(c)
                    || c == '.'
                    || c == ','
                    || c == '('
                    || c == ')') {
                continue;
            }

            if (c == '+'
                    || c == '-'
                    || c == '*'
                    || c == '/'
                    || c == '%'
                    || c == '×'
                    || c == '÷'
                    || c == 'x'
                    || c == 'X') {
                hasOperator = true;
                continue;
            }

            // Não tenta adivinhar o significado de palavras ou símbolos.
            return null;
        }

        return hasOperator ? candidate : null;
    }

    private boolean isExpressionCharacter(char c) {
        return (c >= '0' && c <= '9')
                || Character.isWhitespace(c)
                || c == '.'
                || c == ','
                || c == '('
                || c == ')'
                || c == '+'
                || c == '-'
                || c == '*'
                || c == '/'
                || c == '%'
                || c == '×'
                || c == '÷'
                || c == 'x'
                || c == 'X';
    }

    /**
     * Delega o cálculo ao motor matemático existente.
     * Não implementa uma segunda calculadora.
     */
    private String solveMathSafely(String input) {
        String expression = extractSymbolicExpression(input);

        if (expression == null
                || !JarvisMathEngine.canCalculate(expression)) {
            return MATH_UNAVAILABLE_RESPONSE;
        }

        try {
            String result =
                    JarvisMathEngine.calculateText(expression);

            if (result == null || result.trim().isEmpty()) {
                return MATH_UNAVAILABLE_RESPONSE;
            }

            return "Resultado: " + result.trim() + ".";

        } catch (RuntimeException error) {
            // Não expõe detalhes internos da implementação.
            return MATH_UNAVAILABLE_RESPONSE;
        }
    }
}

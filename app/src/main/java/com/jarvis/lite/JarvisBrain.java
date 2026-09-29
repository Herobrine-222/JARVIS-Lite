package com.jarvis.lite;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.Locale;

/**
 * Camada local leve de raciocínio do JARVIS.
 *
 * Não executa comandos Android, não acessa a internet e não possui
 * permissão para alterar o aparelho. É uma camada de interpretação,
 * cálculo e contexto de sessão.
 *
 * Ela é deliberadamente pequena para não transformar o A14 em um
 * servidor de modelo pesado. Um LLM on-device poderá ser conectado
 * posteriormente como outra camada, sem receber acesso direto ao Android.
 */
public final class JarvisBrain {

    private static final int MAX_TURNS = 12;

    private final Deque<Turn> turns = new ArrayDeque<>();

    public synchronized String process(
            String input,
            String memoryContext) {

        if (input == null) {
            return null;
        }

        String original = JarvisSecurity.limitText(
                input,
                JarvisSecurity.MAX_COMMAND_LENGTH
        );

        String c = normalize(original);

        if (c.isEmpty()) {
            return null;
        }

        addTurn("user", original);

        String result = null;

        /*
         * Matemática simples local.
         * Ex.: "1 + 1", "12 * 4", "(10 + 2) / 3".
         */
        if (isMathExpression(c)) {
            result = calculate(c);

        } else if (containsAny(
                c,
                "que horas sao",
                "qual a hora",
                "me diga a hora"
        )) {

            result = "Agora são "
                    + new SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                    ).format(new Date())
                    + ".";

        } else if (containsAny(
                c,
                "qual a data",
                "que dia e hoje",
                "qual e o dia de hoje"
        )) {

            result = "Hoje é "
                    + new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    ).format(new Date())
                    + ".";

        } else if (containsAny(
                c,
                "como voce esta",
                "como voce ta",
                "tudo bem com voce"
        )) {

            result = "Estou funcionando normalmente e pronto para ajudar.";

        } else if (containsAny(
                c,
                "quem e voce",
                "qual seu nome",
                "qual e seu nome"
        )) {

            result = "Eu sou o JARVIS Lite, seu assistente local.";

        } else if (containsAny(
                c,
                "o que voce lembra",
                "o que voce lembra de mim",
                "quais sao minhas memorias",
                "minhas memorias"
        )) {

            if (memoryContext == null
                    || memoryContext.trim().isEmpty()) {

                result =
                        "Minha memória local não tem informações relevantes para essa pergunta.";

            } else {

                result =
                        "O que encontrei na memória local:\n"
                                + JarvisSecurity.limitText(
                                memoryContext,
                                3000
                        );
            }

        } else if (containsAny(
                c,
                "voce esta ai",
                "jarvis esta ai"
        )) {

            result =
                    "Estou aqui. Sistemas locais operacionais. Em que posso ajudar?";

        } else if (containsAny(
                c,
                "obrigado",
                "obrigada",
                "valeu",
                "vlw"
        )) {

            result = "Por nada. Estou à sua disposição.";

        } else if (containsAny(
                c,
                "de nada",
                "por nada"
        )) {

            result = "Sempre à disposição.";

        } else if (containsAny(
                c,
                "como voce funciona",
                "como funciona sua memoria"
        )) {

            result =
                    "Eu trabalho em camadas: entrada, interpretação local, memória, ferramentas seguras e resposta. Um modelo local poderá ampliar a conversa sem receber acesso irrestrito ao Android.";
        }

        if (result != null) {
            addTurn("assistant", result);
        }

        return result;
    }

    public synchronized String getSessionContext() {
        StringBuilder out = new StringBuilder();

        for (Turn turn : turns) {

            if (out.length() > 0) {
                out.append('\n');
            }

            out.append(turn.role)
                    .append(": ")
                    .append(turn.text);
        }

        return out.toString();
    }

    public synchronized void clearSession() {
        turns.clear();
    }

    private void addTurn(
            String role,
            String text) {

        turns.addLast(
                new Turn(
                        role,
                        JarvisSecurity.limitText(
                                text,
                                2000
                        )
                )
        );

        while (turns.size() > MAX_TURNS) {
            turns.removeFirst();
        }
    }

    private boolean isMathExpression(String c) {

        if (c.length() > 80) {
            return false;
        }

        if (!c.matches("[0-9+\\-*/().,% x]+")) {
            return false;
        }

        return c.matches(".*[+\\-*/%].*");
    }

    private String calculate(String c) {

        try {

            String expr =
                    c.replace("x", "*")
                            .replace(',', '.')
                            .replace(" ", "");

            double value =
                    evaluate(expr);

            if (Double.isNaN(value)
                    || Double.isInfinite(value)) {

                return "Não consigo calcular esse resultado com segurança.";
            }

            if (Math.rint(value) == value) {

                return "O resultado é "
                        + String.format(
                        Locale.US,
                        "%.0f",
                        value
                )
                        + ".";
            }

            String formatted =
                    String.format(
                            Locale.US,
                            "%.6f",
                            value
                    )
                            .replaceAll(
                                    "0+$",
                                    ""
                            )
                            .replaceAll(
                                    "\\.$",
                                    ""
                            );

            return "O resultado é "
                    + formatted
                    + ".";

        } catch (Exception ignored) {

            return null;
        }
    }

    /*
     * Avaliador aritmético local com precedência e parênteses.
     * Não executa código e não interpreta comandos do sistema.
     */
    private double evaluate(String s) {

        ArrayDeque<Double> values =
                new ArrayDeque<>();

        ArrayDeque<Character> ops =
                new ArrayDeque<>();

        int i = 0;

        while (i < s.length()) {

            char ch = s.charAt(i);

            if (Character.isDigit(ch)
                    || ch == '.') {

                int start = i++;

                while (
                        i < s.length()
                                && (
                                Character.isDigit(
                                        s.charAt(i)
                                )
                                        || s.charAt(i) == '.'
                        )
                ) {
                    i++;
                }

                values.push(
                        Double.parseDouble(
                                s.substring(
                                        start,
                                        i
                                )
                        )
                );

                continue;
            }

            if (ch == '(') {

                ops.push(ch);

            } else if (ch == ')') {

                while (
                        !ops.isEmpty()
                                && ops.peek() != '('
                ) {

                    applyTop(
                            values,
                            ops.pop()
                    );
                }

                if (ops.isEmpty()) {
                    throw new IllegalArgumentException();
                }

                ops.pop();

            } else if (isOperator(ch)) {

                while (
                        !ops.isEmpty()
                                && ops.peek() != '('
                                && precedence(
                                ops.peek()
                        ) >= precedence(ch)
                ) {

                    applyTop(
                            values,
                            ops.pop()
                    );
                }

                ops.push(ch);

            } else {

                throw new IllegalArgumentException();
            }

            i++;
        }

        while (!ops.isEmpty()) {

            if (ops.peek() == '(') {
                throw new IllegalArgumentException();
            }

            applyTop(
                    values,
                    ops.pop()
            );
        }

        if (values.size() != 1) {
            throw new IllegalArgumentException();
        }

        return values.pop();
    }

    private void applyTop(
            ArrayDeque<Double> values,
            char op) {

        if (values.size() < 2) {
            throw new IllegalArgumentException();
        }

        double right = values.pop();
        double left = values.pop();

        switch (op) {

            case '+':
                values.push(left + right);
                break;

            case '-':
                values.push(left - right);
                break;

            case '*':
                values.push(left * right);
                break;

            case '/':
                if (right == 0) {
                    throw new ArithmeticException();
                }
                values.push(left / right);
                break;

            case '%':
                if (right == 0) {
                    throw new ArithmeticException();
                }
                values.push(left % right);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    private boolean isOperator(char c) {
        return c == '+'
                || c == '-'
                || c == '*'
                || c == '/'
                || c == '%';
    }

    private int precedence(char c) {
        return (c == '+' || c == '-')
                ? 1
                : 2;
    }

    private boolean containsAny(
            String text,
            String... terms) {

        for (String term : terms) {

            String normalized =
                    normalize(term);

            if (text.equals(normalized)
                    || text.contains(normalized)) {

                return true;
            }
        }

        return false;
    }

    private String normalize(
            String text) {

        String base =
                text == null
                        ? ""
                        : text.toLowerCase(
                        Locale.ROOT
                ).trim();

        base =
                Normalizer.normalize(
                        base,
                        Normalizer.Form.NFD
                );

        return base
                .replaceAll(
                        "\\p{M}+",
                        ""
                )
                .replaceAll(
                        "[^a-z0-9+\\-*/().%, x]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private static final class Turn {

        final String role;
        final String text;

        Turn(
                String role,
                String text) {

            this.role = role;
            this.text = text;
        }
    }
}

package com.jarvis.lite;

import android.content.Context;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Camada local leve de raciocínio do JARVIS.
 *
 * Responsabilidades:
 * - interpretar comandos locais;
 * - responder conversas básicas;
 * - realizar cálculos matemáticos;
 * - trabalhar com data e hora;
 * - consultar memória local;
 * - manter contexto da sessão;
 * - consultar ferramentas locais através das camadas apropriadas.
 *
 * Não acessa a internet.
 * Não executa comandos Android diretamente.
 * Não altera configurações do aparelho.
 */
public final class JarvisBrain {

    private static final int MAX_TURNS = 12;
    private static final int MAX_CONTEXT_LENGTH = 6000;

    private final Deque<Turn> turns =
            new ArrayDeque<>();

    private final JarvisContextManager contextManager;

    /**
     * Construtor recomendado.
     */
    public JarvisBrain(Context context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "Context não pode ser nulo."
            );
        }

        contextManager =
                new JarvisContextManager(
                        context.getApplicationContext()
                );
    }

    /**
     * Mantém compatibilidade com código antigo
     * que criava o cérebro sem Context.
     *
     * Nesse modo a memória/contexto avançado
     * não fica disponível.
     */
    public JarvisBrain() {
        contextManager = null;
    }

    // ============================================================
    // PROCESSAMENTO PRINCIPAL
    // ============================================================

    public synchronized String process(
            String input,
            String memoryContext
    ) {

        if (input == null) {
            return null;
        }

        String original =
                JarvisSecurity.limitText(
                        input,
                        JarvisSecurity.MAX_COMMAND_LENGTH
                );

        String c =
                normalize(original);

        if (c.isEmpty()) {
            return null;
        }

        addTurn(
                "user",
                original
        );

        if (contextManager != null) {

            contextManager.addUserMessage(
                    original
            );
        }

        String result = null;

        // ========================================================
        // MATEMÁTICA
        // ========================================================

        if (isMathExpression(c)) {

            result =
                    calculate(c);

        // ========================================================
        // HORA
        // ========================================================

        } else if (containsAny(
                c,
                "que horas sao",
                "qual a hora",
                "me diga a hora",
                "que horas e",
                "horas agora"
        )) {

            result =
                    "Agora são "
                            + new SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    )
                            + ".";

        // ========================================================
        // DATA
        // ========================================================

        } else if (containsAny(
                c,
                "qual a data",
                "que dia e hoje",
                "qual e o dia de hoje",
                "data de hoje"
        )) {

            result =
                    "Hoje é "
                            + new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    )
                            + ".";

        // ========================================================
        // COMO ESTÁ
        // ========================================================

        } else if (containsAny(
                c,
                "como voce esta",
                "como voce ta",
                "tudo bem com voce",
                "como voce se sente"
        )) {

            result =
                    "Estou funcionando normalmente e pronto para ajudar.";

        // ========================================================
        // IDENTIDADE
        // ========================================================

        } else if (containsAny(
                c,
                "quem e voce",
                "qual seu nome",
                "qual e seu nome",
                "quem voce e"
        )) {

            result =
                    "Eu sou o JARVIS Lite, seu assistente local.";

        // ========================================================
        // MEMÓRIA
        // ========================================================

        } else if (containsAny(
                c,
                "o que voce lembra",
                "o que voce lembra de mim",
                "quais sao minhas memorias",
                "minhas memorias",
                "o que voce sabe sobre mim"
        )) {

            result =
                    processMemoryRequest(
                            original,
                            memoryContext
                    );

        // ========================================================
        // JARVIS ESTÁ AÍ
        // ========================================================

        } else if (containsAny(
                c,
                "voce esta ai",
                "jarvis esta ai",
                "jarvis voce esta ai"
        )) {

            result =
                    "Estou aqui. Sistemas locais operacionais. Em que posso ajudar?";

        // ========================================================
        // AGRADECIMENTO
        // ========================================================

        } else if (containsAny(
                c,
                "obrigado",
                "obrigada",
                "valeu",
                "vlw"
        )) {

            result =
                    "Por nada. Estou à sua disposição.";

        // ========================================================
        // DE NADA
        // ========================================================

        } else if (containsAny(
                c,
                "de nada",
                "por nada"
        )) {

            result =
                    "Sempre à disposição.";

        // ========================================================
        // COMO FUNCIONA
        // ========================================================

        } else if (containsAny(
                c,
                "como voce funciona",
                "como funciona sua memoria",
                "como funciona sua memoria local"
        )) {

            result =
                    "Eu trabalho em camadas: entrada, interpretação local, memória, ferramentas seguras e resposta. Minha memória local pode guardar informações que você pedir para eu lembrar. Um modelo local poderá ampliar a conversa posteriormente sem receber acesso irrestrito ao Android.";

        // ========================================================
        // BOM DIA
        // ========================================================

        } else if (containsAny(
                c,
                "bom dia"
        )) {

            result =
                    "Bom dia. Estou à sua disposição.";

        // ========================================================
        // BOA TARDE
        // ========================================================

        } else if (containsAny(
                c,
                "boa tarde"
        )) {

            result =
                    "Boa tarde. Estou à sua disposição.";

        // ========================================================
        // BOA NOITE
        // ========================================================

        } else if (containsAny(
                c,
                "boa noite"
        )) {

            result =
                    "Boa noite. Estou à sua disposição.";

        // ========================================================
        // OPA
        // ========================================================

        } else if (containsAny(
                c,
                "opa",
                "oi jarvis",
                "ola jarvis",
                "olá jarvis"
        )) {

            result =
                    "Opa. Estou à sua disposição. Em que posso ajudar?";

        // ========================================================
        // DEUS TE ABENÇOE
        // ========================================================

        } else if (containsAny(
                c,
                "deus te abencoe",
                "deus te abençoe"
        )) {

            result =
                    "Amém. Muito obrigado. Que Deus abençoe você também.";

        // ========================================================
        // DURMA BEM
        // ========================================================

        } else if (containsAny(
                c,
                "durma bem",
                "boa noite jarvis"
        )) {

            result =
                    "Obrigado. Desejo uma boa noite para você também.";

        // ========================================================
        // SESSÃO
        // ========================================================

        } else if (containsAny(
                c,
                "encerrar sessao",
                "encerre a sessao",
                "encerrar a sessao",
                "finalizar sessao"
        )) {

            result =
                    "Sessão encerrada. Ficarei em espera.";

        // ========================================================
        // NÃO ENTENDIDO
        // ========================================================

        } else {

            result =
                    fallback(
                            original
                    );
        }

        if (result != null) {

            addTurn(
                    "assistant",
                    result
            );

            if (contextManager != null) {

                contextManager.addJarvisMessage(
                        result
                );
            }
        }

        return result;
    }

    // ============================================================
    // PROCESSAMENTO SIMPLIFICADO
    // ============================================================

    public synchronized String process(
            String input
    ) {

        String memoryContext = "";

        if (contextManager != null &&
                input != null) {

            memoryContext =
                    contextManager
                            .getRelevantMemory(
                                    input
                            );
        }

        return process(
                input,
                memoryContext
        );
    }

    // ============================================================
    // MEMÓRIA
    // ============================================================

    private String processMemoryRequest(
            String input,
            String suppliedMemoryContext
    ) {

        String memoryContext =
                suppliedMemoryContext;

        if (
                memoryContext == null ||
                memoryContext.trim().isEmpty()
        ) {

            if (contextManager != null) {

                memoryContext =
                        contextManager
                                .getRelevantMemory(
                                        input
                                );
            }
        }

        if (
                memoryContext == null ||
                memoryContext.trim().isEmpty()
        ) {

            if (contextManager != null) {

                List<String> memories =
                        contextManager
                                .getMemory()
                                .getAll();

                if (!memories.isEmpty()) {

                    StringBuilder all =
                            new StringBuilder();

                    int limit =
                            Math.min(
                                    memories.size(),
                                    10
                            );

                    for (
                            int i = 0;
                            i < limit;
                            i++
                    ) {

                        if (all.length() > 0) {
                            all.append("\n");
                        }

                        all.append(
                                memories.get(i)
                        );
                    }

                    memoryContext =
                            all.toString();
                }
            }
        }

        if (
                memoryContext == null ||
                memoryContext.trim().isEmpty()
        ) {

            return
                    "Minha memória local não tem informações relevantes para essa pergunta.";
        }

        return
                "O que encontrei na memória local:\n"
                        + JarvisSecurity.limitText(
                        memoryContext,
                        3000
                );
    }

    // ============================================================
    // ADICIONAR MEMÓRIA
    // ============================================================

    public synchronized boolean remember(
            String text
    ) {

        if (contextManager == null) {
            return false;
        }

        return contextManager.remember(
                text
        );
    }

    public synchronized boolean remember(
            String text,
            String category
    ) {

        if (contextManager == null) {
            return false;
        }

        return contextManager.remember(
                text,
                category
        );
    }

    // ============================================================
    // ESQUECER MEMÓRIA
    // ============================================================

    public synchronized int forget(
            String query
    ) {

        if (contextManager == null) {
            return 0;
        }

        return contextManager.forget(
                query
        );
    }

    // ============================================================
    // CONTEXTO DA SESSÃO
    // ============================================================

    public synchronized String getSessionContext() {

        StringBuilder out =
                new StringBuilder();

        for (Turn turn : turns) {

            if (out.length() > 0) {
                out.append('\n');
            }

            out.append(
                    turn.role
            )
                    .append(": ")
                    .append(
                            turn.text
                    );
        }

        return out.toString();
    }

    // ============================================================
    // CONTEXTO COMPLETO
    // ============================================================

    public synchronized String getFullContext(
            String currentMessage
    ) {

        if (contextManager == null) {

            return JarvisSecurity.limitText(
                    getSessionContext(),
                    MAX_CONTEXT_LENGTH
            );
        }

        return JarvisSecurity.limitText(
                contextManager.buildFullContext(
                        currentMessage
                ),
                MAX_CONTEXT_LENGTH
        );
    }

    // ============================================================
    // LIMPAR SESSÃO
    // ============================================================

    public synchronized void clearSession() {

        turns.clear();

        if (contextManager != null) {

            contextManager.clearConversation();
        }
    }

    // ============================================================
    // QUANTIDADE DE TURNOS
    // ============================================================

    public synchronized int getSessionSize() {

        return turns.size();
    }

    // ============================================================
    // VERIFICAR SESSÃO
    // ============================================================

    public synchronized boolean hasSession() {

        return !turns.isEmpty();
    }

    // ============================================================
    // ACESSAR GERENCIADOR DE CONTEXTO
    // ============================================================

    public JarvisContextManager
    getContextManager() {

        return contextManager;
    }

    // ============================================================
    // FALLBACK
    // ============================================================

    private String fallback(
            String input
    ) {

        if (
                input == null ||
                input.trim().isEmpty()
        ) {

            return null;
        }

        /*
         * O cérebro local não inventa uma resposta
         * para perguntas que ele ainda não conhece.
         *
         * Uma camada de IA mais avançada poderá ser
         * conectada posteriormente.
         */

        return
                "Ainda não tenho uma resposta local para isso. Posso trabalhar com comandos, memória, informações do aparelho, cálculos e outras funções que forem adicionadas ao JARVIS.";
    }

    // ============================================================
    // MATEMÁTICA
    // ============================================================

    private boolean isMathExpression(
            String c
    ) {

        if (c.length() > 80) {
            return false;
        }

        if (!c.matches(
                "[0-9+\\-*/().,% x]+"
        )) {

            return false;
        }

        return c.matches(
                ".*[+\\-*/%].*"
        );
    }

    private String calculate(
            String c
    ) {

        try {

            String expr =
                    c.replace(
                            "x",
                            "*"
                    )
                            .replace(
                                    ',',
                                    '.'
                            )
                            .replace(
                                    " ",
                                    ""
                            );

            double value =
                    evaluate(
                            expr
                    );

            if (
                    Double.isNaN(
                            value
                    )
                            ||
                            Double.isInfinite(
                                    value
                            )
            ) {

                return
                        "Não consigo calcular esse resultado com segurança.";
            }

            if (
                    Math.rint(
                            value
                    ) == value
            ) {

                return
                        "O resultado é "
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

            return
                    "O resultado é "
                            + formatted
                            + ".";

        } catch (Exception ignored) {

            return
                    "Não consegui calcular essa expressão.";
        }
    }

    // ============================================================
    // AVALIADOR MATEMÁTICO
    // ============================================================

    private double evaluate(
            String s
    ) {

        ArrayDeque<Double> values =
                new ArrayDeque<>();

        ArrayDeque<Character> ops =
                new ArrayDeque<>();

        int i = 0;

        while (i < s.length()) {

            char ch =
                    s.charAt(i);

            if (
                    Character.isDigit(ch)
                            ||
                            ch == '.'
            ) {

                int start =
                        i++;

                while (
                        i < s.length()
                                &&
                                (
                                        Character.isDigit(
                                                s.charAt(i)
                                        )
                                                ||
                                                s.charAt(i) == '.'
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

                ops.push(
                        ch
                );

            } else if (ch == ')') {

                while (
                        !ops.isEmpty()
                                &&
                                ops.peek()
                                        != '('
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

            } else if (
                    isOperator(
                            ch
                    )
            ) {

                while (
                        !ops.isEmpty()
                                &&
                                ops.peek()
                                        != '('
                                &&
                                precedence(
                                        ops.peek()
                                )
                                        >=
                                        precedence(
                                                ch
                                        )
                ) {

                    applyTop(
                            values,
                            ops.pop()
                    );
                }

                ops.push(
                        ch
                );

            } else {

                throw new IllegalArgumentException();
            }

            i++;
        }

        while (!ops.isEmpty()) {

            if (
                    ops.peek()
                            == '('
            ) {

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

    // ============================================================
    // APLICAR OPERADOR
    // ============================================================

    private void applyTop(
            ArrayDeque<Double> values,
            char op
    ) {

        if (values.size() < 2) {

            throw new IllegalArgumentException();
        }

        double right =
                values.pop();

        double left =
                values.pop();

        switch (op) {

            case '+':

                values.push(
                        left + right
                );

                break;

            case '-':

                values.push(
                        left - right
                );

                break;

            case '*':

                values.push(
                        left * right
                );

                break;

            case '/':

                if (right == 0) {

                    throw new ArithmeticException();
                }

                values.push(
                        left / right
                );

                break;

            case '%':

                if (right == 0) {

                    throw new ArithmeticException();
                }

                values.push(
                        left % right
                );

                break;

            default:

                throw new IllegalArgumentException();
        }
    }

    // ============================================================
    // OPERADOR
    // ============================================================

    private boolean isOperator(
            char c
    ) {

        return c == '+'
                || c == '-'
                || c == '*'
                || c == '/'
                || c == '%';
    }

    // ============================================================
    // PRECEDÊNCIA
    // ============================================================

    private int precedence(
            char c
    ) {

        return (
                c == '+'
                        ||
                        c == '-'
        )
                ? 1
                : 2;
    }

    // ============================================================
    // PROCURAR TERMOS
    // ============================================================

    private boolean containsAny(
            String text,
            String... terms
    ) {

        for (String term : terms) {

            String normalized =
                    normalize(
                            term
                    );

            if (
                    text.equals(
                            normalized
                    )
                            ||
                            text.contains(
                                    normalized
                            )
            ) {

                return true;
            }
        }

        return false;
    }

    // ============================================================
    // NORMALIZAÇÃO
    // ============================================================

    private String normalize(
            String text
    ) {

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

    // ============================================================
    // TURNO DA SESSÃO
    // ============================================================

    private void addTurn(
            String role,
            String text
    ) {

        if (text == null) {
            return;
        }

        turns.addLast(
                new Turn(
                        role,
                        JarvisSecurity.limitText(
                                text,
                                2000
                        )
                )
        );

        while (
                turns.size()
                        > MAX_TURNS
        ) {

            turns.removeFirst();
        }
    }

    // ============================================================
    // CLASSE DE TURNO
    // ============================================================

    private static final class Turn {

        final String role;
        final String text;

        Turn(
                String role,
                String text
        ) {

            this.role = role;
            this.text = text;
        }
    }
}

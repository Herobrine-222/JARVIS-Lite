package com.jarvis.lite;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Motor matemático exato do JARVIS.
 *
 * Não utiliza double ou float para representar os resultados.
 *
 * Suporta:
 * - números inteiros;
 * - números decimais finitos;
 * - frações;
 * - adição;
 * - subtração;
 * - multiplicação;
 * - divisão;
 * - módulo;
 * - parênteses;
 * - números negativos;
 * - simplificação automática de frações;
 * - BigInteger para números inteiros muito grandes.
 *
 * Exemplos:
 *
 * 2 + 2       -> 4
 * 10 / 4      -> 5/2
 * 2/3 + 1/3   -> 1
 * 1/2 + 1/3   -> 5/6
 * (2 + 3) * 4 -> 20
 */
public final class JarvisMathEngine {

    private JarvisMathEngine() {
        // Classe utilitária.
    }

    // ============================================================
    // RESULTADO
    // ============================================================

    /**
     * Calcula uma expressão matemática e retorna o resultado
     * em representação exata.
     *
     * @param expression expressão matemática
     * @return resultado exato
     * @throws IllegalArgumentException se a expressão for inválida
     * @throws ArithmeticException se houver divisão por zero
     */
    public static Fraction calculate(
            String expression
    ) {

        if (expression == null) {
            throw new IllegalArgumentException(
                    "Expressão nula."
            );
        }

        String input =
                expression.trim();

        if (input.isEmpty()) {
            throw new IllegalArgumentException(
                    "Expressão vazia."
            );
        }

        return evaluate(
                input
        );
    }

    /**
     * Calcula uma expressão e devolve diretamente
     * a representação textual exata.
     *
     * @param expression expressão matemática
     * @return resultado exato em texto
     */
    public static String calculateText(
            String expression
    ) {

        return calculate(
                expression
        ).toString();
    }

    /**
     * Verifica se uma expressão pode ser processada
     * pelo motor matemático.
     *
     * Não lança exceção para entradas inválidas.
     */
    public static boolean canCalculate(
            String expression
    ) {

        if (expression == null
                || expression.trim().isEmpty()) {

            return false;
        }

        try {

            calculate(
                    expression
            );

            return true;

        } catch (Exception ignored) {

            return false;
        }
    }

    // ============================================================
    // AVALIAÇÃO
    // ============================================================

    private static Fraction evaluate(
            String expression
    ) {

        String input =
                normalizeExpression(
                        expression
                );

        if (input.isEmpty()) {

            throw new IllegalArgumentException(
                    "Expressão vazia."
            );
        }

        Deque<Fraction> values =
                new ArrayDeque<>();

        Deque<Character> operators =
                new ArrayDeque<>();

        int index = 0;

        boolean expectValue = true;

        while (
                index < input.length()
        ) {

            char current =
                    input.charAt(index);

            // ----------------------------------------------------
            // NÚMERO
            // ----------------------------------------------------

            if (
                    Character.isDigit(
                            current
                    )
                    ||
                    current == '.'
                    ||
                    current == ','
            ) {

                NumberToken token =
                        readNumber(
                                input,
                                index
                        );

                values.push(
                        Fraction.parse(
                                token.text
                        )
                );

                index =
                        token.nextIndex;

                expectValue = false;

                continue;
            }

            // ----------------------------------------------------
            // PARÊNTESE ABERTO
            // ----------------------------------------------------

            if (current == '(') {

                operators.push(
                        current
                );

                index++;

                expectValue = true;

                continue;
            }

            // ----------------------------------------------------
            // PARÊNTESE FECHADO
            // ----------------------------------------------------

            if (current == ')') {

                if (expectValue) {

                    throw new IllegalArgumentException(
                            "Parêntese fechado em posição inválida."
                    );
                }

                while (
                        !operators.isEmpty()
                        &&
                        operators.peek() != '('
                ) {

                    applyTop(
                            values,
                            operators.pop()
                    );
                }

                if (operators.isEmpty()
                        ||
                        operators.peek() != '(') {

                    throw new IllegalArgumentException(
                            "Parênteses incompatíveis."
                    );
                }

                operators.pop();

                index++;

                expectValue = false;

                continue;
            }

            // ----------------------------------------------------
            // OPERADOR
            // ----------------------------------------------------

            if (isOperator(current)) {

                // ------------------------------------------------
                // SINAL UNÁRIO
                // ------------------------------------------------

                if (
                        expectValue
                        &&
                        (
                                current == '+'
                                ||
                                current == '-'
                        )
                ) {

                    /*
                     * Transformamos:
                     *
                     * -5
                     *
                     * em:
                     *
                     * 0 - 5
                     *
                     * Isso também permite:
                     *
                     * 2 * -3
                     * 2 + -3
                     * -(2 + 3)
                     */

                    if (current == '-') {

                        values.push(
                                Fraction.ZERO
                        );

                        while (
                                !operators.isEmpty()
                                &&
                                operators.peek() != '('
                                &&
                                precedence(
                                        operators.peek()
                                ) >= precedence('-')
                        ) {

                            applyTop(
                                    values,
                                    operators.pop()
                            );
                        }

                        operators.push(
                                '-'
                        );
                    }

                    index++;

                    expectValue = true;

                    continue;
                }

                if (expectValue) {

                    throw new IllegalArgumentException(
                            "Operador em posição inválida."
                    );
                }

                while (
                        !operators.isEmpty()
                        &&
                        operators.peek() != '('
                        &&
                        precedence(
                                operators.peek()
                        )
                        >=
                        precedence(
                                current
                        )
                ) {

                    applyTop(
                            values,
                            operators.pop()
                    );
                }

                operators.push(
                        current
                );

                index++;

                expectValue = true;

                continue;
            }

            throw new IllegalArgumentException(
                    "Caractere inválido: "
                            + current
            );
        }

        if (expectValue) {

            throw new IllegalArgumentException(
                    "A expressão terminou com um operador."
            );
        }

        while (
                !operators.isEmpty()
        ) {

            if (
                    operators.peek() == '('
                    ||
                    operators.peek() == ')'
            ) {

                throw new IllegalArgumentException(
                        "Parênteses incompatíveis."
                );
            }

            applyTop(
                    values,
                    operators.pop()
            );
        }

        if (values.size() != 1) {

            throw new IllegalArgumentException(
                    "Expressão matemática inválida."
            );
        }

        return values.pop();
    }

    // ============================================================
    // LEITURA DE NÚMERO
    // ============================================================

    private static NumberToken readNumber(
            String input,
            int start
    ) {

        int index =
                start;

        boolean decimalFound =
                false;

        boolean digitFound =
                false;

        while (
                index < input.length()
        ) {

            char c =
                    input.charAt(index);

            if (
                    Character.isDigit(c)
            ) {

                digitFound = true;

                index++;

                continue;
            }

            if (
                    c == '.'
                    ||
                    c == ','
            ) {

                if (decimalFound) {

                    throw new IllegalArgumentException(
                            "Número decimal inválido."
                    );
                }

                decimalFound = true;

                index++;

                continue;
            }

            break;
        }

        if (!digitFound) {

            throw new IllegalArgumentException(
                    "Número inválido."
            );
        }

        String text =
                input.substring(
                        start,
                        index
                );

        /*
         * Aceitamos vírgula como separador decimal
         * para usuários brasileiros.
         */
        text =
                text.replace(
                        ',',
                        '.'
                );

        return new NumberToken(
                text,
                index
        );
    }

    // ============================================================
    // APLICAÇÃO DE OPERADORES
    // ============================================================

    private static void applyTop(
            Deque<Fraction> values,
            char operator
    ) {

        if (values.size() < 2) {

            throw new IllegalArgumentException(
                    "Quantidade insuficiente de valores."
            );
        }

        Fraction right =
                values.pop();

        Fraction left =
                values.pop();

        Fraction result;

        switch (operator) {

            case '+':

                result =
                        left.add(
                                right
                        );

                break;

            case '-':

                result =
                        left.subtract(
                                right
                        );

                break;

            case '*':

                result =
                        left.multiply(
                                right
                        );

                break;

            case '/':

                result =
                        left.divide(
                                right
                        );

                break;

            case '%':

                result =
                        left.mod(
                                right
                        );

                break;

            default:

                throw new IllegalArgumentException(
                        "Operador desconhecido: "
                                + operator
                );
        }

        values.push(
                result
        );
    }

    // ============================================================
    // OPERADORES
    // ============================================================

    private static boolean isOperator(
            char c
    ) {

        return c == '+'
                || c == '-'
                || c == '*'
                || c == '/'
                || c == '%';
    }

    private static int precedence(
            char c
    ) {

        if (
                c == '+'
                ||
                c == '-'
        ) {

            return 1;
        }

        if (
                c == '*'
                ||
                c == '/'
                ||
                c == '%'
        ) {

            return 2;
        }

        return 0;
    }

    // ============================================================
    // NORMALIZAÇÃO
    // ============================================================

    private static String normalizeExpression(
            String expression
    ) {

        String input =
                expression
                        .trim()
                        .replace(
                                '×',
                                '*'
                        )
                        .replace(
                                '÷',
                                '/'
                        );

        /*
         * "x" também pode ser usado como multiplicação.
         *
         * Só fazemos isso quando o caractere aparece
         * sozinho como operador. Para manter o motor
         * simples e previsível, o JARVIS deverá enviar
         * expressões usando "*" internamente.
         */
        input =
                input.replace(
                        'X',
                        '*'
                );

        input =
                input.replace(
                        'x',
                        '*'
                );

        /*
         * Removemos espaços.
         */
        input =
                input.replaceAll(
                        "\\s+",
                        ""
                );

        return input;
    }

    // ============================================================
    // TOKEN NUMÉRICO
    // ============================================================

    private static final class NumberToken {

        final String text;

        final int nextIndex;

        NumberToken(
                String text,
                int nextIndex
        ) {

            this.text =
                    text;

            this.nextIndex =
                    nextIndex;
        }
    }

    // ============================================================
    // FRAÇÃO EXATA
    // ============================================================

    /**
     * Representa um número racional exatamente como:
     *
     * numerador / denominador
     *
     * O denominador nunca é zero.
     *
     * A fração é sempre reduzida.
     */
    public static final class Fraction {

        public static final Fraction ZERO =
                new Fraction(
                        BigInteger.ZERO,
                        BigInteger.ONE
                );

        public static final Fraction ONE =
                new Fraction(
                        BigInteger.ONE,
                        BigInteger.ONE
                );

        private final BigInteger numerator;

        private final BigInteger denominator;

        // --------------------------------------------------------
        // CONSTRUTOR
        // --------------------------------------------------------

        public Fraction(
                BigInteger numerator,
                BigInteger denominator
        ) {

            if (numerator == null
                    ||
                    denominator == null) {

                throw new IllegalArgumentException(
                        "Numerador e denominador não podem ser nulos."
                );
            }

            if (
                    denominator.equals(
                            BigInteger.ZERO
                    )
            ) {

                throw new ArithmeticException(
                        "Divisão por zero."
                );
            }

            /*
             * Mantém o sinal no numerador.
             */
            if (
                    denominator.signum() < 0
            ) {

                numerator =
                        numerator.negate();

                denominator =
                        denominator.negate();
            }

            /*
             * Redução automática.
             *
             * Exemplo:
             *
             * 10/20 -> 1/2
             */
            BigInteger gcd =
                    numerator.gcd(
                            denominator
                    );

            if (
                    !gcd.equals(
                            BigInteger.ONE
                    )
            ) {

                numerator =
                        numerator.divide(
                                gcd
                        );

                denominator =
                        denominator.divide(
                                gcd
                        );
            }

            this.numerator =
                    numerator;

            this.denominator =
                    denominator;
        }

        // --------------------------------------------------------
        // PARSER
        // --------------------------------------------------------

        public static Fraction parse(
                String value
        ) {

            if (value == null
                    ||
                    value.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Número vazio."
                );
            }

            String text =
                    value.trim()
                            .replace(
                                    ',',
                                    '.'
                            );

            /*
             * Fração escrita diretamente:
             *
             * 2/3
             *
             * Normalmente as divisões são tratadas pelo
             * parser principal, mas deixamos este suporte
             * para utilização direta da classe.
             */
            int slash =
                    text.indexOf('/');

            if (slash > 0
                    &&
                    slash < text.length() - 1
                    &&
                    text.indexOf(
                            '/',
                            slash + 1
                    ) == -1) {

                BigInteger n =
                        new BigInteger(
                                text.substring(
                                        0,
                                        slash
                                )
                        );

                BigInteger d =
                        new BigInteger(
                                text.substring(
                                        slash + 1
                                )
                        );

                return new Fraction(
                        n,
                        d
                );
            }

            /*
             * Inteiro.
             */
            if (
                    !text.contains(".")
            ) {

                return new Fraction(
                        new BigInteger(
                                text
                        ),
                        BigInteger.ONE
                );
            }

            /*
             * Decimal exato.
             *
             * Exemplo:
             *
             * 1.25
             *
             * vira:
             *
             * 125/100
             *
             * e depois:
             *
             * 5/4
             */
            boolean negative =
                    text.startsWith("-");

            String unsigned =
                    negative
                            ? text.substring(1)
                            : text;

            int decimalPosition =
                    unsigned.indexOf('.');

            String integerPart =
                    unsigned.substring(
                            0,
                            decimalPosition
                    );

            String decimalPart =
                    unsigned.substring(
                            decimalPosition + 1
                    );

            if (integerPart.isEmpty()) {
                integerPart = "0";
            }

            if (decimalPart.isEmpty()) {

                throw new IllegalArgumentException(
                        "Número decimal inválido."
                );
            }

            String digits =
                    integerPart
                            + decimalPart;

            BigInteger numerator =
                    new BigInteger(
                            digits
                    );

            BigInteger denominator =
                    BigInteger.TEN.pow(
                            decimalPart.length()
                    );

            if (negative) {

                numerator =
                        numerator.negate();
            }

            return new Fraction(
                    numerator,
                    denominator
            );
        }

        // --------------------------------------------------------
        // OPERAÇÕES
        // --------------------------------------------------------

        public Fraction add(
                Fraction other
        ) {

            checkOther(
                    other
            );

            BigInteger n =
                    numerator
                            .multiply(
                                    other.denominator
                            )
                            .add(
                                    other.numerator
                                            .multiply(
                                                    denominator
                                            )
                            );

            BigInteger d =
                    denominator
                            .multiply(
                                    other.denominator
                            );

            return new Fraction(
                    n,
                    d
            );
        }

        public Fraction subtract(
                Fraction other
        ) {

            checkOther(
                    other
            );

            BigInteger n =
                    numerator
                            .multiply(
                                    other.denominator
                            )
                            .subtract(
                                    other.numerator
                                            .multiply(
                                                    denominator
                                            )
                            );

            BigInteger d =
                    denominator
                            .multiply(
                                    other.denominator
                            );

            return new Fraction(
                    n,
                    d
            );
        }

        public Fraction multiply(
                Fraction other
        ) {

            checkOther(
                    other
            );

            return new Fraction(
                    numerator.multiply(
                            other.numerator
                    ),
                    denominator.multiply(
                            other.denominator
                    )
            );
        }

        public Fraction divide(
                Fraction other
        ) {

            checkOther(
                    other
            );

            if (
                    other.numerator.equals(
                            BigInteger.ZERO
                    )
            ) {

                throw new ArithmeticException(
                        "Divisão por zero."
                );
            }

            return new Fraction(
                    numerator.multiply(
                            other.denominator
                    ),
                    denominator.multiply(
                            other.numerator
                    )
            );
        }

        /**
         * Módulo exato para números inteiros.
         *
         * Para manter o significado matemático previsível,
         * o módulo com valores não inteiros é rejeitado.
         */
        public Fraction mod(
                Fraction other
        ) {

            checkOther(
                    other
            );

            if (
                    other.isZero()
            ) {

                throw new ArithmeticException(
                        "Módulo por zero."
                );
            }

            if (
                    !isInteger()
                    ||
                    !other.isInteger()
            ) {

                throw new ArithmeticException(
                        "O operador % exige números inteiros."
                );
            }

            return new Fraction(
                    numerator.remainder(
                            other.numerator
                    ),
                    BigInteger.ONE
            );
        }

        // --------------------------------------------------------
        // CONSULTAS
        // --------------------------------------------------------

        public BigInteger getNumerator() {

            return numerator;
        }

        public BigInteger getDenominator() {

            return denominator;
        }

        public boolean isInteger() {

            return denominator.equals(
                    BigInteger.ONE
            );
        }

        public boolean isZero() {

            return numerator.equals(
                    BigInteger.ZERO
            );
        }

        public boolean isNegative() {

            return numerator.signum() < 0;
        }

        // --------------------------------------------------------
        // DECIMAL EXATO
        // --------------------------------------------------------

        /**
         * Retorna um decimal exato somente quando a fração
         * possui representação decimal finita.
         *
         * Exemplos:
         *
         * 1/2 -> 0.5
         * 5/4 -> 1.25
         *
         * 1/3 -> null
         */
        public String toExactDecimal() {

            if (isInteger()) {

                return numerator.toString();
            }

            BigInteger denominatorCopy =
                    denominator;

            int twos = 0;
            int fives = 0;

            while (
                    denominatorCopy
                            .mod(
                                    BigInteger.TWO
                            )
                            .equals(
                                    BigInteger.ZERO
                            )
            ) {

                denominatorCopy =
                        denominatorCopy.divide(
                                BigInteger.TWO
                        );

                twos++;
            }

            BigInteger five =
                    BigInteger.valueOf(5);

            while (
                    denominatorCopy
                            .mod(five)
                            .equals(
                                    BigInteger.ZERO
                            )
            ) {

                denominatorCopy =
                        denominatorCopy.divide(
                                five
                        );

                fives++;
            }

            /*
             * Se ainda houver outro fator,
             * o decimal é infinito periódico.
             */
            if (
                    !denominatorCopy.equals(
                            BigInteger.ONE
                    )
            ) {

                return null;
            }

            int scale =
                    Math.max(
                            twos,
                            fives
                    );

            BigInteger scaledNumerator =
                    numerator;

            if (twos < scale) {

                scaledNumerator =
                        scaledNumerator.multiply(
                                BigInteger.TWO.pow(
                                        scale - twos
                                )
                        );
            }

            if (fives < scale) {

                scaledNumerator =
                        scaledNumerator.multiply(
                                five.pow(
                                        scale - fives
                                )
                        );
            }

            boolean negative =
                    scaledNumerator.signum() < 0;

            String digits =
                    scaledNumerator
                            .abs()
                            .toString();

            while (
                    digits.length()
                            <= scale
            ) {

                digits =
                        "0"
                                + digits;
            }

            int decimalPosition =
                    digits.length()
                            - scale;

            String result =
                    digits.substring(
                            0,
                            decimalPosition
                    )
                            + "."
                            + digits.substring(
                            decimalPosition
                    );

            /*
             * Remove zeros desnecessários.
             */
            result =
                    result.replaceAll(
                            "0+$",
                            ""
                    );

            result =
                    result.replaceAll(
                            "\\.$",
                            ""
                    );

            if (negative) {

                result =
                        "-"
                                + result;
            }

            return result;
        }

        // --------------------------------------------------------
        // TEXTO
        // --------------------------------------------------------

        @Override
        public String toString() {

            if (isInteger()) {

                return numerator.toString();
            }

            return numerator
                    + "/"
                    + denominator;
        }

        // --------------------------------------------------------
        // EQUALS / HASH
        // --------------------------------------------------------

        @Override
        public boolean equals(
                Object object
        ) {

            if (
                    this == object
            ) {

                return true;
            }

            if (
                    !(object instanceof Fraction)
            ) {

                return false;
            }

            Fraction other =
                    (Fraction) object;

            return numerator.equals(
                        other.numerator
                    )
                    &&
                    denominator.equals(
                            other.denominator
                    );
        }

        @Override
        public int hashCode() {

            int result =
                    numerator.hashCode();

            result =
                    31 * result
                            + denominator.hashCode();

            return result;
        }

        // --------------------------------------------------------
        // AUXILIAR
        // --------------------------------------------------------

        private void checkOther(
                Fraction other
        ) {

            if (other == null) {

                throw new IllegalArgumentException(
                        "Fração nula."
                );
            }
        }
    }
}

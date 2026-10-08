package com.jarvis.lite;

import java.math.BigInteger;

/**
 * Motor matemático exato do JARVIS Lite.
 * Não usa double/float para o caminho de cálculo exato.
 */
public final class JarvisMathEngine {

    private static final int MAX_EXPRESSION_LENGTH = 500;
    private static final BigInteger TEN = BigInteger.TEN;
    private static final BigInteger TWO = BigInteger.valueOf(2);

    private JarvisMathEngine() {
        // Classe utilitária.
    }

    public static Fraction calculate(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("Expressão nula.");
        }

        String input = normalizeExpression(expression);

        if (input.isEmpty()) {
            throw new IllegalArgumentException("Expressão vazia.");
        }

        if (input.length() > MAX_EXPRESSION_LENGTH) {
            throw new IllegalArgumentException("Expressão muito longa.");
        }

        Parser parser = new Parser(input);
        Fraction result = parser.parseExpression();
        parser.skipWhitespace();

        if (!parser.isAtEnd()) {
            throw new IllegalArgumentException(
                    "Token inesperado na posição " + parser.position() + "."
            );
        }

        return result;
    }

    public static String calculateText(String expression) {
        return calculate(expression).toString();
    }

    public static boolean canCalculate(String expression) {
        try {
            calculate(expression);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String normalizeExpression(String expression) {
        StringBuilder out = new StringBuilder(expression.length());

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (Character.isWhitespace(c)) {
                continue;
            }

            switch (c) {
                case '×':
                    out.append('*');
                    break;
                case '÷':
                    out.append('/');
                    break;
                case 'x':
                case 'X':
                    out.append('*');
                    break;
                case ',':
                    out.append('.');
                    break;
                default:
                    out.append(c);
                    break;
            }
        }

        return out.toString();
    }

    private static final class Parser {

        private final String input;
        private int position;

        Parser(String input) {
            this.input = input;
        }

        Fraction parseExpression() {
            Fraction result = parseTerm();

            while (true) {
                skipWhitespace();

                if (match('+')) {
                    result = result.add(parseTerm());
                } else if (match('-')) {
                    result = result.subtract(parseTerm());
                } else {
                    return result;
                }
            }
        }

        Fraction parseTerm() {
            Fraction result = parseUnary();

            while (true) {
                skipWhitespace();

                if (match('*')) {
                    result = result.multiply(parseUnary());
                } else if (match('/')) {
                    result = result.divide(parseUnary());
                } else if (match('%')) {
                    result = result.mod(parseUnary());
                } else {
                    return result;
                }
            }
        }

        Fraction parseUnary() {
            skipWhitespace();

            if (match('+')) {
                return parseUnary();
            }

            if (match('-')) {
                return parseUnary().negate();
            }

            return parsePrimary();
        }

        Fraction parsePrimary() {
            skipWhitespace();

            if (match('(')) {
                Fraction result = parseExpression();
                skipWhitespace();
                expect(')');
                return result;
            }

            return parseNumber();
        }

        Fraction parseNumber() {
            skipWhitespace();

            int start = position;
            boolean hasDigits = false;
            boolean hasDecimalPoint = false;

            while (!isAtEnd()) {
                char c = currentChar();

                if (Character.isDigit(c)) {
                    hasDigits = true;
                    position++;
                    continue;
                }

                if (c == '.' && !hasDecimalPoint) {
                    hasDecimalPoint = true;
                    position++;
                    continue;
                }

                break;
            }

            if (!hasDigits) {
                throw new IllegalArgumentException(
                        "Número esperado na posição " + start + "."
                );
            }

            String token = input.substring(start, position);
            return Fraction.parseDecimal(token);
        }

        boolean match(char expected) {
            if (!isAtEnd() && currentChar() == expected) {
                position++;
                return true;
            }
            return false;
        }

        void expect(char expected) {
            if (!match(expected)) {
                throw new IllegalArgumentException(
                        "Esperado '" + expected + "' na posição " + position + "."
                );
            }
        }

        void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(currentChar())) {
                position++;
            }
        }

        boolean isAtEnd() {
            return position >= input.length();
        }

        int position() {
            return position;
        }

        private char currentChar() {
            return input.charAt(position);
        }
    }

    public static final class Fraction {

        public static final Fraction ZERO = new Fraction(BigInteger.ZERO, BigInteger.ONE);
        public static final Fraction ONE = new Fraction(BigInteger.ONE, BigInteger.ONE);

        private final BigInteger numerator;
        private final BigInteger denominator;

        private Fraction(BigInteger numerator, BigInteger denominator) {
            if (denominator.signum() == 0) {
                throw new ArithmeticException("Divisão por zero.");
            }

            if (denominator.signum() < 0) {
                numerator = numerator.negate();
                denominator = denominator.negate();
            }

            BigInteger gcd = numerator.gcd(denominator);

            if (!gcd.equals(BigInteger.ONE)) {
                numerator = numerator.divide(gcd);
                denominator = denominator.divide(gcd);
            }

            this.numerator = numerator;
            this.denominator = denominator;
        }

        static Fraction of(BigInteger numerator, BigInteger denominator) {
            return new Fraction(numerator, denominator);
        }

        static Fraction parseDecimal(String token) {
            int dot = token.indexOf('.');

            if (dot < 0) {
                return new Fraction(new BigInteger(token), BigInteger.ONE);
            }

            if (token.indexOf('.', dot + 1) >= 0) {
                throw new IllegalArgumentException("Número decimal inválido: " + token);
            }

            String whole = token.substring(0, dot);
            String decimals = token.substring(dot + 1);

            if (whole.isEmpty() && decimals.isEmpty()) {
                throw new IllegalArgumentException("Número decimal inválido: " + token);
            }

            String digits = whole.isEmpty() ? decimals : whole + decimals;
            BigInteger numerator = new BigInteger(digits);
            BigInteger denominator = TEN.pow(decimals.length());

            return new Fraction(numerator, denominator);
        }

        public Fraction add(Fraction other) {
            return new Fraction(
                    numerator.multiply(other.denominator)
                            .add(other.numerator.multiply(denominator)),
                    denominator.multiply(other.denominator)
            );
        }

        public Fraction subtract(Fraction other) {
            return new Fraction(
                    numerator.multiply(other.denominator)
                            .subtract(other.numerator.multiply(denominator)),
                    denominator.multiply(other.denominator)
            );
        }

        public Fraction multiply(Fraction other) {
            return new Fraction(
                    numerator.multiply(other.numerator),
                    denominator.multiply(other.denominator)
            );
        }

        public Fraction divide(Fraction other) {
            if (other.isZero()) {
                throw new ArithmeticException("Divisão por zero.");
            }

            return new Fraction(
                    numerator.multiply(other.denominator),
                    denominator.multiply(other.numerator)
            );
        }

        public Fraction mod(Fraction other) {
            if (other.isZero()) {
                throw new ArithmeticException("Módulo por zero.");
            }

            if (!isInteger() || !other.isInteger()) {
                throw new ArithmeticException(
                        "O operador % exige valores inteiros."
                );
            }

            return new Fraction(
                    numerator.remainder(other.numerator),
                    BigInteger.ONE
            );
        }

        public Fraction negate() {
            return new Fraction(numerator.negate(), denominator);
        }

        public BigInteger getNumerator() {
            return numerator;
        }

        public BigInteger getDenominator() {
            return denominator;
        }

        public boolean isZero() {
            return numerator.signum() == 0;
        }

        public boolean isInteger() {
            return denominator.equals(BigInteger.ONE);
        }

        public BigInteger toBigIntegerExact() {
            if (!isInteger()) {
                throw new ArithmeticException("O valor não é inteiro.");
            }
            return numerator;
        }

        public String toExactDecimal() {
            if (isInteger()) {
                return numerator.toString();
            }

            BigInteger reducedDenominator = denominator;
            int twos = 0;
            int fives = 0;

            while (reducedDenominator.mod(TWO).signum() == 0) {
                reducedDenominator = reducedDenominator.divide(TWO);
                twos++;
            }

            while (reducedDenominator.mod(BigInteger.valueOf(5)).signum() == 0) {
                reducedDenominator = reducedDenominator.divide(BigInteger.valueOf(5));
                fives++;
            }

            if (!reducedDenominator.equals(BigInteger.ONE)) {
                throw new ArithmeticException(
                        "A fração não possui representação decimal finita exata."
                );
            }

            int scale = Math.max(twos, fives);
            BigInteger scaledNumerator = numerator;

            if (twos < scale) {
                scaledNumerator = scaledNumerator.multiply(
                        TWO.pow(scale - twos)
                );
            }

            if (fives < scale) {
                scaledNumerator = scaledNumerator.multiply(
                        BigInteger.valueOf(5).pow(scale - fives)
                );
            }

            boolean negative = scaledNumerator.signum() < 0;
            String digits = scaledNumerator.abs().toString();

            while (digits.length() <= scale) {
                digits = "0" + digits;
            }

            int decimalPosition = digits.length() - scale;
            String result = digits.substring(0, decimalPosition)
                    + "."
                    + digits.substring(decimalPosition);

            if (negative) {
                result = "-" + result;
            }

            return result;
        }

        @Override
        public String toString() {
            if (denominator.equals(BigInteger.ONE)) {
                return numerator.toString();
            }

            return numerator + "/" + denominator;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }

            if (!(object instanceof Fraction)) {
                return false;
            }

            Fraction other = (Fraction) object;
            return numerator.equals(other.numerator)
                    && denominator.equals(other.denominator);
        }

        @Override
        public int hashCode() {
            int result = numerator.hashCode();
            result = 31 * result + denominator.hashCode();
            return result;
        }
    }
}


package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Laboratório leve de testes de regressão do JARVIS Lite.
 *
 * Os testes só são executados quando run() é chamado explicitamente.
 * Esta classe não altera o aparelho, não acessa a rede e não modifica
 * a memória persistente do aplicativo.
 */
public final class JarvisRegressionLab {

    public interface CheckFunction {
        boolean run() throws Exception;
    }

    public static final class TestCase {
        public final String id;
        public final String description;
        private final CheckFunction check;

        public TestCase(
                String id,
                String description,
                CheckFunction check) {

            this.id = clean(id, "unnamed-test");
            this.description = clean(description, "Sem descrição");
            this.check = check;
        }
    }

    public static final class TestResult {
        public final String id;
        public final String description;
        public final boolean passed;
        public final String status;

        private TestResult(
                String id,
                String description,
                boolean passed,
                String status) {

            this.id = id;
            this.description = description;
            this.passed = passed;
            this.status = status;
        }
    }

    public static final class Report {
        public final List<TestResult> results;
        public final int passed;
        public final int failed;

        private Report(
                List<TestResult> results,
                int passed,
                int failed) {

            this.results = Collections.unmodifiableList(
                    new ArrayList<TestResult>(results)
            );

            this.passed = passed;
            this.failed = failed;
        }

        public int getTotal() {
            return passed + failed;
        }

        public boolean allPassed() {
            return failed == 0;
        }
    }

    private JarvisRegressionLab() {
        // Classe utilitária: não precisa de instâncias.
    }

    /**
     * Executa apenas os testes fornecidos pelo chamador.
     * Cada teste é isolado para que uma falha não interrompa os demais.
     */
    public static Report run(List<TestCase> cases) {
        List<TestResult> results = new ArrayList<TestResult>();

        int passed = 0;
        int failed = 0;

        if (cases == null) {
            return new Report(results, 0, 0);
        }

        for (TestCase test : cases) {

            if (test == null || test.check == null) {
                results.add(new TestResult(
                        test == null ? "null-test" : test.id,
                        test == null
                                ? "Entrada de teste inválida"
                                : test.description,
                        false,
                        "INVALID_TEST"
                ));

                failed++;
                continue;
            }

            try {
                boolean success = test.check.run();

                results.add(new TestResult(
                        test.id,
                        test.description,
                        success,
                        success ? "PASS" : "ASSERTION_FAILED"
                ));

                if (success) {
                    passed++;
                } else {
                    failed++;
                }

            } catch (Exception ex) {
                // Não expõe mensagens que possam conter dados privados.
                results.add(new TestResult(
                        test.id,
                        test.description,
                        false,
                        "EXCEPTION_" + ex.getClass().getSimpleName()
                ));

                failed++;

            } catch (LinkageError err) {
                // Detecta falhas de ligação entre classes durante o teste.
                results.add(new TestResult(
                        test.id,
                        test.description,
                        false,
                        "LINKAGE_ERROR"
                ));

                failed++;
            }
        }

        return new Report(results, passed, failed);
    }

    /**
     * Verifica se um texto existe e não contém apenas espaços.
     */
    public static boolean hasNonBlankText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Verifica se um número é finito e está dentro dos limites.
     */
    public static boolean isWithin(
            double actual,
            double minimum,
            double maximum) {

        return !Double.isNaN(actual)
                && !Double.isInfinite(actual)
                && !Double.isNaN(minimum)
                && !Double.isInfinite(minimum)
                && !Double.isNaN(maximum)
                && !Double.isInfinite(maximum)
                && minimum <= maximum
                && actual >= minimum
                && actual <= maximum;
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        String cleaned = value.trim()
                .replaceAll("[\\r\\n\\t]", " ");

        return cleaned.length() > 120
                ? cleaned.substring(0, 120)
                : cleaned;
    }
}

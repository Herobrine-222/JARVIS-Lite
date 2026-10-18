
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classifica o tipo de suporte disponível para uma afirmação.
 *
 * Não pesquisa na internet e não garante que uma fonte esteja correta.
 * O chamador deve fornecer evidências que realmente possui.
 *
 * Este módulo não altera a memória, as permissões ou as configurações.
 */
public final class JarvisEvidenceEngine {

    public enum Status {
        TOOL_RESULT_OBSERVED,
        SUPPORTED,
        INFERENCE,
        UNKNOWN
    }

    public static final class Evidence {
        public final String source;
        public final String detail;
        public final boolean directlyObserved;
        public final boolean independentlyChecked;

        public Evidence(
                String source,
                String detail,
                boolean directlyObserved,
                boolean independentlyChecked) {

            this.source = clean(source, "Fonte não informada");
            this.detail = clean(detail, "Detalhes não informados");
            this.directlyObserved = directlyObserved;
            this.independentlyChecked = independentlyChecked;
        }
    }

    public static final class Assessment {
        public final Status status;
        public final String explanation;
        public final List<Evidence> evidence;

        private Assessment(
                Status status,
                String explanation,
                List<Evidence> evidence) {

            this.status = status;
            this.explanation = explanation;
            this.evidence = Collections.unmodifiableList(
                    new ArrayList<Evidence>(evidence)
            );
        }

        /**
         * Um resultado de ferramenta não deve ser tratado como
         * garantia absoluta de que uma afirmação é verdadeira.
         */
        public boolean isToolResultObserved() {
            return status == Status.TOOL_RESULT_OBSERVED;
        }
    }

    private JarvisEvidenceEngine() {
        // Classe utilitária.
    }

    /**
     * Classifica uma afirmação usando apenas as evidências recebidas.
     *
     * toolProducedResult indica que uma ferramenta realmente produziu
     * um resultado. O chamador é responsável por informar isso corretamente.
     */
    public static Assessment assess(
            String claim,
            List<Evidence> suppliedEvidence,
            boolean toolProducedResult) {

        List<Evidence> safe = new ArrayList<Evidence>();

        if (suppliedEvidence != null) {
            for (Evidence item : suppliedEvidence) {
                if (item != null) {
                    safe.add(item);
                }
            }
        }

        if (claim == null || claim.trim().isEmpty()) {
            return new Assessment(
                    Status.UNKNOWN,
                    "Não foi fornecida uma afirmação para avaliar.",
                    safe
            );
        }

        boolean observed = false;
        boolean independentlyChecked = false;

        for (Evidence item : safe) {
            observed |= item.directlyObserved;
            independentlyChecked |= item.independentlyChecked;
        }

        if (toolProducedResult && observed) {
            return new Assessment(
                    Status.TOOL_RESULT_OBSERVED,
                    "Uma ferramenta produziu um resultado observado. "
                            + "Isso não garante que o resultado esteja correto "
                            + "nem confirma interpretações além do que foi medido.",
                    safe
            );
        }

        if (observed && independentlyChecked) {
            return new Assessment(
                    Status.SUPPORTED,
                    "Há evidência observada e uma verificação independente "
                            + "informada. Confira as fontes e o alcance da conclusão.",
                    safe
            );
        }

        if (observed || !safe.isEmpty()) {
            return new Assessment(
                    Status.INFERENCE,
                    "Existem informações de apoio, mas elas não bastam "
                            + "para garantir a afirmação como verdadeira.",
                    safe
            );
        }

        return new Assessment(
                Status.UNKNOWN,
                "Nenhuma evidência foi fornecida. "
                        + "O JARVIS deve reconhecer a incerteza ou buscar "
                        + "uma fonte adequada, se isso estiver disponível.",
                safe
        );
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        String cleaned = value.trim()
                .replaceAll("[\\r\\n\\t]", " ");

        return cleaned.length() > 240
                ? cleaned.substring(0, 240)
                : cleaned;
    }
}


package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gerencia o histórico temporário de uma conversa.
 *
 * Não salva dados em disco, não acessa a internet e não executa
 * comandos. A integração com memória persistente será feita
 * separadamente, após a auditoria das classes existentes.
 */
public final class JarvisDialogManager {

    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM
    }

    public static final class DialogMessage {
        private final Role role;
        private final String content;
        private final long timestamp;

        private DialogMessage(
                Role role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        public Role getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    private static final int DEFAULT_MAX_MESSAGES = 100;
    private static final int MAX_ALLOWED_MESSAGES = 1000;
    private static final int MAX_MESSAGE_LENGTH = 10000;

    private final List<DialogMessage> messages = new ArrayList<>();
    private final int maxMessages;

    public JarvisDialogManager() {
        this(DEFAULT_MAX_MESSAGES);
    }

    public JarvisDialogManager(int maxMessages) {
        this.maxMessages = Math.max(
                1, Math.min(maxMessages, MAX_ALLOWED_MESSAGES));
    }

    public synchronized boolean addUserMessage(String content) {
        return addMessage(Role.USER, content);
    }

    public synchronized boolean addAssistantMessage(String content) {
        return addMessage(Role.ASSISTANT, content);
    }

    public synchronized boolean addSystemMessage(String content) {
        return addMessage(Role.SYSTEM, content);
    }

    public synchronized boolean addMessage(Role role, String content) {
        if (role == null || content == null) {
            return false;
        }

        String normalized = content.trim();

        if (normalized.isEmpty()
                || normalized.length() > MAX_MESSAGE_LENGTH) {
            return false;
        }

        messages.add(new DialogMessage(
                role, normalized, System.currentTimeMillis()));

        trimHistory();
        return true;
    }

    /**
     * Retorna uma cópia somente para leitura do histórico.
     */
    public synchronized List<DialogMessage> getHistory() {
        return Collections.unmodifiableList(
                new ArrayList<>(messages));
    }

    /**
     * Retorna as últimas mensagens disponíveis.
     */
    public synchronized List<DialogMessage> getRecentMessages(int count) {
        int safeCount = Math.max(0, count);
        int start = Math.max(0, messages.size() - safeCount);

        return Collections.unmodifiableList(
                new ArrayList<>(messages.subList(start, messages.size())));
    }

    /**
     * Constrói um contexto textual simples para outro módulo.
     * O resultado deve ser tratado como contexto, não como instruções
     * confiáveis nem como fonte de fatos verificados.
     */
    public synchronized String buildRecentContext(int count) {
        List<DialogMessage> recent = getRecentMessages(count);
        StringBuilder context = new StringBuilder();

        for (DialogMessage message : recent) {
            if (context.length() > 0) {
                context.append('\n');
            }

            context.append(roleLabel(message.getRole()))
                    .append(": ")
                    .append(message.getContent());
        }

        return context.toString();
    }

    public synchronized int getMessageCount() {
        return messages.size();
    }

    /**
     * Limpa somente o histórico temporário desta instância.
     * Não apaga a memória persistente do JARVIS.
     */
    public synchronized void clearHistory() {
        messages.clear();
    }

    public synchronized DialogMessage getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }

        return messages.get(messages.size() - 1);
    }

    private void trimHistory() {
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }

    private String roleLabel(Role role) {
        switch (role) {
            case USER:
                return "Usuário";

            case ASSISTANT:
                return "JARVIS";

            case SYSTEM:
            default:
                return "Sistema";
        }
    }
}

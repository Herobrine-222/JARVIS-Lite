package com.jarvis.lite;

import android.content.Context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class JarvisContextManager {

    private static final int MAX_MESSAGES = 20;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int MAX_MEMORY_ITEMS = 5;

    private final JarvisMemory memory;
    private final JarvisDeviceTools deviceTools;

    private final Deque<ConversationMessage> conversation =
            new ArrayDeque<>();

    public JarvisContextManager(Context context) {

        Context appContext =
                context.getApplicationContext();

        memory =
                new JarvisMemory(appContext);

        deviceTools =
                new JarvisDeviceTools(appContext);
    }

    // ============================================================
    // CONVERSA
    // ============================================================

    public synchronized void addUserMessage(
            String message
    ) {

        addMessage(
                "usuario",
                message
        );
    }

    public synchronized void addJarvisMessage(
            String message
    ) {

        addMessage(
                "jarvis",
                message
        );
    }

    private void addMessage(
            String role,
            String message
    ) {

        if (message == null) {
            return;
        }

        String value =
                message.trim();

        if (value.isEmpty()) {
            return;
        }

        if (value.length() > MAX_MESSAGE_LENGTH) {

            value =
                    value.substring(
                            0,
                            MAX_MESSAGE_LENGTH
                    );
        }

        conversation.addLast(
                new ConversationMessage(
                        role,
                        value,
                        System.currentTimeMillis()
                )
        );

        while (
                conversation.size()
                        > MAX_MESSAGES
        ) {

            conversation.removeFirst();
        }
    }

    // ============================================================
    // ÚLTIMA MENSAGEM DO USUÁRIO
    // ============================================================

    public synchronized String getLastUserMessage() {

        ConversationMessage[] messages =
                conversation.toArray(
                        new ConversationMessage[0]
                );

        for (
                int i = messages.length - 1;
                i >= 0;
                i--
        ) {

            if (
                    "usuario".equals(
                            messages[i].role
                    )
            ) {

                return messages[i].text;
            }
        }

        return "";
    }

    // ============================================================
    // ÚLTIMA RESPOSTA DO JARVIS
    // ============================================================

    public synchronized String getLastJarvisMessage() {

        ConversationMessage[] messages =
                conversation.toArray(
                        new ConversationMessage[0]
                );

        for (
                int i = messages.length - 1;
                i >= 0;
                i--
        ) {

            if (
                    "jarvis".equals(
                            messages[i].role
                    )
            ) {

                return messages[i].text;
            }
        }

        return "";
    }

    // ============================================================
    // HISTÓRICO
    // ============================================================

    public synchronized List<String> getConversation() {

        ArrayList<String> result =
                new ArrayList<>();

        for (
                ConversationMessage message
                        : conversation
        ) {

            result.add(
                    message.role
                            + ": "
                            + message.text
            );
        }

        return result;
    }

    // ============================================================
    // CONTEXTO DA CONVERSA
    // ============================================================

    public synchronized String buildConversationContext() {

        if (conversation.isEmpty()) {
            return "";
        }

        StringBuilder context =
                new StringBuilder();

        for (
                ConversationMessage message
                        : conversation
        ) {

            if (context.length() > 0) {
                context.append("\n");
            }

            context
                    .append(
                            message.role
                                    .equals("usuario")
                                    ? "Usuário"
                                    : "JARVIS"
                    )
                    .append(": ")
                    .append(message.text);
        }

        return context.toString();
    }

    // ============================================================
    // MEMÓRIA RELEVANTE
    // ============================================================

    public synchronized String getRelevantMemory(
            String query
    ) {

        if (
                query == null ||
                query.trim().isEmpty()
        ) {
            return "";
        }

        return memory.relevantContext(
                query,
                MAX_MEMORY_ITEMS
        );
    }

    // ============================================================
    // BUSCA DE MEMÓRIAS
    // ============================================================

    public synchronized List<String> searchMemory(
            String query
    ) {

        if (
                query == null ||
                query.trim().isEmpty()
        ) {

            return new ArrayList<>();
        }

        return memory.search(
                query,
                MAX_MEMORY_ITEMS
        );
    }

    // ============================================================
    // MONTAR CONTEXTO COMPLETO
    // ============================================================

    public synchronized String buildContext(
            String currentMessage
    ) {

        StringBuilder context =
                new StringBuilder();

        String mensagem =
                currentMessage == null
                        ? ""
                        : currentMessage.trim();

        // --------------------------------------------------------
        // MEMÓRIA
        // --------------------------------------------------------

        if (!mensagem.isEmpty()) {

            String memories =
                    getRelevantMemory(
                            mensagem
                    );

            if (
                    memories != null &&
                    !memories.trim().isEmpty()
            ) {

                context
                        .append(
                                "MEMÓRIAS RELEVANTES:\n"
                        )
                        .append(memories)
                        .append("\n\n");
            }
        }

        // --------------------------------------------------------
        // CONVERSA
        // --------------------------------------------------------

        String conversationContext =
                buildConversationContext();

        if (
                !conversationContext
                        .trim()
                        .isEmpty()
        ) {

            context
                    .append(
                            "CONVERSA RECENTE:\n"
                    )
                    .append(
                            conversationContext
                    )
                    .append("\n\n");
        }

        // --------------------------------------------------------
        // MENSAGEM ATUAL
        // --------------------------------------------------------

        if (!mensagem.isEmpty()) {

            context
                    .append(
                            "MENSAGEM ATUAL:\n"
                    )
                    .append(mensagem);
        }

        return context.toString();
    }

    // ============================================================
    // CONTEXTO DO APARELHO
    // ============================================================

    public synchronized String buildDeviceContext() {

        try {

            return deviceTools
                    .obterInformacoesAparelho();

        } catch (Exception ignored) {

            return "";
        }
    }

    // ============================================================
    // CONTEXTO COMPLETO + APARELHO
    // ============================================================

    public synchronized String buildFullContext(
            String currentMessage
    ) {

        StringBuilder context =
                new StringBuilder();

        String base =
                buildContext(
                        currentMessage
                );

        if (
                base != null &&
                !base.trim().isEmpty()
        ) {

            context.append(base);
        }

        String device =
                buildDeviceContext();

        if (
                device != null &&
                !device.trim().isEmpty()
        ) {

            if (context.length() > 0) {
                context.append("\n\n");
            }

            context
                    .append(
                            "INFORMAÇÕES DO APARELHO:\n"
                    )
                    .append(device);
        }

        return context.toString();
    }

    // ============================================================
    // LIMPAR CONVERSA ATUAL
    // ============================================================

    public synchronized void clearConversation() {

        conversation.clear();
    }

    // ============================================================
    // QUANTIDADE DE MENSAGENS
    // ============================================================

    public synchronized int getConversationSize() {

        return conversation.size();
    }

    // ============================================================
    // VERIFICAR SE EXISTE CONVERSA
    // ============================================================

    public synchronized boolean hasConversation() {

        return !conversation.isEmpty();
    }

    // ============================================================
    // ACESSO À MEMÓRIA
    // ============================================================

    public JarvisMemory getMemory() {

        return memory;
    }

    // ============================================================
    // ACESSO ÀS FERRAMENTAS DO APARELHO
    // ============================================================

    public JarvisDeviceTools getDeviceTools() {

        return deviceTools;
    }

    // ============================================================
    // ADICIONAR MEMÓRIA
    // ============================================================

    public synchronized boolean remember(
            String text
    ) {

        return memory.remember(
                text
        );
    }

    public synchronized boolean remember(
            String text,
            String category
    ) {

        return memory.remember(
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

        return memory.forget(
                query
        );
    }

    // ============================================================
    // LIMPAR MEMÓRIA
    // ============================================================

    public synchronized void clearMemory() {

        memory.clear();
    }

    // ============================================================
    // REPRESENTAÇÃO DE UMA MENSAGEM
    // ============================================================

    private static class ConversationMessage {

        final String role;
        final String text;
        final long time;

        ConversationMessage(
                String role,
                String text,
                long time
        ) {

            this.role = role;
            this.text = text;
            this.time = time;
        }
    }
}

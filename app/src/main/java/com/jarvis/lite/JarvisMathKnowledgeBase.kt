package com.jarvis.lite

import android.content.Context
import java.io.IOException
import java.text.Normalizer
import java.util.Locale

/**
 * Recuperador local do Caderno de Matemática — do Básico às Áreas Avançadas.
 *
 * O caderno de matemática é empacotado em app/src/main/assets/math/.
 * A consulta ocorre somente no APK: sem internet, sem alteração da
 * memória SQLite e sem execução de comandos do dispositivo.
 */
object JarvisMathKnowledgeBase {
    private const val ASSET_PATH =
        "math/caderno_matematica_basico_avancado.md"

    private const val MAX_RESPONSE_CHARS = 2200
    private const val MAX_PASSAGES = 3

    private val stopWords = setOf(
        "a", "ao", "aos", "as", "o", "os",
        "de", "do", "dos", "da", "das",
        "em", "no", "nos", "na", "nas",
        "um", "uma", "uns", "umas",
        "e", "ou", "por", "para", "com", "sem",
        "como", "qual", "quais", "quando", "onde", "matematica", "matematico", "matematicas",
        "quem", "que", "porque", "explique",
        "explicar", "explicacao", "fale", "falar", "diga",
        "mostrar", "mostre", "me", "voce", "seu", "sua",
        "seus", "suas", "eu", "quero", "gostaria",
        "poderia", "pode", "saber", "sobre", "isso",
        "isto", "aqui", "funciona", "funcionam",
        "funcionamento", "funcionou", "significa",
        "significado", "defina", "definicao", "descreva",
        "descrever", "explana", "ciencia", "caderno",
        "volume", "tema", "assunto", "conceito"
    )

    private val lock = Any()

    @Volatile
    private var cachedPassages: List<Passage>? = null

    @Volatile
    private var loadAttempted = false

    @Volatile
    private var applicationContext: Context? = null

    /**
     * Guarda somente o contexto da aplicação, nunca uma Activity.
     * Mantém compatibilidade com o MainActivity e o JarvisBrain atuais.
     */
    @JvmStatic
    fun initialize(context: Context?) {
        if (context == null) return
        applicationContext = context.applicationContext ?: context
    }

    /**
     * Retorna até três passagens relevantes ou null quando o caderno
     * não contém material suficientemente relacionado à pergunta.
     */
    @JvmStatic
    fun answer(context: Context?, question: String?): String? {
        val resolvedContext = context?.applicationContext ?: applicationContext

        if (resolvedContext == null || question.isNullOrBlank()) {
            return null
        }

        val terms = keywords(question)
        if (terms.isEmpty()) return null

        val passages = getPassages(resolvedContext)
        if (passages.isNullOrEmpty()) return null

        val documentFrequencies = terms.associateWith { term ->
            countMatchingPassages(passages, term)
        }

        val matches = ArrayList<ScoredPassage>()

        for (passage in passages) {
            var score = 0
            var matchedTerms = 0

            for (term in terms) {
                val inHeading = containsWord(passage.headingTokens, term)
                val inBody = containsWord(passage.bodyTokens, term)

                if (inHeading || inBody) matchedTerms++

                // Termos raros como Born e Schrödinger recebem mais peso.
                val frequency = documentFrequencies[term] ?: 0
                val weight = when {
                    frequency <= 3 -> 5
                    frequency <= 10 -> 3
                    else -> 1
                }

                if (inHeading) score += weight * 2
                if (inBody) score += weight

                if (terms.size == 1 && inBody) score += 2
            }

            if (terms.size > 1) {
                score += if (matchedTerms == terms.size) {
                    matchedTerms * 4
                } else {
                    -(terms.size - matchedTerms) * 3
                }
            }

            if (matchedTerms > 0 && score >= 4) {
                matches.add(ScoredPassage(passage, score))
            }
        }

        if (matches.isEmpty()) return null
        matches.sortByDescending { it.score }

        val bestScore = matches.first().score
        val response = StringBuilder(
            "Encontrei estas passagens no caderno offline de matemática " +
                "(Volume I):\n\n"
        )

        var included = 0

        for (match in matches) {
            if (included >= MAX_PASSAGES || match.score < bestScore - 2) {
                break
            }

            val heading = match.passage.heading.trim()
            val body = match.passage.body.trim()
            if (body.isEmpty()) continue

            val block = "$heading\n$body\n\n"
            val available = MAX_RESPONSE_CHARS - response.length

            if (block.length > available) {
                if (available > heading.length + 8) {
                    val bodyRoom = available - heading.length - 5
                    var shortened = body.take(bodyRoom.coerceAtLeast(0))
                    val lastSpace = shortened.lastIndexOf(' ')
                    if (lastSpace > shortened.length / 2) {
                        shortened = shortened.substring(0, lastSpace)
                    }

                    response.append(heading)
                        .append('\n')
                        .append(shortened)
                        .append('…')
                }
                break
            }

            response.append(block)
            included++
        }

        return if (included == 0) null else response.toString().trim()
    }

    private fun getPassages(context: Context): List<Passage>? {
        cachedPassages?.let { return it }
        if (loadAttempted) return cachedPassages

        synchronized(lock) {
            cachedPassages?.let { return it }
            if (loadAttempted) return cachedPassages

            loadAttempted = true

            return try {
                val document = context.assets.open(ASSET_PATH).bufferedReader(
                    Charsets.UTF_8
                ).use { it.readText() }

                parsePassages(document).toList().also {
                    cachedPassages = it
                }
            } catch (_: IOException) {
                cachedPassages = null
                null
            } catch (_: RuntimeException) {
                // Uma falha da base não deve derrubar a conversa.
                cachedPassages = null
                null
            }
        }
    }

    private fun parsePassages(document: String): List<Passage> {
        val output = ArrayList<Passage>()
        val headings = arrayOfNulls<String>(6)
        val paragraph = StringBuilder()

        for (line in document.split(Regex("\\r?\\n"))) {
            val trimmed = line.trim()

            if (trimmed.startsWith("#")) {
                flushParagraph(output, paragraph, headings)

                var level = 0
                while (level < trimmed.length && trimmed[level] == '#') {
                    level++
                }

                if (level in 1..headings.size &&
                    (trimmed.length == level ||
                        trimmed[level].isWhitespace())
                ) {
                    for (index in level - 1 until headings.size) {
                        headings[index] = null
                    }
                    headings[level - 1] = trimmed.substring(level).trim()
                }
                continue
            }

            if (trimmed.isEmpty()) {
                flushParagraph(output, paragraph, headings)
                continue
            }

            if (paragraph.isNotEmpty()) paragraph.append('\n')
            paragraph.append(trimmed)
        }

        flushParagraph(output, paragraph, headings)
        return output
    }

    private fun flushParagraph(
        output: MutableList<Passage>,
        paragraph: StringBuilder,
        headings: Array<String?>
    ) {
        val body = paragraph.toString().trim()
        paragraph.setLength(0)
        if (body.isEmpty()) return

        val headingText = headings
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" > ")
            .ifEmpty { "Caderno de matemática" }

        output.add(
            Passage(
                heading = headingText,
                body = body,
                headingTokens = tokensOf(headingText),
                bodyTokens = tokensOf(body)
            )
        )
    }

    private fun keywords(text: String): List<String> {
        val normalized = normalize(text)
        if (normalized.isEmpty()) return emptyList()

        val unique = LinkedHashSet<String>()
        for (token in normalized.split(Regex("\\s+"))) {
            if (token.length >= 3 && token !in stopWords) {
                unique.add(token)
            }
        }
        return unique.toList()
    }

    private fun containsWord(words: Set<String>, queryWord: String): Boolean {
        if (queryWord in words) return true

        if (queryWord.length > 4 && queryWord.endsWith("s") &&
            queryWord.dropLast(1) in words
        ) return true

        // Flexões frequentes do português: equação/equações,
        // matriz/matrizes e porcentagem/porcentagens.
        if (queryWord.endsWith("ção") &&
            (queryWord.dropLast(3) + "ções") in words
        ) return true

        if (queryWord.endsWith("ções") &&
            (queryWord.dropLast(4) + "ção") in words
        ) return true

        if (queryWord.endsWith("iz") &&
            (queryWord.dropLast(2) + "izes") in words
        ) return true

        if (queryWord.endsWith("izes") &&
            (queryWord.dropLast(4) + "iz") in words
        ) return true

        if (queryWord.endsWith("gem") &&
            (queryWord.dropLast(1) + "ns") in words
        ) return true

        if (queryWord.endsWith("gens") &&
            (queryWord.dropLast(4) + "gem") in words
        ) return true

        return queryWord.length > 4 && (queryWord + "s") in words
    }

    private fun countMatchingPassages(
        passages: List<Passage>,
        term: String
    ): Int = passages.count {
        containsWord(it.headingTokens, term) ||
            containsWord(it.bodyTokens, term)
    }

    private fun tokensOf(text: String): Set<String> {
        val normalized = normalize(text)
        if (normalized.isEmpty()) return emptySet()
        return normalized.split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
    }

    private fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(
            text.lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        )

        return decomposed
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private data class Passage(
        val heading: String,
        val body: String,
        val headingTokens: Set<String>,
        val bodyTokens: Set<String>
    )

    private data class ScoredPassage(
        val passage: Passage,
        val score: Int
    )
}

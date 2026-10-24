package com.jarvis.lite

import android.content.Context
import java.text.Normalizer
import java.util.Locale

/**
 * Base de conhecimentos offline do JARVIS Lite.
 *
 * Mantém as respostas curtas já existentes e encaminha perguntas
 * científicas para os cadernos locais de matemática e ciência quântica.
 * Não acessa a internet.
 */
object JarvisKnowledgeBase {

    /**
     * API estática para manter compatibilidade com o JarvisBrain Java.
     */
    @JvmStatic
    fun answer(context: Context?, input: String?): String? {
        val existing = answer(input)
        if (existing != null) return existing
        val portuguese = JarvisPortugueseKnowledgeBase.answer(context, input)
        if (portuguese != null) return portuguese
        val mathematics = JarvisMathKnowledgeBase.answer(context, input)
        if (mathematics != null) return mathematics
        val physics = JarvisPhysicsKnowledgeBase.answer(context, input)
        if (physics != null) return physics
        return JarvisQuantumKnowledgeBase.answer(context, input)
    }

    /**
     * Respostas curtas que não dependem do caderno científico.
     * Retorna null quando nenhuma frase cadastrada corresponde.
     */
    @JvmStatic
    fun answer(input: String?): String? {
        val q = normalize(input.orEmpty())
        if (q.isEmpty()) return null

        // História do Brasil
        if (matches(
                q,
                "quem descobriu o brasil",
                "quem descobriu brasil",
                "quem foi o descobridor do brasil"
            )
        ) {
            return "A expedição portuguesa liderada por Pedro Álvares " +
                "Cabral chegou ao território que hoje chamamos Brasil " +
                "em 22 de abril de 1500. Porém, a região já era " +
                "habitada por diversos povos indígenas muito antes " +
                "disso. Por isso, é mais preciso falar da chegada " +
                "dos portugueses, e não da descoberta de uma terra " +
                "desabitada."
        }

        if (matches(
                q,
                "quando o brasil ficou independente",
                "quando foi a independencia do brasil"
            )
        ) {
            return "A Independência do Brasil é celebrada em " +
                "7 de setembro de 1822."
        }

        if (matches(q, "qual e a capital do brasil", "capital do brasil")) {
            return "A capital do Brasil é Brasília, no Distrito Federal."
        }

        // Ciências
        if (matches(
                q,
                "qual planeta nos vivemos",
                "em qual planeta nos vivemos",
                "qual e o nosso planeta"
            )
        ) {
            return "Nós vivemos no planeta Terra, o terceiro planeta " +
                "a partir do Sol."
        }

        if (matches(
                q,
                "qual e a estrela do sistema solar",
                "qual estrela fica no centro do sistema solar"
            )
        ) {
            return "O Sol é a estrela no centro do Sistema Solar."
        }

        if (matches(
                q,
                "qual e o planeta vermelho",
                "qual planeta e conhecido como planeta vermelho",
                "planeta vermelho"
            )
        ) {
            return "Marte é conhecido como o planeta vermelho por causa " +
                "da aparência avermelhada de sua superfície."
        }

        if (matches(
                q,
                "o que e fotossintese",
                "explique a fotossintese",
                "como funciona a fotossintese"
            )
        ) {
            return "Fotossíntese é o processo pelo qual plantas, algas " +
                "e alguns microrganismos usam energia da luz para " +
                "produzir matéria orgânica a partir de água e " +
                "dióxido de carbono. Nesse processo, plantas " +
                "liberam oxigênio."
        }

        if (matches(
                q,
                "qual e a formula quimica da agua",
                "formula da agua"
            )
        ) {
            return "A fórmula química da água é H₂O: cada molécula " +
                "contém dois átomos de hidrogênio e um de oxigênio."
        }

        // Geometria
        if (matches(
                q,
                "formula da area do triangulo",
                "qual e a formula da area do triangulo",
                "como calcular a area de um triangulo"
            )
        ) {
            return "A área de um triângulo é A = (base × altura) ÷ 2. " +
                "Multiplique a base pela altura e divida por 2."
        }

        if (matches(
                q,
                "formula da area do retangulo",
                "qual e a formula da area do retangulo",
                "como calcular a area de um retangulo"
            )
        ) {
            return "A área de um retângulo é A = comprimento × largura."
        }

        if (matches(
                q,
                "formula da area do quadrado",
                "qual e a formula da area do quadrado",
                "como calcular a area de um quadrado"
            )
        ) {
            return "A área de um quadrado é A = lado × lado, " +
                "ou A = lado²."
        }

        if (matches(
                q,
                "formula do perimetro do quadrado",
                "qual e o perimetro de um quadrado"
            )
        ) {
            return "O perímetro de um quadrado é P = 4 × lado, " +
                "pois seus quatro lados têm o mesmo comprimento."
        }

        if (matches(
                q,
                "formula da area do circulo",
                "qual e a formula da area do circulo"
            )
        ) {
            return "A área de um círculo é A = π × raio². " +
                "Use o raio ao quadrado e multiplique por π " +
                "(aproximadamente 3,14159)."
        }

        if (matches(
                q,
                "teorema de pitagoras",
                "explique o teorema de pitagoras",
                "qual e a formula de pitagoras"
            )
        ) {
            return "O teorema de Pitágoras vale para triângulos " +
                "retângulos: a² + b² = c², em que a e b são " +
                "os catetos e c é a hipotenusa."
        }

        // Língua portuguesa
        if (matches(q, "o que e um substantivo", "explique o substantivo")) {
            return "Substantivo é a classe de palavras que nomeia " +
                "pessoas, animais, lugares, objetos, sentimentos, " +
                "ideias e outros seres ou conceitos."
        }

        if (matches(q, "o que e um verbo", "explique o verbo")) {
            return "Verbo é a classe de palavras que pode expressar " +
                "ação, estado, mudança de estado ou fenômeno " +
                "da natureza."
        }

        return null
    }

    private fun matches(q: String, vararg phrases: String): Boolean {
        return phrases.any { phrase ->
            val normalizedPhrase = normalize(phrase)
            q == normalizedPhrase || q.contains(normalizedPhrase)
        }
    }

    private fun normalize(text: String): String {
        val value = Normalizer.normalize(
            text.lowercase(Locale.ROOT).trim(),
            Normalizer.Form.NFD
        )

        return value
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

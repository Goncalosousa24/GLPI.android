package com.example.glpimobile

import android.text.SpannedString
import android.text.Spanned

object GlpiHtmlFixer {

    fun clean(html: String?): Spanned {
        if (html.isNullOrEmpty()) return SpannedString("Sem descrição.")

        try {
            var text = html!!

            // PASSO 1 — Descodificar entidades numéricas mais frequentes (até 3 vezes para multi-layer)
            // O GLPI envia &#60; (=<) e &#62; (=>) e &#38; (=&)
            repeat(3) {
                text = text
                    .replace("&#38;", "&")   // & primeiro !
                    .replace("&#60;", "<")
                    .replace("&#62;", ">")
                    .replace("&#34;", "\"")
                    .replace("&#39;", "'")
                    .replace("&#160;", " ")
                    .replace("&#59;", ";")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .replace("&nbsp;", " ")
            }

            // PASSO 2 — Remover blocos <style>...</style> e <script>...</script>
            var cleaned = removeBlock(text, "<style", "</style>")
            cleaned = removeBlock(cleaned, "<script", "</script>")

            // PASSO 3 — Remover todas as tags HTML: <qualquercoisa>
            cleaned = cleaned.replace(Regex("<[^>]{0,5000}>"), "")

            // PASSO 4 — Truncar assinaturas/avisos legais
            // Recalculamos o lower a cada iteração para garantir que o index está correto
            var result = cleaned.trim()

            // Remove email header blocks that appear at the start of the content
            // Pattern: "De: ...\nEnviado: ...\nPara: ...\nAssunto: ..."
            result = result.replace(
                Regex("(?i)(^|\\n)(de|from)\\s*:.*?(\\n|$)(enviado|sent)\\s*:.*?(\\n|$)(para|to)\\s*:.*?(\\n|$)(assunto|subject)\\s*:.*?(\\n|$)", RegexOption.DOT_MATCHES_ALL),
                "\n"
            ).trim()

            // Institutional signature block: only cut when these appear at the start of a line
            // (i.e. as a footer/signature, NOT mid-sentence content)
            val lineAnchoredPatterns = listOf(
                Regex("(?im)^\\s*(município de vila verde|municipio de vila verde)\\s*$"),
                Regex("(?im)^\\s*(câmara municipal|camara municipal)\\s*(de vila verde)?\\s*$"),
                Regex("(?im)^\\s*telf\\s*:"),
                Regex("(?im)^\\s*www\\.cm-"),
                Regex("(?im)^\\s*site\\s*:\\s*www\\."),
            )
            for (pattern in lineAnchoredPatterns) {
                val match = pattern.find(result)
                if (match != null && match.range.first > 0) {
                    result = result.substring(0, match.range.first)
                }
            }

            // Simple substring truncation for clearly unique footer strings
            for (kw in listOf(
                "aviso de confidencialidade", "esta mensagem e quaisquer anexos",
                "confidencialidade:", "*****", "-----",
                // Email forward/reply headers
                "de: ", "enviado: ", "para: ", "assunto: ",
                "from: ", "sent: ", "to: ", "subject: ",
                // Automated email footers (Sophos, etc.)
                "this email alert was generated",
                "do not reply to this email",
                "do not reply",
                "this is an automated",
                "you are receiving this email",
                "esta é uma mensagem automática",
                "não responda a este email"
            )) {
                val lower = result.lowercase()
                val idx = lower.indexOf(kw)
                if (idx > 0) result = result.substring(0, idx)
            }

            // PASSO 5 — Normalizar espaços/newlines
            result = result
                .replace(Regex("[ \t]+"), " ")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()

            return SpannedString(result)
        } catch (e: Exception) {
            // Fallback simples
            val fallback = html
                ?.replace("&#60;", "<")?.replace("&#62;", ">")?.replace("&#38;", "&")
                ?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
            return SpannedString(fallback)
        }
    }

    private fun removeBlock(input: String, startTag: String, endTag: String): String {
        var text = input
        while (true) {
            val s = text.lowercase().indexOf(startTag.lowercase())
            if (s == -1) break
            val e = text.lowercase().indexOf(endTag.lowercase(), s)
            text = if (e == -1) text.substring(0, s)
                   else text.substring(0, s) + text.substring(e + endTag.length)
        }
        return text
    }

    fun cleanToText(html: String?): String = clean(html).toString()
}

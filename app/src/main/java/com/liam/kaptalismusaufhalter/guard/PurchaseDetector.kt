package com.liam.kaptalismusaufhalter.guard

import android.view.accessibility.AccessibilityNodeInfo

data class PurchaseSignal(val price: Double?, val title: String?)

private val PRICE_REGEX = Regex("""(\d{1,4}(?:[.,]\d{3})*[.,]\d{2})\s?€|€\s?(\d{1,4}(?:[.,]\d{3})*[.,]\d{2})""")

// Generic checkout/nav chrome that shouldn't be mistaken for the product name - matched
// as a whole-string comparison (lowercased), not a substring, so it doesn't reject real
// titles that merely mention e.g. "Konto".
private val TITLE_BLOCKLIST = setOf(
    "warenkorb", "mein warenkorb", "zur kasse", "zur kasse gehen", "kasse", "weiter",
    "weiter zur kasse", "zurück", "login", "anmelden", "registrieren", "suche", "menü",
    "konto", "mein konto", "bestellübersicht", "bestellung aufgeben", "lieferadresse",
    "rechnungsadresse", "zahlungsart", "zahlungsmethode", "gesamtsumme", "gesamt",
    "zwischensumme", "versandkosten", "versand", "inkl. mwst.", "inkl. mwst", "gutschein",
    "rabattcode", "gutscheincode", "agb", "datenschutz", "hilfe", "startseite", "home",
    "impressum", "kontakt", "newsletter", "filialen", "prime", "kostenlose lieferung",
    "jetzt kostenpflichtig bestellen", "kostenpflichtig bestellen", "zahlungspflichtig bestellen"
)

private const val TITLE_MIN_LEN = 8
private const val TITLE_MAX_LEN = 90

// Deliberately narrow to final-confirmation phrasing (German consumer-protection law
// requires shops' actual checkout button to say something like "zahlungspflichtig
// bestellen"), not generic "Jetzt kaufen"/"In den Warenkorb" buttons that already sit on
// every product page - those would fire on any browsing, not just the checkout step.
private val BUY_KEYWORDS = listOf(
    "zahlungspflichtig bestellen", "kostenpflichtig bestellen", "kostenpflichtig kaufen",
    "kostenpflichtig bezahlen", "verbindlich bestellen", "zur kasse gehen", "zur kasse",
    "jetzt bezahlen", "checkout", "place order", "pay now", "complete purchase", "confirm order"
)

// Cap how much of the tree we walk, so a huge/degenerate node tree can't cause an ANR.
private const val MAX_NODES = 400
private const val MAX_DEPTH = 40

/**
 * Best-effort heuristic: only fires when the screen has BOTH a price-shaped string
 * AND a clickable node whose text looks like a final-purchase action - reduces false
 * positives on plain product-listing pages that show prices without a checkout CTA.
 */
object PurchaseDetector {

    fun detect(root: AccessibilityNodeInfo?): PurchaseSignal? {
        if (root == null) return null

        var hasBuyButton = false
        var bestPrice: Double? = null
        var bestTitle: String? = null
        var visited = 0

        fun visit(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > MAX_DEPTH || visited >= MAX_NODES) return
            visited++

            val text = node.text?.toString() ?: node.contentDescription?.toString()
            if (!text.isNullOrBlank()) {
                val trimmed = text.trim()
                val lower = trimmed.lowercase()
                if (node.isClickable && !hasBuyButton) {
                    if (BUY_KEYWORDS.any { lower.contains(it) }) {
                        hasBuyButton = true
                    }
                }
                if (bestPrice == null) {
                    parsePrice(trimmed)?.let { bestPrice = it }
                }
                // Best-effort product title: the longest plain text on screen that isn't a
                // price, a buy button, or known checkout/nav chrome. Titles tend to be the
                // most descriptive text on a checkout/cart screen, so "longest wins" is a
                // decent proxy - it's editable in the popup in case this guesses wrong.
                if (isTitleCandidate(trimmed, lower) && (bestTitle == null || trimmed.length > bestTitle!!.length)) {
                    bestTitle = trimmed
                }
            }

            for (i in 0 until node.childCount) {
                if (visited >= MAX_NODES) break
                visit(node.getChild(i), depth + 1)
            }
        }

        visit(root, 0)

        return if (hasBuyButton) PurchaseSignal(price = bestPrice, title = bestTitle) else null
    }

    private fun isTitleCandidate(trimmed: String, lower: String): Boolean {
        if (trimmed.length !in TITLE_MIN_LEN..TITLE_MAX_LEN) return false
        if (lower in TITLE_BLOCKLIST) return false
        if (PRICE_REGEX.containsMatchIn(trimmed)) return false
        if (BUY_KEYWORDS.any { lower.contains(it) }) return false
        if (trimmed.none { it.isLetter() }) return false
        return true
    }

    private fun parsePrice(text: String): Double? {
        val match = PRICE_REGEX.find(text) ?: return null
        val raw = match.groupValues[1].ifBlank { match.groupValues[2] }
        // German-style "1.234,56" vs plain "12,34" / "12.34" - normalize to a dot decimal.
        val normalized = if (raw.contains(',') && raw.substringAfterLast(',').length == 2) {
            raw.replace(".", "").replace(",", ".")
        } else {
            raw.replace(",", "")
        }
        return normalized.toDoubleOrNull()
    }
}

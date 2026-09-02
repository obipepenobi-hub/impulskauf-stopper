package com.liam.kaptalismusaufhalter.guard

import android.view.accessibility.AccessibilityNodeInfo

data class PurchaseSignal(val price: Double?)

private val PRICE_REGEX = Regex("""(\d{1,4}(?:[.,]\d{3})*[.,]\d{2})\s?€|€\s?(\d{1,4}(?:[.,]\d{3})*[.,]\d{2})""")

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
        var visited = 0

        fun visit(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > MAX_DEPTH || visited >= MAX_NODES) return
            visited++

            val text = node.text?.toString() ?: node.contentDescription?.toString()
            if (!text.isNullOrBlank()) {
                if (node.isClickable && !hasBuyButton) {
                    val lower = text.lowercase()
                    if (BUY_KEYWORDS.any { lower.contains(it) }) {
                        hasBuyButton = true
                    }
                }
                if (bestPrice == null) {
                    parsePrice(text)?.let { bestPrice = it }
                }
            }

            for (i in 0 until node.childCount) {
                if (visited >= MAX_NODES) break
                visit(node.getChild(i), depth + 1)
            }
        }

        visit(root, 0)

        return if (hasBuyButton) PurchaseSignal(price = bestPrice) else null
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

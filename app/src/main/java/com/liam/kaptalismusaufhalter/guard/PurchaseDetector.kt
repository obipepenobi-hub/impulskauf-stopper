package com.liam.kaptalismusaufhalter.guard

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.abs

data class PurchaseSignal(val price: Double?, val title: String?, val titleBounds: Rect?)

internal val PRICE_REGEX = Regex("""(\d{1,4}(?:[.,]\d{3})*[.,]\d{2})\s?€|€\s?(\d{1,4}(?:[.,]\d{3})*[.,]\d{2})""")

// Generic checkout/nav chrome that shouldn't be mistaken for the product name - matched
// as a whole-string comparison (lowercased), not a substring, so it doesn't reject real
// titles that merely mention e.g. "Konto". Includes shop-specific cart labels (Amazon uses
// "Einkaufswagen", not the more generic "Warenkorb") since a bare nav tab label is exactly
// the kind of short text that can otherwise win a proximity match by accident.
internal val TITLE_BLOCKLIST = setOf(
    "warenkorb", "mein warenkorb", "einkaufswagen", "mein einkaufswagen", "dein einkaufswagen",
    "listen", "erneut kaufen", "zur kasse", "zur kasse gehen", "kasse", "weiter",
    "weiter zur kasse", "zurück", "login", "anmelden", "registrieren", "suche", "menü",
    "konto", "mein konto", "bestellübersicht", "bestellung aufgeben", "lieferadresse",
    "rechnungsadresse", "zahlungsart", "zahlungsmethode", "gesamtsumme", "gesamt",
    "zwischensumme", "versandkosten", "versand", "inkl. mwst.", "inkl. mwst", "gutschein",
    "rabattcode", "gutscheincode", "agb", "datenschutz", "hilfe", "startseite", "home",
    "impressum", "kontakt", "newsletter", "filialen", "prime", "kostenlose lieferung",
    "jetzt kostenpflichtig bestellen", "kostenpflichtig bestellen", "zahlungspflichtig bestellen"
)

// Marketing/upsell copy near a checkout (recommendation banners, "before you go" nudges) is
// often the longest text on the screen - substring-matched since the exact wording varies a
// lot between shops, unlike the blocklist above which is whole-string nav chrome.
internal val TITLE_REJECT_SUBSTRINGS = listOf(
    "bevor du", "das könnte dir auch gefallen", "das könnte dich auch interessieren",
    "kunden kauften auch", "ähnliche artikel", "empfehlungen", "gesponsert", "anzeige",
    "häufig zusammen gekauft", "andere kunden interessierten sich auch für",
    "nachrichten zu artikeln", "hier sind einige dinge"
)

internal const val TITLE_MIN_LEN = 8

// Real product titles (especially on Amazon) routinely run 100-150+ characters, so this only
// needs to reject genuinely long blocks of text (paragraphs, T&Cs) - the displayed/stored name
// is separately truncated in [truncateTitle], so raising this doesn't risk giant DB entries.
internal const val TITLE_MAX_LEN = 160
private const val TITLE_DISPLAY_MAX_LEN = 100

// Deliberately narrow to final-confirmation phrasing (German consumer-protection law
// requires shops' actual checkout button to say something like "zahlungspflichtig
// bestellen"), not generic "Jetzt kaufen"/"In den Warenkorb" buttons that already sit on
// every product page - those would fire on any browsing, not just the checkout step.
internal val BUY_KEYWORDS = listOf(
    "zahlungspflichtig bestellen", "kostenpflichtig bestellen", "kostenpflichtig kaufen",
    "kostenpflichtig bezahlen", "verbindlich bestellen", "zur kasse gehen", "zur kasse",
    "jetzt bezahlen", "checkout", "place order", "pay now", "complete purchase", "confirm order"
)

// Cap how much of the tree we walk, so a huge/degenerate node tree can't cause an ANR.
private const val MAX_NODES = 400
private const val MAX_DEPTH = 40

internal data class TitleCandidate(val text: String, val bounds: Rect)
internal data class PricePoint(val value: Double, val bounds: Rect)

/**
 * Best-effort heuristic: only fires when the screen has BOTH a price-shaped string
 * AND a clickable node whose text looks like a final-purchase action - reduces false
 * positives on plain product-listing pages that show prices without a checkout CTA.
 */
object PurchaseDetector {

    fun detect(root: AccessibilityNodeInfo?): PurchaseSignal? {
        if (root == null) return null

        var hasBuyButton = false
        val prices = mutableListOf<PricePoint>()
        val titleCandidates = mutableListOf<TitleCandidate>()
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
                parsePrice(trimmed)?.let {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    prices.add(PricePoint(it, bounds))
                }
                if (isTitleCandidate(trimmed, lower)) {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    titleCandidates.add(TitleCandidate(trimmed, bounds))
                }
            }

            for (i in 0 until node.childCount) {
                if (visited >= MAX_NODES) break
                visit(node.getChild(i), depth + 1)
            }
        }

        visit(root, 0)

        if (!hasBuyButton) return null

        val (bestPrice, bestTitle) = pickBestPair(
            prices,
            titleCandidates,
            priceCenterYOf = { (it.bounds.top + it.bounds.bottom) / 2 },
            titleCenterYOf = { (it.bounds.top + it.bounds.bottom) / 2 },
            titleLengthOf = { it.text.length }
        )

        return PurchaseSignal(
            price = bestPrice?.value,
            title = bestTitle?.text?.let { truncateTitle(it) },
            titleBounds = bestTitle?.bounds
        )
    }

    /**
     * A cart/checkout screen can show more than one price (e.g. a subtotal near the top AND
     * the same item's price again next to its title further down) - picking "whichever price
     * appears first" and then finding the nearest title to *that* can anchor on the wrong one
     * (the subtotal, which sits closer to nav chrome like a tab label than to the real title).
     *
     * Instead this pairs every price with its own nearest title candidate, then picks whichever
     * (price, title) pair is closest together overall - the real product row's title sits right
     * next to its own price, which is a tighter pairing than any price has with unrelated chrome
     * elsewhere on the screen.
     *
     * Generic over P/T (rather than taking [PricePoint]/[TitleCandidate] directly) so this
     * selection logic can be unit-tested with plain data - android.graphics.Rect's real fields
     * aren't settable through its stubbed constructor in local JVM unit tests.
     */
    internal fun <P, T> pickBestPair(
        prices: List<P>,
        titles: List<T>,
        priceCenterYOf: (P) -> Int,
        titleCenterYOf: (T) -> Int,
        titleLengthOf: (T) -> Int
    ): Pair<P?, T?> {
        if (prices.isEmpty()) return null to titles.maxByOrNull(titleLengthOf)
        if (titles.isEmpty()) return prices.first() to null

        var bestPrice = prices.first()
        var bestTitle: T? = null
        var bestDistance = Int.MAX_VALUE
        for (price in prices) {
            val priceCenterY = priceCenterYOf(price)
            val nearestTitle = titles.minByOrNull { abs(titleCenterYOf(it) - priceCenterY) } ?: continue
            val distance = abs(titleCenterYOf(nearestTitle) - priceCenterY)
            if (distance < bestDistance) {
                bestDistance = distance
                bestPrice = price
                bestTitle = nearestTitle
            }
        }
        return bestPrice to bestTitle
    }

    internal fun truncateTitle(text: String, maxLen: Int = TITLE_DISPLAY_MAX_LEN): String =
        if (text.length <= maxLen) text else text.take(maxLen).trimEnd() + "…"

    internal fun isTitleCandidate(trimmed: String, lower: String): Boolean {
        if (trimmed.length !in TITLE_MIN_LEN..TITLE_MAX_LEN) return false
        if (lower in TITLE_BLOCKLIST) return false
        if (TITLE_REJECT_SUBSTRINGS.any { lower.contains(it) }) return false
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

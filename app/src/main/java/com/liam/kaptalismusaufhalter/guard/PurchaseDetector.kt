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
    "nachrichten zu artikeln", "hier sind einige dinge",
    "lieferung für", "lieferung am", "geliefert am", "kostenlose lieferung am",
    "auswahl aller artikel", "melde dich an", "einfuhrgebühren", "einfuhrabgaben",
    "zusätzliche gebühren", "gratisversand", "sichere zahlungsoptionen",
    "nutzungsbedingungen", "datenschutzrichtlinie", "erfolgreicher bezahlung",
    "in rechnung gestellt", "als gast bezahlen"
)

// Amazon (and similar shops) reuse "Warenkorb"/"Einkaufswagen" inside many short chrome
// labels beyond the exact strings in TITLE_BLOCKLIST - e.g. "Alle Einkaufswagen" (the
// select-all/deselect-all checkbox above the cart list, which is what got picked as a
// "product name" in a real bug report). Enumerating every such label is a losing game, so
// once a candidate is short (a handful of words) any occurrence of these words is treated
// as cart chrome rather than a real product title - real titles essentially never contain
// them and are almost always much longer anyway.
internal val CART_CHROME_WORDS = setOf("warenkorb", "einkaufswagen")
private const val CART_CHROME_MAX_WORDS = 4

// The recurring failure mode isn't a single bad word - it's entire *sections* below the real
// cart items ("Nochmals kaufen", "Für später gespeichert", recommendation carousels). Those
// sections have their own compact title+price pairs that routinely sit CLOSER together than
// the real item's title and price (which often have seller/stock/discount text between them),
// so they can win the nearest-pair search outright even after individual words are blocklisted.
// Once traversal (which follows the tree in document order, matching the visual top-to-bottom
// order for a normal layout) hits one of these section headers, everything from that point on
// is excluded from title/price collection entirely - a structural cutoff instead of another
// word to chase.
internal val SECTION_END_MARKERS = listOf(
    "für später gespeichert", "später kaufen", "gespeicherte artikel",
    "nochmals kaufen", "häufig erneut gekauft", "wird oft zusammen gekauft",
    "das könnte dir auch gefallen", "das könnte dich auch interessieren",
    "ähnliche artikel", "empfehlungen für dich", "kunden kauften auch",
    "andere kunden interessierten sich auch für", "gesponsert",
    "rücksendungen sind einfach", "inspiriert von deinem", "entdecke",
    "spare bei deiner bestellung", "möchtest ein gerät eintauschen"
)

// A price-shaped number that isn't actually THIS item's price - either a crossed-out "was X€"
// reference price right next to the real one (e.g. Temu's "57,66€ UVP106,39€"), or an order
// total/subtotal bundled with its label in one node (e.g. "Gesamtpreis (inkl. MwSt.) 448,98 €").
// Both match PRICE_REGEX just as well as the real per-item price, and a total in particular can
// easily sit closer to a (long, multi-line) title than the item's own price further down below
// a badge/quantity row does - recognized by the label text shops conventionally pair it with,
// not by strikethrough (accessibility text doesn't carry that formatting).
internal val NON_ITEM_PRICE_MARKERS = listOf(
    "uvp", "statt ", "unverbindliche preisempfehlung", "neupreis", "€ neu",
    "gesamtpreis", "gesamtsumme", "zwischensumme", "gesamt ", "summe"
)

internal const val TITLE_MIN_LEN = 8

// Real product titles (especially on Amazon) routinely run 100-150+ characters, so this only
// needs to reject genuinely long blocks of text (paragraphs, T&Cs) - the displayed/stored name
// is separately truncated in [truncateTitle], so raising this doesn't risk giant DB entries.
internal const val TITLE_MAX_LEN = 160
private const val TITLE_DISPLAY_MAX_LEN = 100

// Mostly scoped to final-confirmation phrasing (German consumer-protection law requires
// shops' actual checkout button to say something like "zahlungspflichtig bestellen"), not
// generic "Jetzt kaufen"/"In den Warenkorb" buttons that already sit on every product page -
// those would fire on any browsing, not just the checkout step. Also includes a few real
// multi-step-checkout and single-tap-purchase phrasings found via testing against real shops
// (Refurbed's cart says "Weiter zum Versand", not any final-order wording at all; Play Store's
// pre-order flow has no cart step, so "vorbestellen" IS the purchase-committing tap) - these are
// still specific enough phrases (not bare "Weiter"/"Kaufen") that they shouldn't fire on
// ordinary browsing, and detect() only fires when a price is also visible on the same screen.
internal val BUY_KEYWORDS = listOf(
    "zahlungspflichtig bestellen", "kostenpflichtig bestellen", "kostenpflichtig kaufen",
    "kostenpflichtig bezahlen", "verbindlich bestellen", "zur kasse gehen", "zur kasse",
    "jetzt bezahlen", "checkout", "place order", "pay now", "complete purchase", "confirm order",
    "vorbestellen", "weiter zum versand", "weiter zur bezahlung", "weiter zur lieferadresse",
    "bestellung abschließen", "kauf abschließen", "jetzt abonnieren"
)

// Cap how much of the tree we walk, so a huge/degenerate node tree can't cause an ANR.
private const val MAX_NODES = 400
private const val MAX_DEPTH = 40

// How many of a price's nearest title candidates to consider before picking the longest one -
// see the comment on pickBestPair for why "nearest wins" alone isn't enough. Needs to be
// generous enough that the real title is still in the running even when something bulky (a
// cover image, a multi-line badge/quantity block) sits between it and its own price - a real
// Play Store screen needed 8 before the actual book title beat several short nearby labels.
private const val NEAREST_TITLES_TO_CONSIDER = 8

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
        var pastPurchaseSection = false
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

                if (!pastPurchaseSection && SECTION_END_MARKERS.any { lower.contains(it) }) {
                    pastPurchaseSection = true
                }

                // The buy button itself can legitimately appear after an upsell section on some
                // real pages (e.g. a trade-in promo sandwiched between the cart item and the
                // "continue" button) - the section cutoff below only stops title/price
                // collection, it must never also suppress buy-button detection.
                if (node.isClickable && !hasBuyButton) {
                    if (BUY_KEYWORDS.any { lower.contains(it) }) {
                        hasBuyButton = true
                    }
                }
                if (!pastPurchaseSection) {
                    if (NON_ITEM_PRICE_MARKERS.none { lower.contains(it) }) {
                        parsePrice(trimmed)?.let {
                            val bounds = Rect()
                            node.getBoundsInScreen(bounds)
                            prices.add(PricePoint(it, bounds))
                        }
                    }
                    if (isTitleCandidate(trimmed, lower)) {
                        val bounds = Rect()
                        node.getBoundsInScreen(bounds)
                        titleCandidates.add(TitleCandidate(trimmed, bounds))
                    }
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
     * A real product row also routinely has short incidental text sitting BETWEEN its title and
     * its own price - stock status ("Auf Lager"), seller info ("Verkäufer: X"), condition, etc.
     * Those are frequently closer to the price than the real (much longer) title is, so simply
     * picking the single nearest title candidate per price can pick one of them instead. To
     * guard against that, each price's title is chosen from among its [NEAREST_TITLES_TO_CONSIDER]
     * nearest candidates by picking the LONGEST of that shortlist, not just the nearest one -
     * short incidental labels lose to the real title on length once both are in the running.
     *
     * Across all prices, this then picks whichever (price, title) pair ends up closest together
     * overall - the real product row's pairing is still tighter than any price has with unrelated
     * chrome elsewhere on the screen.
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
            val shortlist = titles
                .sortedBy { abs(titleCenterYOf(it) - priceCenterY) }
                .take(NEAREST_TITLES_TO_CONSIDER)
            val chosenTitle = shortlist.maxByOrNull(titleLengthOf) ?: continue
            val distance = abs(titleCenterYOf(chosenTitle) - priceCenterY)
            if (distance < bestDistance) {
                bestDistance = distance
                bestPrice = price
                bestTitle = chosenTitle
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
        if (isCartChromeLabel(lower)) return false
        if (PRICE_REGEX.containsMatchIn(trimmed)) return false
        if (BUY_KEYWORDS.any { lower.contains(it) }) return false
        if (trimmed.none { it.isLetter() }) return false
        return true
    }

    private fun isCartChromeLabel(lower: String): Boolean {
        if (lower.split(Regex("\\s+")).size > CART_CHROME_MAX_WORDS) return false
        return CART_CHROME_WORDS.any { lower.contains(it) }
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

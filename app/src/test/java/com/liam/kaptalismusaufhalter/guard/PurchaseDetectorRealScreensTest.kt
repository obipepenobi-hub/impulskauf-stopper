package com.liam.kaptalismusaufhalter.guard

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * End-to-end tests for [PurchaseDetector.detect] against full accessibility node trees modeled
 * directly on the real Amazon cart screenshots from the bug report ("Alle Einkaufswagen" picked
 * instead of the actual product). [PurchaseDetectorTest] covers the pure pairing/filtering logic
 * in isolation with plain data; this runs under Robolectric so a real
 * android.view.accessibility.AccessibilityNodeInfo tree - with real Rect bounds and real
 * parent/child traversal - can be exercised, catching structural bugs (traversal order, the
 * section-cutoff logic) that isolated function tests can't see.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PurchaseDetectorRealScreensTest {

    @Suppress("DEPRECATION")
    private fun node(
        text: String? = null,
        contentDescription: String? = null,
        clickable: Boolean = false,
        bounds: Rect? = null,
        children: List<AccessibilityNodeInfo> = emptyList()
    ): AccessibilityNodeInfo {
        val info = AccessibilityNodeInfo.obtain()
        text?.let { info.text = it }
        contentDescription?.let { info.contentDescription = it }
        info.isClickable = clickable
        bounds?.let { info.setBoundsInScreen(it) }
        val shadow = shadowOf(info)
        children.forEach { shadow.addChild(it) }
        return info
    }

    private fun row(y: Int, height: Int = 40) = Rect(0, y, 1080, y + height)

    private val realTitle =
        "49CC 2 Takt Pocket Bike Motor Benzinmotor für Fahrrad Motorisiertes Fahrrad 66/80cc Set mit Zubehör"

    /**
     * Rebuilds the exact real cart from the bug report: search bar, cart/lists/reorder tabs,
     * an order-status banner, subtotal + checkout button, the "select all" checkbox (whose
     * accessible name was literally "Alle Einkaufswagen" - the thing that got picked as a
     * "product"), a delivery-date line, the one real cart item (with seller/stock/discount text
     * between its title and price, exactly like the real layout), a return-policy blurb, a gift
     * card banner, a "Nochmals kaufen" carousel, and three "Für später gespeichert" cards - each
     * of which pairs its own title tightly with its own price, which is exactly what let a saved
     * item outrank the real product before the section-cutoff was added.
     */
    private fun buildRealAmazonCart(): AccessibilityNodeInfo = node(children = listOf(
        node(text = "Suchen oder eine Frage stellen", bounds = row(20)),
        node(text = "Einkaufswagen", clickable = true, bounds = row(80)),
        node(text = "Listen", clickable = true, bounds = row(80)),
        node(text = "Erneut kaufen", clickable = true, bounds = row(80)),
        node(text = "Nachrichten zu Artikeln im Einkaufswagen", bounds = row(150)),
        node(text = "Zwischensumme 39,99 €", bounds = row(260)),
        node(text = "Zur Kasse gehen (1 Artikel)", clickable = true, bounds = row(300)),
        node(contentDescription = "Alle Einkaufswagen", clickable = true, bounds = row(350)),
        node(text = "Lieferung für 20 € 18. - 19. Sept.", bounds = row(400)),
        node(children = listOf(
            node(clickable = true, bounds = row(460)),
            node(text = realTitle, bounds = row(460, height = 60)),
            node(text = "39,99 €", bounds = row(530)),
            node(text = "Auf Lager", bounds = row(560)),
            node(text = "Verkäufer: WDRAVEN", clickable = true, bounds = row(580)),
            node(text = "Spare 5 % bei 2 ausgewählten Artikeln", bounds = row(600)),
            node(text = "1", clickable = true, bounds = row(640)),
            node(text = "Löschen", clickable = true, bounds = row(670)),
            node(text = "Für später speichern", clickable = true, bounds = row(670)),
            node(text = "Teilen", clickable = true, bounds = row(670))
        )),
        node(text = "Rücksendungen sind einfach", bounds = row(750)),
        node(text = "Liam, schenke Freude mit Amazon.de-Geschenkgutscheinen", bounds = row(820)),
        node(text = "Nochmals kaufen", bounds = row(900)),
        node(children = listOf(
            node(text = "UGREEN USB C Netzteil 30W", bounds = row(950)),
            node(text = "8,96 €", bounds = row(990))
        )),
        node(text = "Für später gespeichert", bounds = row(1100)),
        node(children = listOf(
            node(text = "FFowcye 6 Stück Acryl Nagellack Regal Wand, Nagellackhalter", bounds = row(1150)),
            node(text = "18,99 €", bounds = row(1190))
        )),
        node(children = listOf(
            node(text = "SOLARTRONICS Spannungswandler NM1000 12V auf 230V", bounds = row(1300)),
            node(text = "74,91 €", bounds = row(1340))
        )),
        node(children = listOf(
            node(text = "UGREEN Zapix 2-in-1 15W Qi2 Magnetisches Kabelloses Ladegeraet", bounds = row(1450)),
            node(text = "-33 %", bounds = row(1480)),
            node(text = "19,99 €", bounds = row(1490)),
            node(text = "UVP: 29,99 €", bounds = row(1495))
        ))
    ))

    @Test
    fun `detects the real product and price in the exact cart from the bug report`() {
        val signal = PurchaseDetector.detect(buildRealAmazonCart())

        assertTrue(
            "expected the real product title, got: ${signal?.title}",
            signal?.title?.startsWith("49CC 2 Takt Pocket Bike Motor") == true
        )
        assertEquals(39.99, signal?.price)
    }

    @Test
    fun `never picks a saved-for-later item as the product`() {
        val signal = PurchaseDetector.detect(buildRealAmazonCart())

        val savedTitles = listOf("Nagellack", "Spannungswandler", "Zapix")
        assertTrue(
            "picked a saved-for-later item instead of the real product: ${signal?.title}",
            savedTitles.none { signal?.title?.contains(it) == true }
        )
    }

    @Test
    fun `never picks the select-all checkbox or delivery line as the product`() {
        val signal = PurchaseDetector.detect(buildRealAmazonCart())

        assertTrue(signal?.title?.contains("Einkaufswagen") != true)
        assertTrue(signal?.title?.contains("Lieferung") != true)
    }

    @Test
    fun `does not fire at all on a plain product page without a checkout button`() {
        val root = node(children = listOf(
            node(text = realTitle, bounds = row(200, height = 60)),
            node(text = "39,99 €", bounds = row(280)),
            node(text = "In den Warenkorb", clickable = true, bounds = row(340)),
            node(text = "Jetzt kaufen", clickable = true, bounds = row(390))
        ))

        assertTrue(PurchaseDetector.detect(root) == null)
    }

    /**
     * Rebuilds a real Google Play Books pre-order screen - a single-tap purchase flow with no
     * cart step at all, so "vorbestellen" (now in BUY_KEYWORDS) is the only purchase-committing
     * tap there is. The cover image pushes the actual book title well above several much closer
     * short labels (page count, availability date, publisher) - this is what pushed
     * NEAREST_TITLES_TO_CONSIDER from 5 to 8.
     */
    private fun buildGooglePlayBookPreorder(): AccessibilityNodeInfo = node(children = listOf(
        node(contentDescription = "Zurück", clickable = true, bounds = row(20)),
        node(contentDescription = "Merken", clickable = true, bounds = row(20)),
        node(contentDescription = "Suche", clickable = true, bounds = row(20)),
        node(contentDescription = "Mehr Optionen", clickable = true, bounds = row(20)),
        node(
            text = "Fallen Rook: Roman - Der Dark-Mafia-Romance-Hype endlich auf Deutsch! " +
                "Mit Farbschnitt in limitierter Auflage!",
            bounds = row(120, height = 100)
        ),
        node(text = "Buch 3", bounds = row(240)),
        node(text = "L J Shen", clickable = true, bounds = row(270)),
        node(text = "Blanvalet Taschenbuch Verlag", bounds = row(300)),
        node(text = "E-Book", bounds = row(350)),
        node(text = "544 Seiten", bounds = row(350)),
        node(text = "Für 14,99 € vorbestellen", clickable = true, bounds = row(400)),
        node(text = "Verfügbar am 30. September 2026", bounds = row(450)),
        node(
            text = "Bewertungen und Rezensionen werden nicht geprüft. Weitere Informationen",
            bounds = row(500, height = 60)
        ),
        node(text = "Zum Hörbuch wechseln", clickable = true, bounds = row(580)),
        node(text = "Abo für Buchreihe", clickable = true, bounds = row(630)),
        node(text = "Spiele", clickable = true, bounds = row(1800)),
        node(text = "Apps", clickable = true, bounds = row(1800)),
        node(text = "Suche", clickable = true, bounds = row(1800)),
        node(text = "Bücher", clickable = true, bounds = row(1800)),
        node(text = "Mein Play", clickable = true, bounds = row(1800))
    ))

    @Test
    fun `fires on a one-tap Play Store pre-order and picks the book title, not a nearby label`() {
        val signal = PurchaseDetector.detect(buildGooglePlayBookPreorder())

        assertTrue(
            "expected the book title, got: ${signal?.title}",
            signal?.title?.startsWith("Fallen Rook") == true
        )
        assertEquals(14.99, signal?.price)
    }

    /**
     * Rebuilds the Temu cart as a phone would actually present it (single column, top to
     * bottom) rather than the desktop screenshot's two-column layout - this is a phone
     * accessibility service, and on a phone the checkout button sits in the normal reading
     * order right after the item, not in a separate sidebar region. Covers: banner ads, a
     * customs-duty notice right above the item, a crossed-out "UVP" reference price on the
     * SAME line as the real price, an account-login prompt, legal-disclaimer boilerplate around
     * the checkout button, and a "Entdecke Temus Auswahl" recommendations header below it all.
     */
    private fun buildTemuCart(): AccessibilityNodeInfo = node(children = listOf(
        node(text = "Gratisversand Unglaublich", bounds = row(20)),
        node(text = "Zusätzliche Einfuhrgebühren Bestellungen, die von außerhalb der EU versandt werden", bounds = row(20)),
        node(text = "Startseite", clickable = true, bounds = row(100)),
        node(text = "Warenkorb", bounds = row(100)),
        node(text = "Melde dich an, um deinen Warenkorb in deinem Konto zu speichern.", bounds = row(150)),
        node(text = "Google", clickable = true, bounds = row(200)),
        node(text = "Anmelden / Registrieren", clickable = true, bounds = row(250)),
        node(
            text = "Indem Sie fortfahren, stimmen Sie unseren Nutzungsbedingungen zu und " +
                "bestätigen, dass Sie unsere Datenschutzrichtlinie gelesen haben.",
            bounds = row(300)
        ),
        node(text = "Gratisversand Unglaublich", bounds = row(360)),
        node(
            text = "Keine Einfuhrabgaben für alle Artikel, die von Verkäufern versandt werden, " +
                "und keine zusätzlichen Gebühren bei der Lieferung",
            bounds = row(410)
        ),
        node(text = "Alle auswählen (1)", clickable = true, bounds = row(480)),
        node(text = "Zusätzliche Einfuhrgebühren werden an der Kasse berechnet", bounds = row(520)),
        node(children = listOf(
            node(
                text = "Modernes Duschsystem Set mit LED-Umgebungsbeleuchtung, einfache Installation, " +
                    "4 Sprühmodi, Duschkopf mit Filter, Handbrause und Kopfbrause, für Badezimmer",
                bounds = row(560, height = 40)
            ),
            node(text = "Produktspezifikation: S-904", clickable = true, bounds = row(605)),
            node(text = "Top-Auswahl", bounds = row(640)),
            node(text = "Menge 1", clickable = true, bounds = row(675)),
            node(text = "57,66€", bounds = row(685)),
            node(text = "UVP106,39€", bounds = row(685)),
            node(text = "Versand auf dem Landweg, Lieferung: 15. Okt.-5. Nov.", bounds = row(720))
        )),
        node(
            text = "Du kannst deine Artikel nicht finden? Melde dich an, um deinen Warenkorb zu sehen",
            bounds = row(780)
        ),
        node(text = "Bestellübersicht", bounds = row(840)),
        node(text = "Gesamtsumme 57,66 €", bounds = row(880)),
        node(text = "Zur Kasse (1)", clickable = true, bounds = row(930)),
        node(
            text = "Die Verfügbarkeit und die Preise der Artikel werden nur bei erfolgreicher " +
                "Bezahlung garantiert.",
            bounds = row(980)
        ),
        node(text = "Du kannst als Gast bezahlen.", bounds = row(1030)),
        node(
            text = "Dir werden die Kosten erst in Rechnung gestellt, wenn du diese Bestellung " +
                "auf der nächsten Seite überprüft hast.",
            bounds = row(1080)
        ),
        node(text = "Sichere Zahlungsoptionen", bounds = row(1130)),
        node(text = "Entdecke Temus Auswahl", bounds = row(1200))
    ))

    @Test
    fun `detects the real product and sale price, not the crossed-out UVP, on the Temu cart`() {
        val signal = PurchaseDetector.detect(buildTemuCart())

        assertTrue(
            "expected the shower system title, got: ${signal?.title}",
            signal?.title?.startsWith("Modernes Duschsystem") == true
        )
        assertEquals(57.66, signal?.price)
    }

    /**
     * Rebuilds a real refurbished-marketplace cart (iPad Air + an add-on repair/replacement
     * service). The cart's own "continue" button says "Weiter zum Versand" ("continue to
     * shipping") rather than any of the original final-checkout phrasing - now recognized since
     * BUY_KEYWORDS was widened for exactly this multi-step-checkout case. Also covers two
     * things that could otherwise outrank the iPad: the tightly-packed "BackUp" add-on service
     * (title right next to its own price, unlike the iPad's which sits below a badge/quantity
     * row) and a trade-in promo sentence long enough to win on length alone.
     */
    private fun buildRefurbedCart(): AccessibilityNodeInfo = node(children = listOf(
        node(contentDescription = "Zurück", clickable = true, bounds = row(20)),
        node(text = "Dein Warenkorb", bounds = row(20)),
        node(contentDescription = "Merken", clickable = true, bounds = row(20)),
        node(text = "Gesamtpreis (inkl. MwSt.) 448,98 €", clickable = true, bounds = row(300)),
        node(children = listOf(
            node(
                text = "iPad Air 10.9\" (2022) 5. Generation 64 GB - WLAN - Polarstern",
                bounds = row(430, height = 160)
            ),
            node(text = "Sehr gut", bounds = row(615)),
            node(text = "1", clickable = true, bounds = row(723)),
            node(text = "Nur 1 übrig", bounds = row(723)),
            node(text = "390,00 €", bounds = row(730)),
            node(text = "769,00 € neu", bounds = row(755))
        )),
        node(text = "Kostenlose Lieferung: 17.-18. Sept.", bounds = row(870)),
        node(children = listOf(
            node(text = "BackUp Reparatur- & Ersatzservice - 12-monate", clickable = true, bounds = row(1100)),
            node(text = "52,99 €", bounds = row(1108))
        )),
        node(text = "Spare bei deiner Bestellung", bounds = row(1230)),
        node(text = "Du möchtest ein Gerät eintauschen?", bounds = row(1290)),
        node(
            text = "Bring den Preis deines Warenkorbs herunter, indem du ein Altgerät verkaufst. So funktioniert's",
            bounds = row(1340)
        ),
        node(text = "Eintauschwert entdecken", clickable = true, bounds = row(1420)),
        node(text = "Weiter zum Versand", clickable = true, bounds = row(1520))
    ))

    @Test
    fun `fires on 'Weiter zum Versand' and picks the main item, not the add-on or the trade-in promo`() {
        val signal = PurchaseDetector.detect(buildRefurbedCart())

        assertTrue(
            "expected the iPad, got: ${signal?.title}",
            signal?.title?.startsWith("iPad Air") == true
        )
        assertEquals(390.00, signal?.price)
    }
}

package bw.mmele

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** values-tn must cover every translatable English string with the same placeholders. */
class LocalizationTest {
    private val ph = Regex("%\\d+\\$[sd]")

    private fun load(dir: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        File("src/main/res/$dir").listFiles { f -> f.name.startsWith("strings") && f.extension == "xml" }!!.forEach { f ->
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f)
            val nodes = doc.documentElement.childNodes
            for (i in 0 until nodes.length) {
                val e = nodes.item(i) as? org.w3c.dom.Element ?: continue
                if (e.getAttribute("translatable") == "false") continue
                val name = e.getAttribute("name")
                when (e.tagName) {
                    "string" -> out[name] = e.textContent
                    "plurals" -> {
                        val items = e.getElementsByTagName("item")
                        for (j in 0 until items.length) {
                            val it = items.item(j) as org.w3c.dom.Element
                            out["$name#${it.getAttribute("quantity")}"] = it.textContent
                        }
                    }
                }
            }
        }
        return out
    }

    @Test fun setswanaCoversEveryStringWithMatchingPlaceholders() {
        val en = load("values")
        val tn = load("values-tn")
        assertTrue(en.size > 300)
        assertEquals("missing in values-tn", emptySet<String>(), en.keys - tn.keys)
        assertEquals("extra in values-tn", emptySet<String>(), tn.keys - en.keys)
        for ((k, v) in en) {
            assertEquals("placeholders differ in $k", ph.findAll(v).map { it.value }.sorted().toList(), ph.findAll(tn.getValue(k)).map { it.value }.sorted().toList())
            assertTrue("empty translation $k", tn.getValue(k).isNotBlank())
        }
    }

    @Test fun reviewDocListsEveryString() {
        val doc = File("../docs/SETSWANA_REVIEW.md").readText()
        for (k in load("values").keys) assertTrue("$k missing from SETSWANA_REVIEW.md", doc.contains("`${k.replace("#", " (")}${if ('#' in k) ")" else ""}`"))
    }

    @Test fun localesConfigListsBothLanguages() {
        val cfg = File("src/main/res/xml/locales_config.xml").readText()
        assertTrue(cfg.contains("\"en\"") && cfg.contains("\"tn\""))
    }
}

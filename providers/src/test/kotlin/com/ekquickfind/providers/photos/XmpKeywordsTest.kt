package com.ekquickfind.providers.photos

import org.junit.Assert.assertEquals
import org.junit.Test

class XmpKeywordsTest {
    private fun xmp(body: String) =
        """
        <x:xmpmeta xmlns:x="adobe:ns:meta/"><rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:lr="http://ns.adobe.com/lightroom/1.0/">$body</rdf:Description>
        </rdf:RDF></x:xmpmeta>
        """.trimIndent()

    @Test
    fun readsDublinCoreSubjects() {
        val keywords =
            XmpKeywords.parse(
                xmp("<dc:subject><rdf:Bag><rdf:li>Holiday</rdf:li><rdf:li> Crete </rdf:li></rdf:Bag></dc:subject>"),
            )
        assertEquals(listOf("Holiday", "Crete"), keywords)
    }

    @Test
    fun hierarchicalKeywordsKeepTheirLastLevelAndDuplicatesCollapse() {
        val body =
            "<dc:subject><rdf:Bag><rdf:li>Dog</rdf:li></rdf:Bag></dc:subject>" +
                "<lr:hierarchicalSubject><rdf:Bag><rdf:li>Animals|Pets|Dog</rdf:li><rdf:li>Places|Greece|Chania</rdf:li></rdf:Bag></lr:hierarchicalSubject>"
        assertEquals(listOf("Dog", "Chania"), XmpKeywords.parse(xmp(body)))
    }

    @Test
    fun entitiesAreDecoded() {
        assertEquals(
            listOf("Tom & Jerry"),
            XmpKeywords.parse(xmp("<dc:subject><rdf:Bag><rdf:li>Tom &amp; Jerry</rdf:li></rdf:Bag></dc:subject>")),
        )
    }

    @Test
    fun missingOrBrokenXmpHasNoKeywords() {
        assertEquals(emptyList<String>(), XmpKeywords.parse(null))
        assertEquals(emptyList<String>(), XmpKeywords.parse("not xmp at all"))
        assertEquals(emptyList<String>(), XmpKeywords.parse(xmp("<dc:title><rdf:Alt><rdf:li>Title only</rdf:li></rdf:Alt></dc:title>")))
    }
}

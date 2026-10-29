package com.ekquickfind.core.search.quick

import com.ekquickfind.core.search.CopyAction
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Calculator and unit conversion, computed synchronously so the answer lands with the keystroke. */
class QuickAnswersProvider : SearchProvider {
    override val id = ProviderId("quick-answers")
    override val section = Section.QuickAnswers

    override fun search(query: Query): Flow<List<SearchResult>> = flowOf(listOfNotNull(answer(query.raw)))

    private fun answer(text: String): SearchResult? {
        Calculator.evaluate(text)?.let { calc ->
            return row("calc", title = calc.formatted, subtitle = "${calc.expression} =", glyph = Glyph.Calculator)
        }
        UnitConverter.convert(text)?.let { conv ->
            val value = conv.formatted.substringBefore(' ')
            return row("convert", title = conv.formatted, subtitle = "${conv.source} =", glyph = Glyph.Convert, copyValue = value)
        }
        return null
    }

    private fun row(
        kind: String,
        title: String,
        subtitle: String,
        glyph: Glyph,
        copyValue: String = title,
    ) = SearchResult(
        id = ResultId("$id:$kind"),
        providerId = id,
        section = section,
        title = title,
        subtitle = subtitle,
        icon = ResultIcon.Vector(glyph),
        score = 1f,
        primaryAction = CopyAction(copyValue),
        isRecordable = false,
        isPromotable = true,
    )
}

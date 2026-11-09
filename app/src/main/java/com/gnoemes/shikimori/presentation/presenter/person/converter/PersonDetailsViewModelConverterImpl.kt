package com.gnoemes.shikimori.presentation.presenter.person.converter

import android.content.Context
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentType
import com.gnoemes.shikimori.entity.common.presentation.DetailsDescriptionItem
import com.gnoemes.shikimori.entity.common.presentation.DetailsHeadSimpleItem
import com.gnoemes.shikimori.entity.common.presentation.richtext.Block
import com.gnoemes.shikimori.entity.common.presentation.richtext.Inline
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichText
import com.gnoemes.shikimori.entity.common.presentation.richtext.Style
import com.gnoemes.shikimori.entity.roles.domain.PersonDetails
import com.gnoemes.shikimori.presentation.presenter.common.converter.DetailsContentViewModelConverter
import com.gnoemes.shikimori.utils.date.DateTimeConverter
import org.joda.time.DateTime
import org.joda.time.Years
import javax.inject.Inject

class PersonDetailsViewModelConverterImpl @Inject constructor(
        private val context: Context,
        private val dateTimeConverter: DateTimeConverter,
        private val contentConverter: DetailsContentViewModelConverter
) : PersonDetailsViewModelConverter {

    override fun apply(t: PersonDetails): List<Any> {
        val items = mutableListOf<Any>()

        val head = convertHeadItem(t)
        items.add(head)

        val roles = convertRoles(t.roles)
        items.add(DetailsDescriptionItem(roles))

        items.add(Pair(DetailsContentType.CHARACTERS, contentConverter.apply(t.characters)))
        items.add(Pair(DetailsContentType.WORKS, contentConverter.apply(t.works)))

        return items
    }

    //the same text as before («Сэйю: 12» a line), built for the content view directly: it was HTML for
    //Html.fromHtml, and is not user text
    private fun convertRoles(roles: List<List<String?>?>): RichText {
        //each role: its name, «: », its count, then any further part on a line of its own
        val lines = roles.flatMap { role ->
            val parts = role.orEmpty().filterNotNull().filter { it.isNotBlank() }
            if (parts.isEmpty()) emptyList()
            else listOf(parts[0] + ": " + parts.getOrElse(1) { "" }) + parts.drop(2)
        }
        if (lines.isEmpty()) return RichText.EMPTY
        val inlines = lines.flatMapIndexed { index, line ->
            (if (index > 0) listOf(Inline.LineBreak) else emptyList()) + Inline.Text(line, Style.PLAIN)
        }
        return RichText(listOf(Block.Paragraph(inlines)))
    }

    private fun convertHeadItem(t: PersonDetails): DetailsHeadSimpleItem = DetailsHeadSimpleItem(
            t.image,
            context.getString(R.string.on_eng),
            t.name,
            context.getString(R.string.on_jp),
            t.nameJp,
            context.getString(R.string.person_birthday),
            convertBirthDay(t.birthDay),
            t.jobTitle
    )

    private fun convertBirthDay(birthDay: DateTime?): String? {
        val date = dateTimeConverter.convertToFullHumanDateString(birthDay)
        return if (date != null) {
            val years = Years.yearsBetween(birthDay, DateTime.now()).years
            "$date ($years)"
        } else null
    }
}
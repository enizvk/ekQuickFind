package com.ekquickfind.providers.contacts

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.providers.android

data class OpenContactAction(
    val contactId: Long,
    val lookupKey: String,
    override val label: String = "Open",
) : Action {
    override val glyph = Glyph.Contact

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val uri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
        return if (host.android().startActivity(Intent(Intent.ACTION_VIEW, uri))) ActionOutcome.Close else ActionOutcome.Stay
    }
}

/** Dials or texts a number, asking which one when the contact has several. */
data class NumberAction(
    val numbers: List<ContactPhone>,
    val kind: Kind,
) : Action {
    enum class Kind(
        val label: String,
        val glyph: Glyph,
        val intentAction: String,
        val scheme: String,
    ) {
        Call("Call", Glyph.Call, Intent.ACTION_DIAL, "tel"),
        Message("Message", Glyph.Message, Intent.ACTION_SENDTO, "smsto"),
    }

    override val label = kind.label
    override val glyph = kind.glyph

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val number =
            if (numbers.size == 1) {
                numbers.single()
            } else {
                val index = host.choose(kind.label, numbers.map { "${phoneLabel(it)} · ${it.number}" }) ?: return ActionOutcome.Stay
                numbers[index]
            }
        val intent = Intent(kind.intentAction, Uri.fromParts(kind.scheme, number.number, null))
        return if (host.android().startActivity(intent)) ActionOutcome.Close else ActionOutcome.Stay
    }
}

internal fun phoneLabel(phone: ContactPhone): String =
    phone.customLabel?.takeIf { it.isNotBlank() } ?: when (phone.type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK, ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "Work"
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
        else -> "Other"
    }

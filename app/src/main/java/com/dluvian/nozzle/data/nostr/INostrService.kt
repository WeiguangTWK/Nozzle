package com.dluvian.nozzle.data.nostr

import com.dluvian.nozzle.data.nostr.client.model.ContactListEntry
import com.dluvian.nozzle.data.nostr.client.model.Event
import com.dluvian.nozzle.data.nostr.client.model.Filter
import com.dluvian.nozzle.data.nostr.client.model.Metadata
import com.dluvian.nozzle.data.nostr.client.model.ReplyTo

interface INostrService {
    fun publishProfile(metadata: Metadata): Event

    fun sendPost(content: String, relays: Collection<String>?): Event

    fun sendRepost(
        postId: String,
        postPubkey: String,
        quote: String,
        originUrl: String,
        relays: Collection<String>?
    ): Event

    fun sendLike(postId: String, postPubkey: String, relays: Collection<String>?): Event

    fun sendReply(
        replyTo: ReplyTo,
        replyToPubkey: String,
        content: String,
        relays: Collection<String>?
    ): Event

    fun updateContactList(contacts: List<ContactListEntry>): Event

    fun subscribe(
        filters: List<Filter>,
        unsubOnEOSE: Boolean,
        relays: Collection<String>?,
    ): List<String>

    fun unsubscribe(subscriptionIds: List<String>)

    fun close()
}

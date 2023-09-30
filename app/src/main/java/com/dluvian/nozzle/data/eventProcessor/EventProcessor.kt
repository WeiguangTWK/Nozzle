package com.dluvian.nozzle.data.eventProcessor

import android.util.Log
import com.dluvian.nozzle.data.cache.IIdCache
import com.dluvian.nozzle.data.nostr.utils.KeyUtils
import com.dluvian.nozzle.data.room.AppDatabase
import com.dluvian.nozzle.data.room.entity.HashtagEntity
import com.dluvian.nozzle.data.room.entity.Nip65Entity
import com.dluvian.nozzle.data.room.entity.PostEntity
import com.dluvian.nozzle.data.room.entity.ProfileEntity
import com.dluvian.nozzle.data.utils.TimeConstants
import com.dluvian.nozzle.data.utils.UrlUtils.removeTrailingSlashes
import com.dluvian.nozzle.data.utils.getCurrentTimeInSeconds
import com.dluvian.nozzle.model.nostr.Event
import com.dluvian.nozzle.model.nostr.Metadata
import com.dluvian.nozzle.model.nostr.Tag
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

private const val TAG = "EventProcessor"

class EventProcessor(
    private val dbSweepExcludingCache: IIdCache,
    private val database: AppDatabase,
) : IEventProcessor {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
    private val otherIdsCache = Collections.synchronizedSet(mutableSetOf<String>())
    private var upperTimeBoundary = getUpperTimeBoundary()

    // TODO: Remove from cache if fails

    override fun process(event: Event, relayUrl: String?) {
        if (isFromFuture(event)) {
            Log.w(TAG, "Discard event from the future ${event.id}")
            return
        }
        if (event.isPost()) {
            processPost(event = event, relayUrl = relayUrl)
            return
        }
        if (event.isProfileMetadata()) {
            processMetadata(event = event)
            return
        }
        if (event.isContactList()) {
            processContactList(event = event)
            return
        }
        if (event.isNip65()) {
            processNip65(event = event)
            return
        }
        if (event.isReaction()) {
            processReaction(event = event)
            return
        }
    }

    private fun processPost(event: Event, relayUrl: String?) {
        if (!verify(event)) return
        insertEventRelay(
            eventId = event.id,
            relayUrl = relayUrl?.removeTrailingSlashes()
        )

        val wasNew = dbSweepExcludingCache.addPostId(event.id)
        if (!wasNew) return

        scope.launch {
            database.postDao().insertIfNotPresent(PostEntity.fromEvent(event))
            // TODO: Insert hashtags in tx
            val hashtags = event.getHashtags()
                .map { HashtagEntity(eventId = event.id, hashtag = it) }
            if (hashtags.isNotEmpty()) {
                Log.d(TAG, "Insert hashtags: $hashtags")
                database.hashtagDao().insertOrIgnore(*hashtags.toTypedArray())
            }
        }
    }

    private fun processContactList(event: Event) {
        if (otherIdsCache.contains(event.id)) return
        if (!verify(event)) return

        otherIdsCache.add(event.id)

        scope.launch {
            database.contactDao().insertAndDeleteOutdated(
                pubkey = event.pubkey,
                newTimestamp = event.createdAt,
                contactPubkeys = getContactPubkeys(event.tags),
            )
            dbSweepExcludingCache.addContactListAuthor(event.pubkey)
        }
    }

    private fun processMetadata(event: Event) {
        if (otherIdsCache.contains(event.id)) return
        if (!verify(event)) return

        otherIdsCache.add(event.id)

        Log.d(TAG, "Process profile event ${event.content}")
        deserializeMetadata(event.content)?.let {
            scope.launch {
                database.profileDao().insertAndDeleteOutdated(
                    pubkey = event.pubkey,
                    newTimestamp = event.createdAt,
                    ProfileEntity(
                        pubkey = event.pubkey,
                        metadata = Metadata(
                            name = it.name.orEmpty().trim(),
                            about = it.about.orEmpty().trim(),
                            picture = it.picture.orEmpty().trim(),
                            nip05 = it.nip05.orEmpty().trim(),
                            lud16 = it.lud16.orEmpty().trim(),
                        ),
                        createdAt = event.createdAt,
                    )
                )
                dbSweepExcludingCache.addPubkey(event.pubkey)
            }
        }
    }

    private fun processNip65(event: Event) {
        if (otherIdsCache.contains(event.id)) return

        val nip65Entries = event.getNip65Entries()
        if (nip65Entries.isEmpty()) return
        if (!verify(event)) return

        otherIdsCache.add(event.id)

        Log.d(TAG, "Process ${nip65Entries.size} nip65 entries from ${event.pubkey}")
        scope.launch {
            val entities = nip65Entries.map {
                Nip65Entity(
                    pubkey = event.pubkey,
                    url = it.url,
                    isRead = it.isRead,
                    isWrite = it.isWrite,
                    createdAt = event.createdAt,
                )
            }
            database.nip65Dao().insertAndDeleteOutdated(
                pubkey = event.pubkey,
                timestamp = event.createdAt,
                nip65Entities = entities.toTypedArray()
            )
            dbSweepExcludingCache.addNip65Author(event.pubkey)
        }
    }

    private fun processReaction(event: Event) {
        if (event.content != "+") return
        if (otherIdsCache.contains(event.id)) return
        if (!verify(event)) return

        otherIdsCache.add(event.id)

        event.getReactedToId()?.let {
            scope.launch {
                database.reactionDao().like(eventId = it, pubkey = event.pubkey)
            }
        }
    }

    private fun verify(event: Event): Boolean {
        val isValid = event.verify()
        if (!isValid) {
            Log.w(TAG, "Invalid event kind ${event.kind} id ${event.id} ")
        }
        return isValid
    }

    private fun deserializeMetadata(json: String): Metadata? {
        try {
            return gson.fromJson(json, Metadata::class.java)
        } catch (t: Throwable) {
            Log.i(TAG, "Failed to deserialize $json")
        }
        return null
    }

    private fun getContactPubkeys(tags: List<Tag>): List<String> {
        return tags
            .asSequence()
            .filter { tag -> tag.size >= 2 && tag[0] == "p" }
            .map { tag -> tag[1] }
            .filter { KeyUtils.isValidHexKey(hexKey = it) }
            .distinct()
            .toList()
    }

    private fun insertEventRelay(eventId: String, relayUrl: String?) {
        if (relayUrl == null) return

        val wasNew = otherIdsCache.add(eventId + relayUrl)
        if (!wasNew) return

        scope.launch {
            database.eventRelayDao().insertOrIgnore(eventId = eventId, relayUrl = relayUrl)
        }
    }

    private fun getUpperTimeBoundary(): Long {
        return getCurrentTimeInSeconds() + TimeConstants.MINUTE_IN_SECONDS
    }

    private fun isFromFuture(event: Event): Boolean {
        if (event.createdAt > upperTimeBoundary) {
            upperTimeBoundary = getUpperTimeBoundary()
            return event.createdAt > upperTimeBoundary
        }
        return false
    }
}

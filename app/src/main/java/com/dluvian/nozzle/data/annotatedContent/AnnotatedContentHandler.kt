package com.dluvian.nozzle.data.annotatedContent

import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.dluvian.nozzle.data.nostr.utils.EncodingUtils.nostrUriToNostrId
import com.dluvian.nozzle.data.nostr.utils.EncodingUtils.note1ToHex
import com.dluvian.nozzle.data.nostr.utils.EncodingUtils.readNevent
import com.dluvian.nozzle.data.nostr.utils.MentionUtils
import com.dluvian.nozzle.data.nostr.utils.ShortenedNameUtils.getShortenedNevent
import com.dluvian.nozzle.data.nostr.utils.ShortenedNameUtils.getShortenedNote1
import com.dluvian.nozzle.data.nostr.utils.ShortenedNameUtils.getShortenedNprofile
import com.dluvian.nozzle.data.nostr.utils.ShortenedNameUtils.getShortenedNpub
import com.dluvian.nozzle.data.utils.AnnotatedStringUtils.pushAnnotatedString
import com.dluvian.nozzle.data.utils.AnnotatedStringUtils.pushStyledUrlAnnotation
import com.dluvian.nozzle.data.utils.HashtagUtils
import com.dluvian.nozzle.data.utils.UrlUtils
import com.dluvian.nozzle.model.Pubkey
import com.dluvian.nozzle.model.nostr.Nevent
import com.dluvian.nozzle.model.nostr.NeventNostrId
import com.dluvian.nozzle.model.nostr.NoteNostrId
import com.dluvian.nozzle.model.nostr.NprofileNostrId
import com.dluvian.nozzle.model.nostr.NpubNostrId
import com.dluvian.nozzle.ui.theme.HyperlinkBlue
import java.util.Collections

private const val TAG = "AnnotatedContentHandler"

class AnnotatedContentHandler : IAnnotatedContentHandler {
    private val NEVENT_TAG = "NEVENT"
    private val NOTE1_TAG = "NOTE1"
    private val NPROFILE_TAG = "NPROFILE"
    private val NPUB_TAG = "NPUB"
    private val HASHTAG = "HASHTAG"

    private val mentionAndHashtagStyle = SpanStyle(color = HyperlinkBlue)
    private val hyperlinkStyle = mentionAndHashtagStyle.copy(
        textDecoration = TextDecoration.Underline
    )

    private val cache: MutableMap<String, AnnotatedString> =
        Collections.synchronizedMap(mutableMapOf())

    override fun annotateContent(
        content: String,
        mentionedNamesByPubkey: Map<Pubkey, String>
    ): AnnotatedString {
        if (content.isEmpty()) return AnnotatedString("")
        val cached = cache[content]
        if (cached != null) return cached

        val urls = UrlUtils.extractUrls(content)
        val nostrUris = MentionUtils.extractNostrUris(content)
        val tokens = (urls + nostrUris).toMutableList()
        val hashtags = HashtagUtils.extractHashtags(content).filter { hashtag ->
            tokens.none { isOverlappingHashtag(hashtag.range, it.range) }
        }
        tokens.addAll(hashtags)

        if (tokens.isEmpty()) return AnnotatedString(text = content)
        tokens.sortBy { it.range.first }

        val editedContent = StringBuilder(content)
        var isCacheable = true
        val result = buildAnnotatedString {
            for (token in tokens) {
                val firstIndex = editedContent.indexOf(token.value)
                if (firstIndex > 0) {
                    append(editedContent.subSequence(0, firstIndex))
                    editedContent.delete(0, firstIndex)
                }
                if (urls.contains(token)) {
                    pushStyledUrlAnnotation(
                        url = token.value,
                        style = hyperlinkStyle
                    )
                } else if (hashtags.contains(token)) {
                    pushAnnotatedString(
                        tag = HASHTAG,
                        annotation = token.value,
                        style = mentionAndHashtagStyle,
                        text = token.value
                    )
                } else {
                    when (val nostrId = nostrUriToNostrId(token.value)) {
                        is NpubNostrId -> {
                            val mentionedName = mentionedNamesByPubkey[nostrId.pubkeyHex]
                            if (mentionedName.isNullOrBlank()) isCacheable = false
                            val name = "@" + (mentionedName
                                ?.ifBlank { getShortenedNpub(nostrId.npub) }
                                ?: getShortenedNpub(nostrId.npub)
                                ?: nostrId.npub)
                            pushAnnotatedString(
                                tag = NPUB_TAG,
                                annotation = nostrId.npub,
                                style = mentionAndHashtagStyle,
                                text = name
                            )
                        }

                        is NprofileNostrId -> {
                            val mentionedName = mentionedNamesByPubkey[nostrId.pubkeyHex]
                            if (mentionedName.isNullOrBlank()) isCacheable = false
                            val name = "@" + (mentionedName
                                ?.ifBlank { getShortenedNprofile(nostrId.nprofile) }
                                ?: getShortenedNprofile(nostrId.nprofile)
                                ?: nostrId.nprofile)
                            pushAnnotatedString(
                                tag = NPROFILE_TAG,
                                annotation = nostrId.nprofile,
                                style = mentionAndHashtagStyle,
                                text = name
                            )
                        }

                        is NoteNostrId -> {
                            pushAnnotatedString(
                                tag = NOTE1_TAG,
                                annotation = nostrId.note1,
                                style = mentionAndHashtagStyle,
                                text = getShortenedNote1(nostrId.note1) ?: nostrId.note1
                            )
                        }

                        is NeventNostrId -> {
                            pushAnnotatedString(
                                tag = NEVENT_TAG,
                                annotation = nostrId.nevent,
                                style = mentionAndHashtagStyle,
                                text = getShortenedNevent(nostrId.nevent) ?: nostrId.nevent
                            )
                        }

                        null -> {
                            Log.w(TAG, "Failed to identify ${token.value}")
                            append(token.value)
                        }
                    }
                }
                editedContent.delete(0, token.value.length)
            }
            append(editedContent)
        }
        if (isCacheable) cache[content] = result
        return result
    }

    @OptIn(ExperimentalTextApi::class)
    override fun extractMediaLinks(annotatedContent: AnnotatedString): List<String> {
        return annotatedContent.getUrlAnnotations(start = 0, end = annotatedContent.length)
            .map { it.item.url }
            .filter { url -> UrlUtils.mediaSuffixes.any { suffix -> url.endsWith(suffix) } }
    }

    override fun extractNevents(annotatedContent: AnnotatedString): List<Nevent> {
        val nevents = annotatedContent
            .getStringAnnotations(tag = NEVENT_TAG, start = 0, end = annotatedContent.length)
            .mapNotNull { readNevent(it.item)?.let { nevent -> Pair(it.start, nevent) } }
        val note1s = annotatedContent
            .getStringAnnotations(tag = NOTE1_TAG, start = 0, end = annotatedContent.length)
            .mapNotNull {
                note1ToHex(it.item)?.let { hex ->
                    Pair(
                        it.start,
                        Nevent(eventId = hex, relays = emptyList())
                    )
                }
            }

        return (nevents + note1s).sortedBy { it.first }.map { it.second }
    }

    private fun isOverlappingHashtag(hashtagRange: IntRange, otherRange: IntRange): Boolean {
        return hashtagRange.first < otherRange.first
                && hashtagRange.last > otherRange.first
    }
}

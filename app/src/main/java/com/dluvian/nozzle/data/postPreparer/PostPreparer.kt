package com.dluvian.nozzle.data.postPreparer

import com.dluvian.nozzle.data.nostr.utils.EncodingUtils
import com.dluvian.nozzle.data.nostr.utils.MentionUtils
import com.dluvian.nozzle.data.nostr.utils.MentionUtils.removeMentionCharAndNostrUri
import com.dluvian.nozzle.data.utils.HashtagUtils
import com.dluvian.nozzle.model.PostWithTagsAndMentions

class PostPreparer : IPostPreparer {
    override fun getCleanPostWithTagsAndMentions(content: String): PostWithTagsAndMentions {
        val strBuilder = StringBuilder(content.trim())
        val allMentions = MentionUtils.extractMentionedProfiles(content)
        allMentions
            .filter { it.value.startsWith(MentionUtils.MENTION_CHAR) }
            .filter { EncodingUtils.profileIdToNostrId(it.value.removePrefix(MentionUtils.MENTION_CHAR)) != null }
            .sortedByDescending { it.range.first }
            .forEach {
                strBuilder.replace(
                    it.range.first,
                    it.range.last + 1,
                    EncodingUtils.URI + it.value.removePrefix(MentionUtils.MENTION_CHAR)
                )
            }
        val finalContent = strBuilder.toString()
        return PostWithTagsAndMentions(
            content = finalContent,
            hashtags = HashtagUtils.extractHashtagValues(finalContent).distinct(),
            mentions = allMentions
                .mapNotNull { EncodingUtils.profileIdToNostrId(it.value.removeMentionCharAndNostrUri()) }
                .map { it.hex }
                .toList()
                .distinct()
        )
    }
}
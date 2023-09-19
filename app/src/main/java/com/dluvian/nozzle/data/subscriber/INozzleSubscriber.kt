package com.dluvian.nozzle.data.subscriber

import com.dluvian.nozzle.data.room.helper.BasePost
import com.dluvian.nozzle.model.helper.PubkeysAndAuthorPubkeys
import com.dluvian.nozzle.model.nostr.Nevent

interface INozzleSubscriber {
    suspend fun subscribeMentionedPosts(basePosts: Collection<BasePost>): List<Nevent>

    fun subscribeMentionedProfiles(
        basePosts: Collection<BasePost>
    ): PubkeysAndAuthorPubkeys

    suspend fun subscribeNewProfiles(pubkeys: Set<String>)
}
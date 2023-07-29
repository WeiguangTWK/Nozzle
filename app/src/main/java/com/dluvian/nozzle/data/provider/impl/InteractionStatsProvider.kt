package com.dluvian.nozzle.data.provider.impl

import com.dluvian.nozzle.data.provider.IInteractionStatsProvider
import com.dluvian.nozzle.data.provider.IPubkeyProvider
import com.dluvian.nozzle.data.room.dao.PostDao
import com.dluvian.nozzle.data.room.dao.ReactionDao
import com.dluvian.nozzle.data.utils.NORMAL_DEBOUNCE
import com.dluvian.nozzle.data.utils.SHORT_DEBOUNCE
import com.dluvian.nozzle.data.utils.firstThenDistinctDebounce
import com.dluvian.nozzle.model.InteractionStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class InteractionStatsProvider(
    private val pubkeyProvider: IPubkeyProvider,
    private val reactionDao: ReactionDao,
    private val postDao: PostDao,
) : IInteractionStatsProvider {
    override fun getStatsFlow(postIds: List<String>): Flow<InteractionStats> {
        val numOfRepliesFlow = postDao.getNumOfRepliesPerPostFlow(postIds)
            .firstThenDistinctDebounce(NORMAL_DEBOUNCE)
        val likedByMeFlow = reactionDao.listLikedByFlow(pubkeyProvider.getPubkey(), postIds)
            .firstThenDistinctDebounce(NORMAL_DEBOUNCE)

        return combine(
            numOfRepliesFlow,
            likedByMeFlow,
        ) { numOfReplies, likedByMe ->
            InteractionStats(
                numOfRepliesPerPost = numOfReplies,
                likedByMe = likedByMe,
            )
        }.firstThenDistinctDebounce(SHORT_DEBOUNCE)
    }
}

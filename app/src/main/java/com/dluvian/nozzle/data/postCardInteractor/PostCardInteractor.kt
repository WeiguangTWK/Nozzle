package com.dluvian.nozzle.data.postCardInteractor

import android.util.Log
import com.dluvian.nozzle.data.nostr.INostrService
import com.dluvian.nozzle.data.provider.IRelayProvider
import com.dluvian.nozzle.data.room.dao.ReactionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "PostCardInteractor"

class PostCardInteractor(
    private val nostrService: INostrService,
    private val relayProvider: IRelayProvider,
    private val reactionDao: ReactionDao,
) : IPostCardInteractor {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun like(postId: String, postPubkey: String) {
        Log.i(TAG, "Like $postId")
        val event = nostrService.sendLike(
            postId = postId,
            postPubkey = postPubkey,
            relays = relayProvider.getWriteRelays()
        )
        scope.launch {
            reactionDao.like(pubkey = event.pubkey, eventId = postId)
        }
    }
}

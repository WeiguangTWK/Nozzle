package com.dluvian.nozzle.model

data class PostWithMeta(
    val id: String,
    val replyToId: String?,
    val replyToName: String?,
    val replyToPubkey: String?,
    val replyRelayHint: String?,
    val pubkey: String,
    val createdAt: Long,
    val content: String,
    val mediaUrl: String?,
    val name: String,
    val pictureUrl: String,
    val isLikedByMe: Boolean,
    val isFollowedByMe: Boolean,
    val isOneself: Boolean,
    val trustScore: Float?,
    val numOfReplies: Int,
    val relays: List<String>,
) {
    fun getPostIds(): PostIds {
        return PostIds(id = id, replyToId = replyToId)
    }
}

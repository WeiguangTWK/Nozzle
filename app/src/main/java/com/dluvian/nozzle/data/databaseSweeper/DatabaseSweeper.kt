package com.dluvian.nozzle.data.databaseSweeper

import android.util.Log
import com.dluvian.nozzle.data.cache.IIdCache
import com.dluvian.nozzle.data.provider.IPubkeyProvider
import com.dluvian.nozzle.data.room.AppDatabase
import com.dluvian.nozzle.data.utils.getCurrentTimeInSeconds

private const val TAG = "DatabaseSweeper"

class DatabaseSweeper(
    private val keepPosts: Int,
    private val pubkeyProvider: IPubkeyProvider,
    private val dbSweepExcludingCache: IIdCache,
    private val database: AppDatabase
) : IDatabaseSweeper {
    override suspend fun sweep() {
        val postCount = database.postDao().countPosts()
        if (postCount < 2 * keepPosts) {
            Log.i(TAG, "Skip sweep. Only $postCount posts in db")
            return
        }
        val excludePostIds = dbSweepExcludingCache.getPostIds()
        val excludeProfiles = dbSweepExcludingCache.getPubkeys()
        Log.i(
            TAG,
            "Sweep database leaving $keepPosts, " +
                    "excluding ${excludePostIds.size} posts " +
                    "and ${excludeProfiles.size} profiles"
        )
        val start = System.currentTimeMillis()

        val deletePostCount = database.postDao().deleteAllExceptNewest(
            amountToKeep = keepPosts,
            exclude = excludePostIds,
            currentTimestamp = getCurrentTimeInSeconds()
        )
        if (deletePostCount == 0) return
        Log.i(TAG, "Deleted $deletePostCount posts")

        val deleteRelayCount = database.eventRelayDao().deleteOrphaned()
        Log.i(TAG, "Deleted $deleteRelayCount event relay entries")

        val deleteReactionCount = database.reactionDao().deleteOrphaned()
        Log.i(TAG, "Deleted $deleteReactionCount reactions")

        // TODO: pubkeyProvider not needed once user accounts are saved in roomDb
        val deleteProfileCount = database.profileDao().deleteOrphaned(
            exclude = excludeProfiles + pubkeyProvider.getPubkey()
        )
        Log.i(TAG, "Deleted $deleteProfileCount profiles")

        val deleteContactCount = database.contactDao().deleteOrphaned()
        Log.i(TAG, "Deleted $deleteContactCount contact entries")

        val deleteNip65Count = database.nip65Dao().deleteOrphaned()
        Log.i(TAG, "Deleted $deleteNip65Count nip65 entries")

        Log.i(TAG, "Execution time ${System.currentTimeMillis() - start}ms")
    }
}
package com.dluvian.nozzle.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dluvian.nozzle.data.room.dao.AccountDao
import com.dluvian.nozzle.data.room.dao.ContactDao
import com.dluvian.nozzle.data.room.dao.EventRelayDao
import com.dluvian.nozzle.data.room.dao.HashtagDao
import com.dluvian.nozzle.data.room.dao.MentionDao
import com.dluvian.nozzle.data.room.dao.Nip65Dao
import com.dluvian.nozzle.data.room.dao.PostDao
import com.dluvian.nozzle.data.room.dao.ProfileDao
import com.dluvian.nozzle.data.room.dao.ReactionDao
import com.dluvian.nozzle.data.room.entity.AccountEntity
import com.dluvian.nozzle.data.room.entity.ContactEntity
import com.dluvian.nozzle.data.room.entity.EventRelayEntity
import com.dluvian.nozzle.data.room.entity.HashtagEntity
import com.dluvian.nozzle.data.room.entity.MentionEntity
import com.dluvian.nozzle.data.room.entity.Nip65Entity
import com.dluvian.nozzle.data.room.entity.PostEntity
import com.dluvian.nozzle.data.room.entity.ProfileEntity
import com.dluvian.nozzle.data.room.entity.ReactionEntity

@Database(
    version = 19,
//    autoMigrations = [
//        AutoMigration (from = 19, to = 20)
//    ],
    entities = [
        ContactEntity::class,
        EventRelayEntity::class,
        Nip65Entity::class,
        PostEntity::class,
        ProfileEntity::class,
        ReactionEntity::class,
        HashtagEntity::class,
        MentionEntity::class,
        AccountEntity::class,
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun eventRelayDao(): EventRelayDao
    abstract fun nip65Dao(): Nip65Dao
    abstract fun profileDao(): ProfileDao
    abstract fun postDao(): PostDao
    abstract fun reactionDao(): ReactionDao
    abstract fun hashtagDao(): HashtagDao
    abstract fun mentionDao(): MentionDao
    abstract fun accountDao(): AccountDao
}

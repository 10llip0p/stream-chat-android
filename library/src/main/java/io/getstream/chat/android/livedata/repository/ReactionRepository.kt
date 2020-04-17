package io.getstream.chat.android.livedata.repository

import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.models.Reaction
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.utils.SyncStatus
import io.getstream.chat.android.livedata.dao.ReactionDao
import io.getstream.chat.android.livedata.entity.ReactionEntity
import java.security.InvalidParameterException

/**
 * We don't do any caching on reactions since usage is infrequent
 */
class ReactionRepository(var reactionDao: ReactionDao, var currentUser: User, var client: ChatClient) {

    suspend fun insertReaction(reaction: Reaction) {
        insert(listOf(ReactionEntity(reaction)))
    }

    suspend fun insertManyReactions(reactions: List<Reaction>) {
        val entities = reactions.map { ReactionEntity(it) }
        insert(entities)
    }

    suspend fun insert(reactionEntity: ReactionEntity) {
        insert(listOf(reactionEntity))
    }

    suspend fun insert(reactionEntities: List<ReactionEntity>) {
        for (reactionEntity in reactionEntities) {
            if (reactionEntity.messageId.isEmpty()) {
                throw InvalidParameterException("message id can't be empty when creating a reaction")
            }
            if (reactionEntity.type.isEmpty()) {
                throw InvalidParameterException("type can't be empty when creating a reaction")
            }
            if (reactionEntity.userId.isEmpty()) {
                throw InvalidParameterException("user id can't be empty when creating a reaction")
            }
        }

        reactionDao.insert(reactionEntities)
    }
    suspend fun select(messageId: String, userId: String, type: String): ReactionEntity? {
        return reactionDao.select(messageId, userId, type)
    }

    suspend fun selectSyncNeeded(): List<ReactionEntity> {
        return reactionDao.selectSyncNeeded()
    }

    suspend fun retryReactions(): List<ReactionEntity> {
        val userMap: Map<String, User> = mutableMapOf(currentUser.id to currentUser)

        val reactionEntities = selectSyncNeeded()
        for (reactionEntity in reactionEntities) {
            val reaction = reactionEntity.toReaction(userMap)
            reaction.user = null
            val result = if (reactionEntity.deletedAt != null) {
                client.deleteReaction(reaction.messageId, reaction.type).execute()
            } else {
                client.sendReaction(reaction).execute()
            }

            if (result.isSuccess) {
                reactionEntity.syncStatus = SyncStatus.SYNCED
                insert(reactionEntity)
            } else if (result.error().isPermanent()) {
                reactionEntity.syncStatus = SyncStatus.SYNC_FAILED
                insert(reactionEntity)
            }
        }
        return reactionEntities
    }


}

// TODO: move to llc
fun ChatError.isPermanent(): Boolean {
    return true
}

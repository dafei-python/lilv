package com.dushishiyi.lilv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SnapshotDao {

    @Query("SELECT * FROM snapshots ORDER BY fetchedAt DESC LIMIT 1")
    suspend fun getLatest(): SnapshotEntity?

    @Query("SELECT * FROM snapshots ORDER BY fetchedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SnapshotEntity>

    @Insert
    suspend fun insert(snapshot: SnapshotEntity)

    /** 只保留最近 :keep 份快照 */
    @Query(
        """
        DELETE FROM snapshots
        WHERE fetchedAt NOT IN (
            SELECT fetchedAt FROM snapshots ORDER BY fetchedAt DESC LIMIT :keep
        )
        """
    )
    suspend fun trim(keep: Int)
}

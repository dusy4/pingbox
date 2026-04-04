package com.dusy4.pingbox.data.local

import androidx.room.*

@Entity(tableName = "notification_history")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String?,
    val icon: String?,
    val tag: String?,
    val targetType: String?,
    val targetValue: String?,
    val receivedAt: Long = System.currentTimeMillis(),
    val openedAt: Long? = null
)

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val matchTag: String?,
    val targetType: String,
    val targetValue: String,
    val priority: Int = 0,
    val enabled: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification_history ORDER BY receivedAt DESC LIMIT 100")
    suspend fun getAll(): List<NotificationEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)
    
    @Query("DELETE FROM notification_history WHERE id = :id")
    suspend fun delete(id: String)
    
    @Query("DELETE FROM notification_history")
    suspend fun deleteAll()
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY priority ASC")
    suspend fun getAll(): List<RuleEntity>
    
    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY priority ASC")
    suspend fun getEnabled(): List<RuleEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RuleEntity)
    
    @Delete
    suspend fun delete(rule: RuleEntity)
    
    @Update
    suspend fun update(rule: RuleEntity)
    
    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [NotificationEntity::class, RuleEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TriggerDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun ruleDao(): RuleDao
}

package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY createdTimestamp DESC")
    fun getAllGoals(): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal)

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)
}

@Dao
interface RewardItemDao {
    @Query("SELECT * FROM reward_items ORDER BY id ASC")
    fun getAllRewards(): Flow<List<RewardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: RewardItem)

    @Update
    suspend fun updateReward(reward: RewardItem)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM wallet_card_profile WHERE id = 1")
    fun getProfileFlow(): Flow<WalletCardProfile?>

    @Query("SELECT * FROM wallet_card_profile WHERE id = 1")
    suspend fun getProfileDirect(): WalletCardProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: WalletCardProfile)
}

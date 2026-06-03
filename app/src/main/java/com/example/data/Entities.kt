package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val isCredit: Boolean,
    val category: String, // "Wallet", "UPI", "Gold", "Savings", "Reward"
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val category: String, // "Gaming", "Gadgets", "Education", "Fashion", "General"
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reward_items")
data class RewardItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brandName: String,
    val offerTitle: String,
    val discountAmount: Double,
    val couponCode: String,
    val isClaimed: Boolean = false,
    val category: String, // "Food", "Gaming", "Shopping", "Entertainment"
    val costPoints: Int
)

@Entity(tableName = "wallet_card_profile")
data class WalletCardProfile(
    @PrimaryKey val id: Int = 1, // Fixed ID for single user setup
    val kycStatus: String = "VERIFIED", // "VERIFIED", "PENDING", "NOT_STARTED"
    val walletBalance: Double = 4500.00,
    val rewardPoints: Int = 1250,
    val cardBlocked: Boolean = false,
    val cardPin: String = "1234",
    val cardNumber: String = "4312 9081 2134 5678",
    val cardCvv: String = "733",
    val cardExpiry: String = "09/30",
    val showBalance: Boolean = true
)

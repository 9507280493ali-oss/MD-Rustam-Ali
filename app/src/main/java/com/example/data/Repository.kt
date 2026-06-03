package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class TeenPayRepository(private val database: AppDatabase) {

    private val transactionDao = database.transactionDao()
    private val savingsGoalDao = database.savingsGoalDao()
    private val rewardItemDao = database.rewardItemDao()
    private val profileDao = database.profileDao()

    val transactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val savingsGoals: Flow<List<SavingsGoal>> = savingsGoalDao.getAllGoals()
    val rewards: Flow<List<RewardItem>> = rewardItemDao.getAllRewards()
    val profile: Flow<WalletCardProfile?> = profileDao.getProfileFlow()

    // Initialize database defaults if empty
    suspend fun initializeDatabaseIfEmpty() {
        val currentProfile = profileDao.getProfileDirect()
        if (currentProfile == null) {
            // Set up default active user profile
            val defaultProfile = WalletCardProfile(
                id = 1,
                kycStatus = "VERIFIED",
                walletBalance = 4850.50,
                rewardPoints = 850,
                cardBlocked = false,
                cardPin = "2580",
                cardNumber = "4815 1623 4268 9012",
                cardCvv = "911",
                cardExpiry = "12/31",
                showBalance = true
            )
            profileDao.insertOrUpdateProfile(defaultProfile)

            // Set up default transactions
            val defaultTransactions = listOf(
                Transaction(
                    title = "Send to Aditya (Rent/Snacks)",
                    amount = 250.00,
                    isCredit = false,
                    category = "UPI",
                    timestamp = System.currentTimeMillis() - 3600000 * 2, // 2 hours ago
                    note = "Snacks & Cold drinks party"
                ),
                Transaction(
                    title = "Pocket Money Deposit",
                    amount = 2000.00,
                    isCredit = true,
                    category = "Wallet",
                    timestamp = System.currentTimeMillis() - 3600000 * 24, // 1 day ago
                    note = "Monthly pocket allowance from Dad"
                ),
                Transaction(
                    title = "Zomato Food Order",
                    amount = 450.00,
                    isCredit = false,
                    category = "UPI",
                    timestamp = System.currentTimeMillis() - 3600000 * 36, // 1.5 days ago
                    note = "Cheese burst pizza"
                ),
                Transaction(
                    title = "DigiGold Cashback Reward",
                    amount = 25.00,
                    isCredit = true,
                    category = "Reward",
                    timestamp = System.currentTimeMillis() - 3600000 * 48, // 2 days ago
                    note = "Cashback scratch card win!"
                ),
                Transaction(
                    title = "Google Play Recharge Code",
                    amount = 150.00,
                    isCredit = false,
                    category = "Wallet",
                    timestamp = System.currentTimeMillis() - 3600000 * 72, // 3 days ago
                    note = "Subway Surfers Bundle"
                )
            )
            for (tx in defaultTransactions) {
                transactionDao.insertTransaction(tx)
            }

            // Set up default target goals
            val defaultGoals = listOf(
                SavingsGoal(
                    name = "PlayStation 5 Console",
                    targetAmount = 55000.00,
                    currentAmount = 18500.00,
                    category = "Gaming"
                ),
                SavingsGoal(
                    name = "Smart College Laptop",
                    targetAmount = 45000.00,
                    currentAmount = 27000.00,
                    category = "Education"
                ),
                SavingsGoal(
                    name = "Noise Noise-Canceling Earbuds",
                    targetAmount = 4999.00,
                    currentAmount = 3500.00,
                    category = "Gadgets"
                )
            )
            for (goal in defaultGoals) {
                savingsGoalDao.insertGoal(goal)
            }

            // Set up default rewards store options
            val defaultRewards = listOf(
                RewardItem(brandName = "Amazon Pay", offerTitle = "Flat ₹50 OFF Gift Card", discountAmount = 50.0, couponCode = "AMZN50TPY", category = "Shopping", costPoints = 150),
                RewardItem(brandName = "Google Play", offerTitle = "Extra ₹100 Redeemed Code", discountAmount = 100.0, couponCode = "GPLAY100T", category = "Gaming", costPoints = 250),
                RewardItem(brandName = "Zomato", offerTitle = "Get ₹150 OFF Food Delivery", discountAmount = 150.0, couponCode = "ZOMTEEN150", category = "Food", costPoints = 300),
                RewardItem(brandName = "BookMyShow", offerTitle = "Buy 1 Get 1 Movie Ticket", discountAmount = 200.0, couponCode = "BMSBOGOTP", category = "Entertainment", costPoints = 400),
                RewardItem(brandName = "Myntra", offerTitle = "Flat 15% OFF (₹300 Max)", discountAmount = 300.0, couponCode = "MYNTRA15TE", category = "Shopping", costPoints = 500)
            )
            for (r in defaultRewards) {
                rewardItemDao.insertReward(r)
            }
        }
    }

    // Perform transaction action
    suspend fun performTransaction(title: String, amount: Double, isCredit: Boolean, category: String, note: String? = null): Boolean {
        val currentProfile = profileDao.getProfileDirect() ?: return false
        
        if (!isCredit && currentProfile.walletBalance < amount) {
            return false // Insufficient funds
        }

        // Apply wallet balance change
        val newBalance = if (isCredit) {
            currentProfile.walletBalance + amount
        } else {
            currentProfile.walletBalance - amount
        }

        // Reward points logic (earn points when spending)
        val earnPoints = if (!isCredit) (amount / 10).toInt() else 0
        val newPoints = currentProfile.rewardPoints + earnPoints

        // Save transaction
        val tx = Transaction(
            title = title,
            amount = amount,
            isCredit = isCredit,
            category = category,
            note = note
        )
        transactionDao.insertTransaction(tx)

        // Save profile
        profileDao.insertOrUpdateProfile(
            currentProfile.copy(
                walletBalance = newBalance,
                rewardPoints = newPoints
            )
        )
        return true
    }

    // Toggle balance visibility
    suspend fun toggleShowBalance(): Boolean {
        val currentProfile = profileDao.getProfileDirect() ?: return false
        val updated = currentProfile.copy(showBalance = !currentProfile.showBalance)
        profileDao.insertOrUpdateProfile(updated)
        return updated.showBalance
    }

    // Toggle Card Block state
    suspend fun toggleCardBlocked(): Boolean {
        val currentProfile = profileDao.getProfileDirect() ?: return false
        val updated = currentProfile.copy(cardBlocked = !currentProfile.cardBlocked)
        profileDao.insertOrUpdateProfile(updated)
        return updated.cardBlocked
    }

    // Reset Card PIN
    suspend fun updateCardPin(newPin: String): Boolean {
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) return false
        val currentProfile = profileDao.getProfileDirect() ?: return false
        profileDao.insertOrUpdateProfile(currentProfile.copy(cardPin = newPin))
        return true
    }

    // Save funds into target goal
    suspend fun investInSavingsGoal(goalId: Int, amount: Double): Boolean {
        val currentProfile = profileDao.getProfileDirect() ?: return false
        if (currentProfile.walletBalance < amount) return false // Insufficient funds in wallet

        val allGoals = savingsGoalDao.getAllGoals().firstOrNull() ?: return false
        val targetGoal = allGoals.find { it.id == goalId } ?: return false

        // 1. Deduct from wallet & grant transaction point reward
        val newWalletBalance = currentProfile.walletBalance - amount
        val rewardPointsEarned = (amount / 20).toInt()

        // 2. Add to target goal
        val newCurrentGoalAmount = targetGoal.currentAmount + amount
        val updatedGoal = targetGoal.copy(currentAmount = newCurrentGoalAmount)

        // Save state in atomic sequence
        savingsGoalDao.updateGoal(updatedGoal)
        
        transactionDao.insertTransaction(
            Transaction(
                title = "Invested in '${targetGoal.name}'",
                amount = amount,
                isCredit = false,
                category = "Savings",
                note = "Saved for ${targetGoal.name}"
            )
        )

        profileDao.insertOrUpdateProfile(
            currentProfile.copy(
                walletBalance = newWalletBalance,
                rewardPoints = currentProfile.rewardPoints + rewardPointsEarned
            )
        )
        return true
    }

    // Create a new custom savings goal
    suspend fun createSavingsGoal(name: String, targetAmount: Double, category: String): Boolean {
        if (name.isBlank() || targetAmount <= 0) return false
        val goal = SavingsGoal(
            name = name,
            targetAmount = targetAmount,
            currentAmount = 0.0,
            category = category
        )
        savingsGoalDao.insertGoal(goal)
        return true
    }

    // Claim / Purchase reward item with loyalty points
    suspend fun claimRewardWithPoints(rewardId: Int): Boolean {
        val currentProfile = profileDao.getProfileDirect() ?: return false
        val allRewards = rewardItemDao.getAllRewards().firstOrNull() ?: return false
        val targetReward = allRewards.find { id -> id.id == rewardId } ?: return false

        if (targetReward.isClaimed) return false
        if (currentProfile.rewardPoints < targetReward.costPoints) return false

        // 1. Deduct points
        val newPoints = currentProfile.rewardPoints - targetReward.costPoints

        // 2. Mark reward as claimed
        val updatedReward = targetReward.copy(isClaimed = true)

        // Save state in atomic sequence
        rewardItemDao.updateReward(updatedReward)
        
        transactionDao.insertTransaction(
            Transaction(
                title = "Claimed ${targetReward.brandName} Voucher",
                amount = targetReward.discountAmount,
                isCredit = true, // Shows as earned value back
                category = "Reward",
                note = "Redeemed with ${targetReward.costPoints} Points"
            )
        )

        profileDao.insertOrUpdateProfile(
            currentProfile.copy(
                rewardPoints = newPoints
            )
        )
        return true
    }
    
    // Perform KYC flow
    suspend fun completeKycFlow(status: String): Boolean {
        val currentProfile = profileDao.getProfileDirect() ?: return false
        profileDao.insertOrUpdateProfile(currentProfile.copy(kycStatus = status))
        return true
    }
}

package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TeenPayViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TeenPayRepository(database)

    private val sharedPrefs = application.getSharedPreferences("teenpay_prefs", android.content.Context.MODE_PRIVATE)
    private val _isBiometricEnabled = MutableStateFlow(sharedPrefs.getBoolean("biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
        viewModelScope.launch {
            _uiEvents.emit(if (enabled) "Biometric Lock Security enabled!" else "Biometric Lock Security disabled.")
        }
    }

    private val _isPasswordLockEnabled = MutableStateFlow(sharedPrefs.getBoolean("password_lock_enabled", false))
    val isPasswordLockEnabled: StateFlow<Boolean> = _isPasswordLockEnabled.asStateFlow()

    private val _appPassword = MutableStateFlow(sharedPrefs.getString("app_password", "0000") ?: "0000")
    val appPassword: StateFlow<String> = _appPassword.asStateFlow()

    fun setPasswordLockEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("password_lock_enabled", enabled).apply()
        _isPasswordLockEnabled.value = enabled
        viewModelScope.launch {
            _uiEvents.emit(if (enabled) "Password Lock Security enabled!" else "Password Lock Security disabled.")
        }
    }

    fun setAppPassword(password: String) {
        sharedPrefs.edit().putString("app_password", password).apply()
        _appPassword.value = password
        viewModelScope.launch {
            _uiEvents.emit("App lock password changed successfully!")
        }
    }

    private val _isLowBalanceAlertEnabled = MutableStateFlow(sharedPrefs.getBoolean("low_balance_alert_enabled", true))
    val isLowBalanceAlertEnabled: StateFlow<Boolean> = _isLowBalanceAlertEnabled.asStateFlow()

    private val _cardDailyLimit = MutableStateFlow(sharedPrefs.getFloat("card_daily_limit", 10000f).toDouble())
    val cardDailyLimit: StateFlow<Double> = _cardDailyLimit.asStateFlow()

    private val _cardTxLimit = MutableStateFlow(sharedPrefs.getFloat("card_tx_limit", 5000f).toDouble())
    val cardTxLimit: StateFlow<Double> = _cardTxLimit.asStateFlow()

    fun setCardDailyLimit(limit: Double) {
        sharedPrefs.edit().putFloat("card_daily_limit", limit.toFloat()).apply()
        _cardDailyLimit.value = limit
        viewModelScope.launch {
            _uiEvents.emit("Daily Card Spend Limit set to ₹${limit.toInt()}")
        }
    }

    fun setCardTxLimit(limit: Double) {
        sharedPrefs.edit().putFloat("card_tx_limit", limit.toFloat()).apply()
        _cardTxLimit.value = limit
        viewModelScope.launch {
            _uiEvents.emit("Per-Transaction Card Limit set to ₹${limit.toInt()}")
        }
    }

    private val _lowBalanceThreshold = MutableStateFlow(sharedPrefs.getFloat("low_balance_threshold", 500f).toDouble())
    val lowBalanceThreshold: StateFlow<Double> = _lowBalanceThreshold.asStateFlow()

    fun setLowBalanceAlertEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("low_balance_alert_enabled", enabled).apply()
        _isLowBalanceAlertEnabled.value = enabled
        viewModelScope.launch {
            _uiEvents.emit("Low Balance Alert notification matches update: ${if (enabled) "Enabled" else "Disabled"}")
        }
    }

    fun setLowBalanceThreshold(threshold: Double) {
        sharedPrefs.edit().putFloat("low_balance_threshold", threshold.toFloat()).apply()
        _lowBalanceThreshold.value = threshold
        viewModelScope.launch {
            _uiEvents.emit("Low Balance Threshold successfully updated to ₹${threshold.toInt()}")
        }
    }

    val transactionsState: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savingsGoalsState: StateFlow<List<SavingsGoal>> = repository.savingsGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val rewardsState: StateFlow<List<RewardItem>> = repository.rewards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val profileState: StateFlow<WalletCardProfile?> = repository.profile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WalletCardProfile() // Default initial value to avoid initial blank frames
        )

    // Events flow for displaying beautiful SnackBar / Toast feedbacks
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfEmpty()
        }
    }

    // Interactive Core Actions
    fun depositFunds(amount: Double) {
        viewModelScope.launch {
            val success = repository.performTransaction(
                title = "Added Money to Wallet",
                amount = amount,
                isCredit = true,
                category = "Wallet",
                note = "Topped up wallet balance"
            )
            if (success) {
                _uiEvents.emit("₹${amount.toInt()} successfully added to your wallet!")
            } else {
                _uiEvents.emit("Failed to add money. Please try again.")
            }
        }
    }

    fun makeUpiPayment(toUser: String, amount: Double, note: String?) {
        viewModelScope.launch {
            val title = "Send to $toUser"
            val success = repository.performTransaction(
                title = title,
                amount = amount,
                isCredit = false,
                category = "UPI",
                note = note
            )
            if (success) {
                _uiEvents.emit("Transferred ₹${amount.toInt()} to $toUser!")
            } else {
                _uiEvents.emit("Oops! Insufficient wallet balance.")
            }
        }
    }

    fun scanAndPaySimulate(merchant: String, amount: Double) {
        viewModelScope.launch {
            val success = repository.performTransaction(
                title = "Paid to $merchant (QR)",
                amount = amount,
                isCredit = false,
                category = "UPI",
                note = "Scanned merchant QR"
            )
            if (success) {
                _uiEvents.emit("Successfully paid ₹${amount.toInt()} to $merchant!")
            } else {
                _uiEvents.emit("Payment failed! Check your wallet balance.")
            }
        }
    }

    fun toggleShowBalance() {
        viewModelScope.launch {
            repository.toggleShowBalance()
        }
    }

    fun showNotification(message: String) {
        viewModelScope.launch {
            _uiEvents.emit(message)
        }
    }

    fun toggleCardBlocked() {
        viewModelScope.launch {
            val isBlocked = repository.toggleCardBlocked()
            if (isBlocked) {
                _uiEvents.emit("TeenX Debit Card blocked successfully.")
            } else {
                _uiEvents.emit("TeenX Debit Card unblocked and active.")
            }
        }
    }

    fun resetCardPin(newPin: String) {
        viewModelScope.launch {
            val success = repository.updateCardPin(newPin)
            if (success) {
                _uiEvents.emit("TeenX Card PIN changed successfully!")
            } else {
                _uiEvents.emit("Invalid PIN format. Must be 4 numeric digits.")
            }
        }
    }

    fun addSavingsGoal(name: String, targetAmount: Double, category: String) {
        viewModelScope.launch {
            val success = repository.createSavingsGoal(name, targetAmount, category)
            if (success) {
                _uiEvents.emit("Savings goal '$name' successfully created!")
            } else {
                _uiEvents.emit("Failed to create savings goal. Enter proper name/target.")
            }
        }
    }

    fun investInGoal(goalId: Int, goalName: String, amount: Double) {
        viewModelScope.launch {
            val success = repository.investInSavingsGoal(goalId, amount)
            if (success) {
                _uiEvents.emit("Transferred ₹${amount.toInt()} from wallet into '$goalName'!")
            } else {
                _uiEvents.emit("Insufficient wallet balance to add money.")
            }
        }
    }

    fun claimVoucher(rewardId: Int, brandName: String, pointsCost: Int) {
        viewModelScope.launch {
            val success = repository.claimRewardWithPoints(rewardId)
            if (success) {
                _uiEvents.emit("Successfully claimed $brandName voucher!")
            } else {
                _uiEvents.emit("Not enough loyalty reward points! Need $pointsCost points.")
            }
        }
    }

    fun submitKycFlow(status: String) {
        viewModelScope.launch {
            val success = repository.completeKycFlow(status)
            if (success) {
                _uiEvents.emit("KYC verification status: $status")
            }
        }
    }

    fun buyDigiGold(amount: Double) {
        viewModelScope.launch {
            val success = repository.performTransaction(
                title = "Purchased 24K DigiGold",
                amount = amount,
                isCredit = false,
                category = "Gold",
                note = "Simulated gold purchase"
            )
            if (success) {
                _uiEvents.emit("Invested ₹${amount.toInt()} in 24K DigiGold successfully!")
            } else {
                _uiEvents.emit("Gold purchase failed. Check wallet balance.")
            }
        }
    }

    // Custom VM Factory block
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TeenPayViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TeenPayViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

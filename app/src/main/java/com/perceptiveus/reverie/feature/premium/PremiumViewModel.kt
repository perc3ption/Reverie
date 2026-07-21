package com.perceptiveus.reverie.feature.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.EntitlementRepository
import com.perceptiveus.reverie.core.entitlement.Entitlements
import com.perceptiveus.reverie.core.entitlement.PremiumProductInfo
import com.perceptiveus.reverie.core.entitlement.PurchaseCanceledException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PremiumViewModel(
    private val entitlementRepository: EntitlementRepository,
) : ViewModel() {

    val entitlements: StateFlow<Entitlements> = entitlementRepository.entitlements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Entitlements())

    val premiumProduct: StateFlow<PremiumProductInfo?> = entitlementRepository.premiumProduct
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _purchaseMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val purchaseMessages: SharedFlow<String> = _purchaseMessages.asSharedFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    fun purchasePremium(activity: Activity) {
        if (_isPurchasing.value) return
        viewModelScope.launch {
            _isPurchasing.value = true
            val result = entitlementRepository.purchasePremium(activity)
            _isPurchasing.value = false
            result.fold(
                onSuccess = {
                    _purchaseMessages.emit("Premium unlocked. Thank you!")
                },
                onFailure = { error ->
                    if (error is PurchaseCanceledException) return@fold
                    _purchaseMessages.emit(error.message ?: "Purchase failed.")
                },
            )
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            val result = entitlementRepository.restorePurchases()
            val restored = result.getOrDefault(false)
            _purchaseMessages.emit(
                when {
                    restored -> "Purchases restored."
                    result.isFailure -> result.exceptionOrNull()?.message
                        ?: "Could not restore purchases."
                    else -> "No purchases found to restore."
                },
            )
        }
    }
}

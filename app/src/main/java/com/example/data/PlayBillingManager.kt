package com.example.data

import android.content.Context
import android.util.Log
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Modern, fully bypassed offline mock service for Google Play Billing.
 * Keeps standard type signatures to preserve compile stability but operates entirely
 * offline to render the app secure, harmless, and cost-free.
 */
class PlayBillingManager(
    private val context: Context,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val TAG = "PlayBillingManager"

    // Backing flows for Reactive UI state matching
    private val _billingConnectionState = MutableStateFlow(false)
    val billingConnectionState: StateFlow<Boolean> = _billingConnectionState.asStateFlow()

    private val _productsDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productsDetails: StateFlow<List<ProductDetails>> = _productsDetails.asStateFlow()

    private val _purchasesState = MutableSharedFlow<List<Purchase>>(replay = 1)
    val purchasesState: SharedFlow<List<Purchase>> = _purchasesState.asSharedFlow()

    private val _billingError = MutableSharedFlow<String>()
    val billingError: SharedFlow<String> = _billingError.asSharedFlow()

    init {
        Log.i(TAG, "PlayBillingManager initialized in BYPASS mode. No connections will be opened.")
    }

    fun startBillingConnection() {
        Log.d(TAG, "Billing system bypassed. Offline mode active.")
        _billingConnectionState.value = false
    }

    fun queryAvailableProducts() {
        Log.d(TAG, "Product queries bypassed.")
    }

    fun launchBillingFlow(activity: android.app.Activity, productDetails: ProductDetails, selectedOfferToken: String? = null) {
        Log.w(TAG, "Billing flow launch requested but blocked. Payments are disabled.")
        externalScope.launch {
            _billingError.emit("Payments are disabled in this build.")
        }
    }

    fun restorePurchases(onRestoreCompleted: (List<Purchase>) -> Unit) {
        onRestoreCompleted(emptyList())
    }

    fun endConnection() {
        Log.d(TAG, "Billing connection ended safely.")
    }
}

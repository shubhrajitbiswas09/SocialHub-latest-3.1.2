package com.example.data

import android.content.Context
import android.util.Log
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
 * Clean mock stubs for Google Play Billing types to eliminate external compile-time dependency
 * and keep the app size incredibly lightweight and secure.
 */
class Purchase(val products: List<String>, val purchaseState: Int)
class ProductDetails(val name: String, val productId: String)

/**
 * Modern, offline-only stub manager for Play Billing.
 * Removes dependencies, protects the app against payment hijacking, and operates strictly in
 * secure offline sandboxed mode.
 */
class PlayBillingManager(
    private val context: Context,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val TAG = "PlayBillingManager"

    private val _billingConnectionState = MutableStateFlow(false)
    val billingConnectionState: StateFlow<Boolean> = _billingConnectionState.asStateFlow()

    private val _productsDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productsDetails: StateFlow<List<ProductDetails>> = _productsDetails.asStateFlow()

    private val _purchasesState = MutableSharedFlow<List<Purchase>>(replay = 1)
    val purchasesState: SharedFlow<List<Purchase>> = _purchasesState.asSharedFlow()

    private val _billingError = MutableSharedFlow<String>()
    val billingError: SharedFlow<String> = _billingError.asSharedFlow()

    init {
        Log.i(TAG, "PlayBillingManager initialized in pure, dependency-free offline mode.")
    }

    fun startBillingConnection() {
        Log.d(TAG, "Billing system fully bypassed.")
        _billingConnectionState.value = false
    }

    fun queryAvailableProducts() {
        Log.d(TAG, "Product queries bypassed.")
    }

    fun launchBillingFlow(activity: android.app.Activity, productDetails: ProductDetails, selectedOfferToken: String? = null) {
        Log.w(TAG, "Billing flow blocked. Payments are disabled.")
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

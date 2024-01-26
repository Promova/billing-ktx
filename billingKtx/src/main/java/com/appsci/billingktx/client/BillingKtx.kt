package com.appsci.billingktx.client

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.FeatureType
import com.android.billingclient.api.BillingConfig
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.GetBillingConfigParams
import com.android.billingclient.api.InAppMessageParams
import com.android.billingclient.api.InAppMessageResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.queryPurchasesAsync
import com.android.billingclient.api.querySkuDetails
import com.appsci.billingktx.connection.BillingKtxFactory
import com.appsci.billingktx.exception.BillingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface BillingKtx {

    fun connect(): Flow<BillingClient>

    suspend fun isFeatureSupported(@FeatureType feature: String): Result<Boolean>

    fun observeUpdates(): Flow<PurchasesUpdate>

    suspend fun getPurchases(@BillingClient.ProductType productType: String): Result<List<Purchase>>

    suspend fun getPurchaseHistory(@BillingClient.ProductType productType: String): Result<List<PurchaseHistoryRecord>>

    /**
     * do not mix subs and inapp types in the same params object
     */
    suspend fun getProductDetails(params: QueryProductDetailsParams): Result<List<ProductDetails>>

    suspend fun launchFlow(activity: Activity, params: BillingFlowParams): Result<Unit>

    suspend fun showInappMessages(
        activity: Activity,
        params: InAppMessageParams,
    ): Result<InAppMessageResult>

    suspend fun consumeProduct(params: ConsumeParams): Result<Unit>

    suspend fun acknowledge(params: AcknowledgePurchaseParams): Result<Unit>

    suspend fun getBillingConfig(): Result<BillingConfig>

    @Deprecated("use getProductDetails instead")
    suspend fun getSkuDetails(params: SkuDetailsParams): Result<List<SkuDetails>>
}

class BillingKtxImpl(
    billingFactory: BillingKtxFactory,
) : BillingKtx {

    private val scope = CoroutineScope(SupervisorJob())

    private val updatesFlow = MutableSharedFlow<PurchasesUpdate>()

    private val updatedListener = PurchasesUpdatedListener { result, purchases ->
        val event = when (val responseCode = result.responseCode) {
            BillingClient.BillingResponseCode.OK -> PurchasesUpdate.Success(
                responseCode,
                purchases.orEmpty()
            )

            BillingClient.BillingResponseCode.USER_CANCELED -> PurchasesUpdate.Canceled(
                responseCode,
                purchases.orEmpty()
            )

            else -> PurchasesUpdate.Failed(responseCode, purchases.orEmpty())
        }
        scope.launch {
            updatesFlow.emit(event)
        }
    }

    private val connectionFlow = billingFactory
        .createBillingClientFlow(updatedListener)

    override fun connect(): Flow<BillingClient> {
        return connectionFlow
    }

    override suspend fun isFeatureSupported(@FeatureType feature: String): Result<Boolean> {
        return runCatchingOnBilling {
            val result = it.isFeatureSupported(feature)
            result.responseCode == BillingClient.BillingResponseCode.OK
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUpdates(): Flow<PurchasesUpdate> {
        return connectionFlow.flatMapLatest {
            updatesFlow
        }
    }

    override suspend fun getPurchases(@BillingClient.ProductType productType: String): Result<List<Purchase>> {
        return getBoughtItems(productType)
    }

    override suspend fun getPurchaseHistory(@BillingClient.ProductType productType: String): Result<List<PurchaseHistoryRecord>> {
        return getHistory(productType)
    }

    @Deprecated("use getProductDetails instead")
    override suspend fun getSkuDetails(params: SkuDetailsParams): Result<List<SkuDetails>> {
        return runCatchingOnBilling { client ->
            val detailsResult = client.querySkuDetails(params)
            val billingResult = detailsResult.billingResult
            val skuDetailsList = detailsResult.skuDetailsList
            val responseCode = billingResult.responseCode
            if (isSuccess(responseCode)) {
                skuDetailsList.orEmpty()
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }

    override suspend fun getProductDetails(params: QueryProductDetailsParams): Result<List<ProductDetails>> {
        return runCatchingOnBilling { client ->
            val detailsResult = client.queryProductDetails(params)
            val billingResult = detailsResult.billingResult
            val productDetails = detailsResult.productDetailsList
            val responseCode = billingResult.responseCode
            if (isSuccess(responseCode)) {
                productDetails.orEmpty()
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }

    override suspend fun launchFlow(activity: Activity, params: BillingFlowParams): Result<Unit> {
        return runCatchingOnBilling {
            val billingResult = withContext(Dispatchers.Main) {
                it.launchBillingFlow(activity, params)
            }
            if (isSuccess(billingResult.responseCode)) {
                Unit
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }

    override suspend fun showInappMessages(
        activity: Activity,
        params: InAppMessageParams,
    ): Result<InAppMessageResult> {
        return runCatchingOnBilling { client ->
            suspendCoroutine {
                client.showInAppMessages(activity, params) { result: InAppMessageResult ->
                    val responseCode = result.responseCode
                    if (isSuccess(responseCode)) {
                        it.resume(result)
                    } else {
                        it.resumeWithException(
                            BillingException.fromResponseCode(responseCode)
                        )
                    }
                }
            }
        }
    }

    override suspend fun consumeProduct(params: ConsumeParams): Result<Unit> {
        return runCatchingOnBilling { client ->
            val consumePurchase = client.consumePurchase(params)
            val billingResult = consumePurchase.billingResult
            val responseCode = billingResult.responseCode
            if (isSuccess(responseCode)) {
                Unit
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }

    override suspend fun acknowledge(params: AcknowledgePurchaseParams): Result<Unit> {
        return runCatchingOnBilling { client ->
            val billingResult = client.acknowledgePurchase(params)
            val responseCode = billingResult.responseCode
            if (isSuccess(responseCode)) {
                Unit
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }

    override suspend fun getBillingConfig(): Result<BillingConfig> {
        return runCatchingOnBilling { client ->
            val params = GetBillingConfigParams
                .newBuilder()
                .build()
            suspendCoroutine {
                client.getBillingConfigAsync(params) { billingResult, config ->
                    if (isSuccess(billingResult.responseCode) && config != null) {
                        it.resume(config)
                    } else {
                        it.resumeWithException(BillingException.fromResult(billingResult))
                    }
                }
            }
        }
    }

    private suspend fun getBoughtItems(@BillingClient.ProductType type: String): Result<List<Purchase>> {
        return runCatchingOnBilling {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(type)
                .build()
            val purchasesResult = it.queryPurchasesAsync(params)
            val billingResult = purchasesResult.billingResult
            val purchasesList = purchasesResult.purchasesList

            if (isSuccess(billingResult.responseCode)) {
                purchasesList
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }

    private suspend fun getHistory(@BillingClient.ProductType type: String): Result<List<PurchaseHistoryRecord>> {
        return runCatchingOnBilling { client ->
            val params = QueryPurchaseHistoryParams.newBuilder()
                .setProductType(type)
                .build()
            val historyResult = client.queryPurchaseHistory(params)
            val billingResult = historyResult.billingResult
            val responseCode = billingResult.responseCode
            val list = historyResult.purchaseHistoryRecordList
            if (isSuccess(responseCode)) {
                list.orEmpty()
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }

    private suspend fun <R> runCatchingOnBilling(
        block: suspend (BillingClient) -> R,
    ): Result<R> {
        return runCatching {
            withConnectedClient {
                block(it)
            }
        }
    }

    private suspend fun <R> withConnectedClient(
        block: suspend (BillingClient) -> R,
    ): R {
        return connectionFlow.map {
            block(it)
        }.first()
    }

    private fun isSuccess(@BillingClient.BillingResponseCode responseCode: Int): Boolean {
        return responseCode == BillingClient.BillingResponseCode.OK
    }
}

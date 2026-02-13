package com.appsci.billingktx.client.repository

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.FeatureType
import com.android.billingclient.api.BillingConfig
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.GetBillingConfigParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.appsci.billingktx.client.connection.BillingConnectionImpl
import com.appsci.billingktx.exception.BillingException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BillingRepositoryImpl(
    private val connection: BillingConnectionImpl,
) : BillingRepository {

    override suspend fun isFeatureSupported(@FeatureType feature: String): Boolean {
        return connection.withConnectedClient {
            val result = it.isFeatureSupported(feature)
            result.responseCode == BillingClient.BillingResponseCode.OK
        }
    }

    override suspend fun getPurchases(@BillingClient.ProductType productType: String): List<Purchase> {
        return connection.withConnectedClient {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(productType)
                .build()
            val purchasesResult = it.queryPurchasesAsync(params)
            val billingResult = purchasesResult.billingResult
            val purchasesList = purchasesResult.purchasesList

            if (connection.isSuccess(billingResult.responseCode)) {
                purchasesList
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }

    override suspend fun getProductDetails(params: QueryProductDetailsParams): List<ProductDetails> {
        return connection.withConnectedClient { client ->
            val detailsResult = client.queryProductDetails(params)
            val billingResult = detailsResult.billingResult
            val productDetails = detailsResult.productDetailsList
            val responseCode = billingResult.responseCode
            if (connection.isSuccess(responseCode)) {
                productDetails.orEmpty()
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }

    override suspend fun getBillingConfig(): BillingConfig {
        return connection.withConnectedClient { client ->
            val params = GetBillingConfigParams
                .newBuilder()
                .build()
            suspendCancellableCoroutine {
                client.getBillingConfigAsync(params) { billingResult, config ->
                    if (connection.isSuccess(billingResult.responseCode) && config != null) {
                        it.resume(config)
                    } else {
                        it.resumeWithException(BillingException.fromResult(billingResult))
                    }
                }
            }
        }
    }

    override suspend fun consumeProduct(params: ConsumeParams) {
        return connection.withConnectedClient { client ->
            val consumePurchase = client.consumePurchase(params)
            val billingResult = consumePurchase.billingResult
            val responseCode = billingResult.responseCode
            if (connection.isSuccess(responseCode)) {
                Unit
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }

    override suspend fun acknowledge(params: AcknowledgePurchaseParams) {
        return connection.withConnectedClient { client ->
            val billingResult = client.acknowledgePurchase(params)
            val responseCode = billingResult.responseCode
            if (connection.isSuccess(responseCode)) {
                Unit
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }
}

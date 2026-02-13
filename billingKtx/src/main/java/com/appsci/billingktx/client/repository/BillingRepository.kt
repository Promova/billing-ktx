package com.appsci.billingktx.client.repository

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.FeatureType
import com.android.billingclient.api.BillingConfig
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams

interface BillingRepository {

    suspend fun isFeatureSupported(@FeatureType feature: String): Boolean

    suspend fun getPurchases(@BillingClient.ProductType productType: String): List<Purchase>

    /**
     * do not mix subs and inapp types in the same params object
     */
    suspend fun getProductDetails(params: QueryProductDetailsParams): List<ProductDetails>

    suspend fun getBillingConfig(): BillingConfig

    suspend fun consumeProduct(params: ConsumeParams)

    suspend fun acknowledge(params: AcknowledgePurchaseParams)
}

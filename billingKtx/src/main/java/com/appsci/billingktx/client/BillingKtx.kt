package com.appsci.billingktx.client

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.FeatureType
import com.android.billingclient.api.BillingConfig
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.InAppMessageParams
import com.android.billingclient.api.InAppMessageResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.appsci.billingktx.client.connection.BillingConnection
import com.appsci.billingktx.client.connection.BillingConnectionImpl
import com.appsci.billingktx.client.repository.BillingApi
import com.appsci.billingktx.client.ui.BillingFlowLauncher
import com.appsci.billingktx.connection.BillingKtxFactory
import kotlinx.coroutines.flow.Flow

class BillingKtx(
    context: Context,
    enableOneTimeProducts: Boolean = false,
    enablePrepaidPlans: Boolean = false,
    enableAutoServiceReconnection: Boolean = false,
) : BillingConnection {

    private val connection = BillingConnectionImpl(
        BillingKtxFactory(
            context = context,
            enableOneTimeProducts = enableOneTimeProducts,
            enablePrepaidPlans = enablePrepaidPlans,
            enableAutoServiceReconnection = enableAutoServiceReconnection,
        ),
    )

    private val api = BillingApi(connection)
    private val flowLauncher = BillingFlowLauncher(connection)

    override fun connect(): Flow<BillingClient> = connection.connect()

    override fun observeUpdates(): Flow<PurchasesUpdate> = connection.observeUpdates()

    suspend fun isFeatureSupported(@FeatureType feature: String): Boolean =
        api.isFeatureSupported(feature)

    suspend fun getPurchases(@BillingClient.ProductType productType: String): List<Purchase> =
        api.getPurchases(productType)

    /**
     * Do not mix subs and inapp types in the same params object.
     */
    suspend fun getProductDetails(params: QueryProductDetailsParams): List<ProductDetails> =
        api.getProductDetails(params)

    suspend fun getBillingConfig(): BillingConfig =
        api.getBillingConfig()

    suspend fun consumeProduct(params: ConsumeParams) =
        api.consumeProduct(params)

    suspend fun acknowledge(params: AcknowledgePurchaseParams) =
        api.acknowledge(params)

    suspend fun launchFlow(activity: Activity, params: BillingFlowParams) =
        flowLauncher.launchFlow(activity, params)

    suspend fun showInappMessages(
        activity: Activity,
        params: InAppMessageParams,
    ): InAppMessageResult = flowLauncher.showInappMessages(activity, params)
}

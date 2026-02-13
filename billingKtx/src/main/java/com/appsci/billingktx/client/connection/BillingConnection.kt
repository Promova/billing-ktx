package com.appsci.billingktx.client.connection

import com.android.billingclient.api.BillingClient
import com.appsci.billingktx.client.PurchasesUpdate
import kotlinx.coroutines.flow.Flow

interface BillingConnection {

    fun connect(): Flow<BillingClient>

    fun observeUpdates(): Flow<PurchasesUpdate>
}

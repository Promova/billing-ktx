package com.appsci.billingktx.client.connection

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener
import com.appsci.billingktx.client.PurchasesUpdate
import com.appsci.billingktx.connection.BillingKtxFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class BillingConnectionImpl(
    billingFactory: BillingKtxFactory,
    scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : BillingConnection {

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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUpdates(): Flow<PurchasesUpdate> {
        return connectionFlow.flatMapLatest {
            updatesFlow
        }
    }

    internal suspend fun <R> withConnectedClient(
        block: suspend (BillingClient) -> R,
    ): R {
        return connectionFlow.map {
            block(it)
        }.first()
    }

}

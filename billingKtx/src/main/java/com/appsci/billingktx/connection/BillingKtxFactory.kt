package com.appsci.billingktx.connection

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.appsci.billingktx.exception.BillingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import timber.log.Timber

class BillingKtxFactory(
    private val context: Context,
    private val transform: (Flow<BillingClient>) -> Flow<BillingClient> = DefaultTransform(
        sharingScope = CoroutineScope(SupervisorJob()),
    ),
) {

    fun createBillingClientFlow(listener: PurchasesUpdatedListener): Flow<BillingClient> {
        return transform(
            createClientFlow(listener)
        )
    }

    private fun createClientFlow(
        listener: PurchasesUpdatedListener,
    ): Flow<BillingClient> {
        val flow: Flow<BillingClient> = callbackFlow {
            val billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(listener)
                .build()
            Timber.d("createClientFlow callbackFlow ${billingClient.hashCode()}")
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingServiceDisconnected() {
                        Timber.d("onBillingServiceDisconnected")
                        close(Exception("onBillingServiceDisconnected"))
                    }

                    override fun onBillingSetupFinished(result: BillingResult) {
                        val responseCode = result.responseCode
                        if (responseCode == BillingClient.BillingResponseCode.OK) {
                            trySendBlocking(billingClient)
                        } else {
                            Timber.e("onBillingSetupFinished response $responseCode")
                            close(BillingException.fromResult(result))
                        }
                    }
                }
            )
            awaitClose {
                if (billingClient.isReady) {
                    Timber.d("endConnection")
                    billingClient.endConnection()
                }
            }
        }
        return flow
    }
}

class DefaultTransform<T>(
    private val sharingScope: CoroutineScope,
    private val timeOut: Long = 3_000,
) : (Flow<T>) -> Flow<T> {
    override fun invoke(p1: Flow<T>): Flow<T> {
        return p1.retryWhen(retryWhen())
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
            .shareIn(
                scope = sharingScope,
                started = SharingStarted.WhileSubscribed(
                    /**
                     * unsubscribe from upstream when there are no subscribers after timeOut
                     */
                    stopTimeoutMillis = timeOut,
                    /**
                     * clear cached client immediately after it is closed
                     */
                    replayExpirationMillis = 0,
                ),
                replay = 1,
            )
            .map {
                it.getOrThrow()
            }
    }

}

private fun <T> retryWhen(): suspend FlowCollector<T>.(cause: Throwable, attempt: Long) -> Boolean =
    { _, attempt ->
        val shouldRetry = attempt < 3
        if (shouldRetry) {
            delay(500)
        }
        shouldRetry
    }

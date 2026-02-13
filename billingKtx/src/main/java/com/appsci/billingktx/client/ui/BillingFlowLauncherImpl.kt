package com.appsci.billingktx.client.ui

import android.app.Activity
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.InAppMessageParams
import com.android.billingclient.api.InAppMessageResult
import com.appsci.billingktx.client.connection.BillingConnectionImpl
import com.appsci.billingktx.exception.BillingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BillingFlowLauncherImpl(
    private val connection: BillingConnectionImpl,
) : BillingFlowLauncher {

    override suspend fun launchFlow(activity: Activity, params: BillingFlowParams) {
        return connection.withConnectedClient {
            val billingResult = withContext(Dispatchers.Main) {
                it.launchBillingFlow(activity, params)
            }
            if (connection.isSuccess(billingResult.responseCode)) {
                Unit
            } else {
                throw BillingException.fromResult(billingResult)
            }
        }
    }

    override suspend fun showInappMessages(
        activity: Activity,
        params: InAppMessageParams,
    ): InAppMessageResult {
        return connection.withConnectedClient { client ->
            suspendCancellableCoroutine {
                client.showInAppMessages(activity, params) { result: InAppMessageResult ->
                    val responseCode = result.responseCode
                    if (connection.isSuccess(responseCode)) {
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
}

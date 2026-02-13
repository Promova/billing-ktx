package com.appsci.billingktx.client.ui

import android.app.Activity
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.InAppMessageParams
import com.android.billingclient.api.InAppMessageResult

interface BillingFlowLauncher {

    suspend fun launchFlow(activity: Activity, params: BillingFlowParams)

    suspend fun showInappMessages(
        activity: Activity,
        params: InAppMessageParams,
    ): InAppMessageResult
}

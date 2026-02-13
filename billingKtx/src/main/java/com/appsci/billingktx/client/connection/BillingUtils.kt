package com.appsci.billingktx.client.connection

import com.android.billingclient.api.BillingClient

internal fun isSuccess(@BillingClient.BillingResponseCode responseCode: Int): Boolean {
    return responseCode == BillingClient.BillingResponseCode.OK
}

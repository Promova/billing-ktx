package com.appsci.billingktx.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.appsci.billingktx.client.BillingKtx
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

fun BillingKtx.keepConnection(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
) {
    val billingKtx = this
    with(lifecycleOwner) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(state) {
                billingKtx.connect()
                    .catch { Timber.e(it) }
                    .collect()
            }
        }
    }
}

package com.appsci.billingktx.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.QueryProductDetailsParams
import com.appsci.billingktx.client.BillingKtx
import com.appsci.billingktx.client.BillingKtxImpl
import com.appsci.billingktx.connection.BillingKtxFactory
import com.appsci.billingktx.lifecycle.keepConnection
import com.appsci.billingktx.sample.theme.BillingKtxTheme
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private lateinit var billingKtx: BillingKtx

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        billingKtx = BillingKtxImpl(BillingKtxFactory(this))
        billingKtx.keepConnection(this)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                billingKtx.observeUpdates()
                    .collect {
                        Timber.d("observeUpdates $it")
                    }
            }
        }

        val loadPurchasesClick = {
            lifecycleScope.launch {
                val products: Result<List<Purchase>> =
                    billingKtx.getPurchases(BillingClient.ProductType.SUBS)
                        .onSuccess {
                            Timber.d("getPurchases $it")
                        }.onFailure {
                            Timber.e(it, "getPurchases")
                        }
                val subs: Result<List<Purchase>> =
                    billingKtx.getPurchases(BillingClient.ProductType.INAPP)
                        .onSuccess {
                            Timber.d("getPurchases $it")
                        }.onFailure {
                            Timber.e(it, "getPurchases")
                        }
            }
            Unit
        }

        val loadHistoryClick = {
            lifecycleScope.launch {
                val products: Result<List<PurchaseHistoryRecord>> =
                    billingKtx.getPurchaseHistory(BillingClient.ProductType.SUBS)
                        .onFailure {
                            Timber.e(it, "getPurchaseHistory")
                        }.onSuccess {
                            Timber.d("getPurchaseHistory $it")
                        }
                val subs: Result<List<PurchaseHistoryRecord>> =
                    billingKtx.getPurchaseHistory(BillingClient.ProductType.INAPP)
                        .onFailure {
                            Timber.e(it, "getPurchaseHistory")
                        }.onSuccess {
                            Timber.d("getPurchaseHistory $it")
                        }
            }
            Unit
        }
        val launchFlowClick = {
            lifecycleScope.launch {
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .setProductId("sku1")
                        .build()
                )
                val productDetailsList = billingKtx.getProductDetails(
                    QueryProductDetailsParams.newBuilder()
                        .setProductList(
                            productList,
                        ).build()
                ).onFailure {
                    Timber.e(it, "getProductDetails")
                }.onSuccess {
                    Timber.d("getProductDetails $it")
                }.getOrNull()
                val details = productDetailsList?.getOrNull(0)
                details?.let {
                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams
                                    .newBuilder()
                                    .setProductDetails(details)
                                    .setOfferToken("offerToken")
                                    .build()
                            )
                        )
                        .build()
                    billingKtx.launchFlow(
                        activity = this@MainActivity,
                        params = flowParams,
                    )
                }
            }
            Unit
        }
        setContent {
            BillingKtxTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        OutlinedButton(
                            onClick = loadPurchasesClick,
                        ) {
                            Text(text = "Load purchases")
                        }
                        OutlinedButton(
                            onClick = loadHistoryClick,
                        ) {
                            Text(text = "Load history")
                        }
                        OutlinedButton(
                            onClick = launchFlowClick,
                        ) {
                            Text(text = "Launch flow")
                        }
                    }
                }
            }
        }
    }
}

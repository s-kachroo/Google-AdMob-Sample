package com.example.admobnativeadspoc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.admobnativeadspoc.composables.NativeAdBodyView
import com.example.admobnativeadspoc.composables.NativeAdCallToActionView
import com.example.admobnativeadspoc.composables.NativeAdChoicesView
import com.example.admobnativeadspoc.composables.NativeAdHeadlineView
import com.example.admobnativeadspoc.composables.NativeAdIconView
import com.example.admobnativeadspoc.composables.NativeAdMediaView
import com.example.admobnativeadspoc.composables.NativeAdPriceView
import com.example.admobnativeadspoc.composables.NativeAdStarRatingView
import com.example.admobnativeadspoc.composables.NativeAdState
import com.example.admobnativeadspoc.composables.NativeAdStoreView
import com.example.admobnativeadspoc.composables.NativeAdView
import com.example.admobnativeadspoc.ui.theme.AdMobNativeAdsPOCTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd


class NativeComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdMobNativeAdsPOCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    NativeLayoutScreen()
                }
            }
        }
    }

    @Preview
    @Composable
    fun NativeLayoutScreenPreview() {
        AdMobNativeAdsPOCTheme {
            Surface(
                modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
            ) {
                NativeLayoutScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
    @Composable
    fun NativeLayoutScreen() {
        // Cache the mutable state for our notification bar.
        val context = LocalContext.current
        var messageText by remember { mutableStateOf("Native ad is not loaded.") }
        var messageColor by remember { mutableStateOf(Color.Yellow) }

        // Cache the native ad so that we can apply it to our view elements.
        val nativeAd = remember { mutableStateOf<NativeAd?>(null) }

        // Construct a native state to configure the NativeComposable.
        val nativeState = NativeAdState(
            adUnitId = AD_UNIT_ID,
            adRequest = AdRequest.Builder().build(),
            onAdLoaded = {
                messageColor = Color.Green
                messageText = "Native ad is loaded."
                Log.i(TAG, messageText)
            },
            onAdFailedToLoad = { error: LoadAdError ->
                messageColor = Color.Red
                messageText = "Native ad failed to load with error: ${error.message}"
                Log.e(TAG, messageText)
            },
            onAdImpression = {
                Log.i(TAG, "Native ad impression")
            },
            onAdClicked = {
                Log.i(TAG, "Native ad clicked")
            },
            onAdOpened = {
                Log.i(TAG, "Native ad opened")
            },
            onAdClosed = {
                Log.i(TAG, "Native ad closed.")
            },
        )

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            content = {
                // render title
                TopAppBar(
                    title = { Text(text = "Native Compose") },
                    navigationIcon = {
                        IconButton(onClick = {
                            val intent = Intent(context, MainActivity::class.java)
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                )

                // render ad status box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(messageColor)
                ) {
                    Text(text = messageText, style = MaterialTheme.typography.bodyLarge)
                }

                // native ad composable box
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .wrapContentHeight(Alignment.Top)
                ) {
                    NativeAdView(nativeAdState = nativeState, nativeAdReference = nativeAd) {
                        Column(
                            Modifier
                                .align(Alignment.TopStart)
                                .wrapContentHeight(Alignment.Top)
                        ) {
                            // native ad composable components
                            NativeAdChoicesView()

                            Row {
                                NativeAdIconView(Modifier.padding(5.dp)) {
                                    nativeAd.value?.icon?.let {
                                        nativeAd.value?.icon?.drawable?.toBitmap()?.let { it1 ->
                                            Image(bitmap = it1.asImageBitmap(), "Icon")
                                        }
                                    }
                                }

                                Column {
                                    NativeAdHeadlineView {
                                        nativeAd.value?.headline?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.headlineLarge
                                            )
                                        }
                                    }

                                    NativeAdStarRatingView {
                                        nativeAd.value?.starRating?.let {
                                            Text(
                                                text = "Rated $it",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }

                            NativeAdBodyView(
                                Modifier
                                    .padding(1.dp)
                            ) {
                                nativeAd.value?.body?.let {
                                    Text(text = it)
                                }
                            }

                            NativeAdMediaView(
                                Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .fillMaxHeight()
                            )

                            Row(
                                Modifier
                                    .align(Alignment.End)
                                    .padding(5.dp)
                            ) {
                                NativeAdPriceView(
                                    Modifier
                                        .padding(5.dp)
                                        .align(Alignment.CenterVertically)
                                ) {
                                    nativeAd.value?.price?.let {
                                        Text(text = it)
                                    }
                                }

                                NativeAdStoreView(
                                    Modifier
                                        .padding(5.dp)
                                        .align(Alignment.CenterVertically)
                                ) {
                                    nativeAd.value?.store?.let {
                                        Text(text = it)
                                    }
                                }

                                NativeAdCallToActionView(
                                    Modifier.padding(5.dp)
                                ) {
                                    nativeAd.value?.callToAction?.let {
                                        Button(onClick = {
                                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                        }) {
                                            Text(
                                                text = it
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    companion object {
        const val TAG = "NativeComposeActivity"
        const val AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    }
}
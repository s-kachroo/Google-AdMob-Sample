package com.example.admobnativeadspoc.composables

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.AdChoicesView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * A CompositionLocal that can provide a `NativeAdView` to ad attributes such as `NativeHeadline`.
 */
internal val LocalNativeAdView = staticCompositionLocalOf<NativeAdView?> { null }

/**
 * This is the Compose wrapper for a NativeAdView.
 *
 * @param nativeAdState The NativeAdState object containing ad configuration.
 * @param nativeAdReference Mutable state for referencing the native ad between elements.
 * @param modifier modify the native ad view container.
 * @param content A composable function that defines the rest of the native ad view's elements.
 */
@Composable
fun NativeAdView(
    nativeAdState: NativeAdState,
    nativeAdReference: MutableState<NativeAd?>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val localContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isActivityDestroyedOrFinishing = rememberUpdatedState(
        newValue = isActivityDestroyedOrFinishing(
            localContext,
            lifecycleOwner
        )
    )
    val nativeAdView = remember {
        NativeAdView(localContext).apply {
            id = View.generateViewId()
        }
    }
    var currentAd: NativeAd? = null

    AndroidView(
        factory = {
            val adLoader = AdLoader.Builder(localContext, nativeAdState.adUnitId)

            adLoader.forNativeAd { nativeAd ->
                if (isActivityDestroyedOrFinishing.value) {
                    nativeAd.destroy()
                    return@forNativeAd
                }

                // Destroy old native ad assets to prevent memory leaks.
                currentAd?.destroy()
                currentAd = nativeAd

                // Set the native ad on the native ad view.
                nativeAdView.setNativeAd(nativeAd)
                nativeAdReference.value = nativeAd

                // TODO: Remove after androidx.compose.ui:ui:1.7.0-beta04
                nativeAdView.viewTreeObserver.dispatchOnGlobalLayout()
            }

            adLoader.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeAdState.onAdFailedToLoad?.invoke(error)
                }

                override fun onAdLoaded() {
                    nativeAdState.onAdLoaded?.invoke()
                }

                override fun onAdClicked() {
                    nativeAdState.onAdClicked?.invoke()
                }

                override fun onAdClosed() {
                    nativeAdState.onAdClosed?.invoke()
                }

                override fun onAdImpression() {
                    nativeAdState.onAdImpression?.invoke()
                }

                override fun onAdOpened() {
                    nativeAdState.onAdOpened?.invoke()
                }

                override fun onAdSwipeGestureClicked() {
                    nativeAdState.onAdSwipeGestureClicked?.invoke()
                }
            })

            if (nativeAdState.nativeAdOptions != null) {
                adLoader.withNativeAdOptions(nativeAdState.nativeAdOptions)
            }

            adLoader.build().loadAd(nativeAdState.adRequest)

            nativeAdView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )

                addView(ComposeView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )

                    setContent {
                        // Set nativeAdView as the current LocalNativeAdView so that
                        // content can access the NativeAdView via LocalNativeAdView.current.
                        // This would allow ad attributes (such as NativeHeadline) to attribute
                        // its contained View subclass via setter functions
                        // (e.g. nativeAdView.headlineView = view)
                        CompositionLocalProvider(LocalNativeAdView provides nativeAdView) {
                            content.invoke()
                        }
                    }
                })
            }
        },
        modifier = modifier,
    )

    // TODO : improve using implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2") and stackoverflow.com/questions/66546962/jetpack-compose-how-do-i-refresh-a-screen-when-app-returns-to-foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY || event == Lifecycle.Event.ON_STOP) {
                currentAd?.destroy()
                currentAd = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun isActivityDestroyedOrFinishing(context: Context, lifecycleOwner: LifecycleOwner): Boolean {
    return (context is LifecycleOwner && (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED || lifecycleOwner.lifecycle.currentState == Lifecycle.State.CREATED || lifecycleOwner.lifecycle.currentState == Lifecycle.State.STARTED))
}

/**
 * The ComposeWrapper container for a advertiserView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdAdvertiserView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val localContext = LocalContext.current
    val localComposeView = remember {
        ComposeView(localContext).apply {
            id = View.generateViewId()
        }
    }

    AndroidView(
        factory = {
            nativeAdView.advertiserView = localComposeView
            localComposeView.apply { setContent(content) }
        },
        modifier = modifier,
    )
}

/**
 * The ComposeWrapper container for a bodyView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdBodyView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val localContext = LocalContext.current
    val localComposeView = remember {
        ComposeView(localContext).apply {
            id = View.generateViewId()
        }
    }

    AndroidView(
        factory = {
            nativeAdView.bodyView = localComposeView
            localComposeView.apply { setContent(content) }
        },
        modifier = modifier,
    )
}

/**
 * The ComposeWrapper container for a callToActionView inside a NativeAdView. This composable must
 * be invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdCallToActionView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val localContext = LocalContext.current
    val localComposeView = remember {
        ComposeView(localContext).apply {
            id = View.generateViewId()
        }
    }

    AndroidView(
        factory = {
            nativeAdView.callToActionView = localComposeView
            localComposeView.apply {
                setContent(content)
            }
        },
        modifier = modifier,
    )
}

/**
 * The ComposeWrapper for a adChoicesView inside a NativeAdView. This composable must be invoked
 * from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 */
@Composable
fun NativeAdChoicesView(modifier: Modifier = Modifier) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val localContext = LocalContext.current

    AndroidView(
        factory = {
            AdChoicesView(localContext).apply {
                minimumWidth = 15
                minimumHeight = 15
            }
        },
        update = { view ->
            nativeAdView.adChoicesView = view
        },
        modifier = modifier,
    )
}

/**
 * The ComposeWrapper container for a headlineView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdHeadlineView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val localContext = LocalContext.current
    val localComposeView = remember {
        ComposeView(localContext).apply {
            id = View.generateViewId()
        }
    }

    AndroidView(
        factory = {
            nativeAdView.headlineView = localComposeView
            localComposeView.apply {
                setContent(content)
            }
        },
        modifier = modifier,
    )
}

/**
 * The ComposeWrapper container for a iconView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdIconView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val localContext = LocalContext.current
    val localComposeView = remember {
        ComposeView(localContext).apply {
            id = View.generateViewId()
        }
    }

    AndroidView(
        factory = {
            nativeAdView.iconView = localComposeView
            localComposeView.apply {
                setContent(content)
            }
        },
        modifier = modifier,
    )
}

/**
 * The ComposeWrapper for a mediaView inside a NativeAdView. This composable must be invoked from
 * within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 */
@Composable
fun NativeAdMediaView(modifier: Modifier = Modifier) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val localContext = LocalContext.current

    AndroidView(
        factory = {
            MediaView(localContext)
        },
        update = { view ->
            nativeAdView.mediaView = view
        },
        modifier = modifier,
    )
}

/**
 * The ComposeWrapper container for a priceView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdPriceView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val localContext = LocalContext.current
    val localComposeView = remember {
        ComposeView(localContext).apply {
            id = View.generateViewId()
        }
    }

    AndroidView(
        factory = {
            nativeAdView.priceView = localComposeView
            localComposeView.apply {
                setContent(content)
            }
        },
        modifier = modifier,
    )
}

/**
 * The ComposeWrapper container for a starRatingView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdStarRatingView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val localContext = LocalContext.current
    val localComposeView = remember {
        ComposeView(localContext).apply {
            id = View.generateViewId()
        }
    }

    AndroidView(
        factory = {
            nativeAdView.starRatingView = localComposeView
            localComposeView.apply {
                setContent(content)
            }
        },
        modifier = modifier,
    )
}

/**
 * The ComposeWrapper container for a storeView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdStoreView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val localContext = LocalContext.current
    val localComposeView = remember {
        ComposeView(localContext).apply {
            id = View.generateViewId()
        }
    }

    AndroidView(
        factory = {
            nativeAdView.storeView = localComposeView
            localComposeView.apply {
                setContent(content)
            }
        },
        modifier = modifier,
    )
}
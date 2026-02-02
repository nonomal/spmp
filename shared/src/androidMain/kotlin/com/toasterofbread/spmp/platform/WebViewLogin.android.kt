package com.toasterofbread.spmp.platform

import LocalPlayerState
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.request.RequestInterceptor
import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult
import com.multiplatform.webview.web.WebContent
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import dev.toastbits.composekit.components.utils.composable.LoadActionIconButton
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import dev.toastbits.composekit.components.utils.composable.animatedvisibility.NullableValueAnimatedVisibility
import dev.toastbits.composekit.theme.core.ThemeValues
import dev.toastbits.composekit.theme.core.ui.LocalComposeKitTheme
import dev.toastbits.composekit.theme.core.vibrantAccent
import dev.toastbits.composekit.util.LocalLocale
import dev.toastbits.composekit.util.model.Locale
import dev.toastbits.composekit.util.platform.Platform
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.webview_restart_required
import spmp.shared.generated.resources.webview_runtime_downloading

actual fun isWebViewLoginSupported(): Boolean =
    true

actual suspend fun initWebViewLogin(context: AppContext, onProgress: (Float, String?) -> Unit): Result<Boolean> {
    clearStorage()
    return Result.success(false)
}

private fun clearStorage() {
    WebStorage.getInstance().deleteAllData()
    CookieManager.getInstance().apply {
        removeAllCookies(null)
        flush()
    }
}

@Composable
actual fun WebViewLogin(
    initial_url: String,
    onClosed: () -> Unit,
    shouldShowPage: (url: String) -> Boolean,
    modifier: Modifier,
    loading_message: String?,
    base_cookies: String,
    user_agent: String?,
    viewport_width: String?,
    viewport_height: String?,
    onRequestIntercepted: suspend (WebViewRequest, openUrl: (String) -> Unit, getCookies: suspend (String) -> List<Pair<String, String>>) -> Unit
) {
    val context: AppContext = LocalPlayerState.current.context
    val theme: ThemeValues = LocalComposeKitTheme.current
    val locale: Locale = LocalLocale.current

    val coroutineScope = rememberCoroutineScope()

    var initialised: Boolean by remember { mutableStateOf(false) }
    var initProgress: Float? by remember { mutableStateOf(null) }
    var initMessage: String? by remember { mutableStateOf(null) }
    var initError: Throwable? by remember { mutableStateOf(null) }
    var restartRequired: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (initialised) {
            return@LaunchedEffect
        }

        val downloadingMessage: String =
            getString(Res.string.webview_runtime_downloading)

        initWebViewLogin(context) { progress, _ ->
            initProgress = progress
            initMessage = downloadingMessage
        }.fold(
            { restartRequired = it },
            { initError = it }
        )
        initialised = true
    }

    initError?.also { error ->
        Box(modifier, contentAlignment = Alignment.Center) {
            ErrorInfoDisplay(
                error,
                onDismiss = onClosed
            )
        }
        return
    }

    if (!initialised) {
        Column(
            modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NullableValueAnimatedVisibility(initMessage) { message ->
                if (message != null) {
                    Text(message)
                }
            }

            InitProgressIndicator(
                initProgress,
                colour = theme.accent,
                trackColour = theme.accent.copy(alpha = 0.5f)
            )
        }
        return
    }

    if (restartRequired) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.webview_restart_required))
        }
        return
    }

    val state: WebViewState = remember {
        val content: WebContent = WebContent.Url(
            url = initial_url,
            additionalHttpHeaders = if (base_cookies.isNotBlank()) mapOf("Cookie" to base_cookies) else emptyMap()
        )
        WebViewState(content).also { state ->
            state.webSettings.apply {
                isJavaScriptEnabled = true
                customUserAgentString = user_agent

                androidWebSettings.apply {
                    isAlgorithmicDarkeningAllowed = true
                    useWideViewPort = true
                }
            }

            state.content = content
        }
    }

    val navigator: WebViewNavigator = remember(state, coroutineScope) {
        WebViewNavigator(
            coroutineScope,
            urlRequestInterceptor = object : RequestInterceptor {
                override fun onInterceptRequest(request: WebRequest, navigator: WebViewNavigator): WebRequestInterceptResult {
                    return WebRequestInterceptResult.Allow
                }
            },
            resourceRequestInterceptor = object : RequestInterceptor {
                override fun onInterceptRequest(request: WebRequest, navigator: WebViewNavigator): WebRequestInterceptResult {
                    coroutineScope.launch {
                        onRequestIntercepted(
                            request.toWebViewRequest(),
                            { url ->
                                navigator.loadUrl(url)
                            },
                            { url ->
                                val headerCookies: List<Pair<String, String>> =
                                    request.headers["Cookie"]?.split(';')?.map { cookie ->
                                        val split: List<String> = cookie.split('=', limit = 2)
                                        Pair(split[0].trim(), split[1].trim())
                                    }.orEmpty()

                                val stateCookies: List<Pair<String, String>> =
                                    state.cookieManager.getCookies(url).map { Pair(it.name, it.value) }

                                return@onRequestIntercepted headerCookies + stateCookies
                            }
                        )
                    }

                    if (Platform.DESKTOP.isCurrent()) {
                        val cookies: String = (request.headers["Cookie"]?.plus(";") ?: "") + base_cookies
                        return WebRequestInterceptResult.Modify(
                            request.copy(headers = request.headers.toMutableMap().also { it["Cookie"] = cookies })
                        )
                    }
                    else {
                        return WebRequestInterceptResult.Allow
                    }
                }
            }
        )
    }

    val show: Boolean by remember(state) {
        derivedStateOf {
            state.lastLoadedUrl?.let { shouldShowPage(it) } ?: false
        }
    }

    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            SubtleLoadingIndicator(
                Modifier.drawWithContent {
                    if (state.isLoading) drawContent()
                }
            )

            Text(
                state.lastLoadedUrl.orEmpty(),
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            LoadActionIconButton({ onClosed() }) {
                Icon(Icons.Default.Close, null)
            }
        }

        val shape: Shape = MaterialTheme.shapes.small
        Box(
            Modifier
                .border(2.dp, theme.vibrantAccent, shape)
                .clip(shape)
                .padding(2.dp)
                .fillMaxSize()
                .weight(1f)
        ) {
            @Suppress("RemoveRedundantQualifierName")
            androidx.compose.animation.AnimatedVisibility(
                !show,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                SubtleLoadingIndicator(message = loading_message)
            }

            WebView(
                state = state,
                navigator = navigator,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


@Composable
private fun InitProgressIndicator(
    progress: Float?,
    colour: Color,
    trackColour: Color,
    modifier: Modifier = Modifier
) {
    if (progress == null) {
        LinearProgressIndicator(
            color = colour,
            trackColor = trackColour,
            modifier = modifier,
        )
    }
    else {
        LinearProgressIndicator(
            progress = { progress },
            color = colour,
            trackColor = trackColour,
            modifier = modifier
        )
    }
}

private fun WebRequest.toWebViewRequest(): WebViewRequest =
    WebViewRequest(
        url,
        isRedirect,
        method,
        headers.toMap()
    )

// private fun WebResourceRequest.toWebViewRequest(): WebViewRequest =
//     WebViewRequest(
//         url.toString(),
//         isRedirect,
//         method,
//         requestHeaders
//     )

// @SuppressLint("SetJavaScriptEnabled")
// @Composable
// fun OldWebViewLogin(
//     initial_url: String,
//     modifier: Modifier,
//     onClosed: () -> Unit,
//     shouldShowPage: (url: String) -> Boolean,
//     loading_message: String?,
//     onRequestIntercepted: suspend (WebViewRequest, openUrl: (String) -> Unit) -> Unit
// ) {
//     val player: PlayerState = LocalPlayerState.current
//     var web_view: WebView? by remember { mutableStateOf(null) }
//     val is_dark: Boolean by remember { derivedStateOf { player.theme.background.isDark() } }

//     var requested_url: String? by remember { mutableStateOf(null) }
//     OnChangedEffect(requested_url) {
//         requested_url?.also {
//             web_view?.loadUrl(it)
//         }
//     }

//     OnChangedEffect(web_view, is_dark) {
//         web_view?.apply {
//             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                 settings.isAlgorithmicDarkeningAllowed = is_dark
//             }
//             else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                 @Suppress("DEPRECATION")
//                 settings.forceDark = if (is_dark) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
//             }
//         }
//     }

//     DisposableEffect(Unit) {
//         onDispose {
//             clearStorage()
//         }
//     }

//     BackHandler(web_view?.canGoBack() == true) {
//         val web: WebView = web_view ?: return@BackHandler

//         val back_forward_list = web.copyBackForwardList()
//         if (back_forward_list.currentIndex > 0) {
//             val previous_url = back_forward_list.getItemAtIndex(back_forward_list.currentIndex - 1).url
//             if (previous_url == initial_url) {
//                 onClosed()
//                 clearStorage()
//                 return@BackHandler
//             }
//         }

//         web.goBack()
//     }

//     var show_webview by remember { mutableStateOf(false) }

//     Box(contentAlignment = Alignment.Center) {
//         AnimatedVisibility(!show_webview, enter = fadeIn(), exit = fadeOut()) {
//             SubtleLoadingIndicator(message = loading_message)
//         }

//         AndroidView(
//             modifier = modifier.graphicsLayer {
//                 alpha = if (show_webview) 1f else 0f
//             },
//             factory = { context ->
//                 WebView(context).apply {
//                     clearStorage()

//                     settings.javaScriptEnabled = true
//                     settings.domStorageEnabled = true

//                     layoutParams = ViewGroup.LayoutParams(
//                         ViewGroup.LayoutParams.MATCH_PARENT,
//                         ViewGroup.LayoutParams.MATCH_PARENT
//                     )

//                     webChromeClient = object : WebChromeClient() {
//                         override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
//                             return true
//                         }
//                     }
//                     webViewClient = object : WebViewClient() {
//                         override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
//                             super.onPageStarted(view, url, favicon)

//                             if (!shouldShowPage(url)) {
//                                 show_webview = false
//                             }
//                         }

//                         override fun onPageFinished(view: WebView?, url: String?) {
//                             super.onPageFinished(view, url)

//                             if (url != null && shouldShowPage(url)) {
//                                 show_webview = true
//                             }
//                         }

//                         override fun shouldInterceptRequest(
//                             view: WebView,
//                             request: WebResourceRequest
//                         ): WebResourceResponse? {
//                             runBlocking {
//                                 onRequestIntercepted(
//                                     request.toWebViewRequest(),
//                                     {
//                                         requested_url = it
//                                     }
//                                 )
//                             }
//                             return null
//                         }
//                     }

//                     loadUrl(initial_url)
//                     web_view = this
//                 }
//             }
//         )
//     }
// }
